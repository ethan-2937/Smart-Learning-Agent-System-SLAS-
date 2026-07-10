package com.hlju.learning.vector;

import com.hlju.learning.config.VectorProperties;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QdrantVectorStore implements VectorStore {
    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    private final VectorProperties properties;
    private final InMemoryVectorStore fallbackStore;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public QdrantVectorStore(VectorProperties properties, InMemoryVectorStore fallbackStore,
                             EmbeddingClient embeddingClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.fallbackStore = fallbackStore;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
        this.client = RestClient.create(properties.qdrantUrl());
    }

    @PostConstruct
    void init() {
        if (!"qdrant".equalsIgnoreCase(properties.provider())) {
            return;
        }
        try {
            ensureCollection();
        } catch (Exception ex) {
            log.warn("Qdrant is not ready, vector search will use in-memory fallback. reason={}", ex.getMessage());
        }
    }

    @Override
    public void upsertChunks(List<MaterialChunk> chunks) {
        fallbackStore.upsertChunks(chunks);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        try {
            ensureCollection();
            List<Map<String, Object>> points = new ArrayList<>();
            for (MaterialChunk chunk : chunks) {
                float[] vector = embeddingClient.embed(chunk.text());
                if (vector.length != properties.dimension()) {
                    log.warn("Skip Qdrant upsert for chunk {}, vector dimension {} != configured {}",
                            chunk.chunkId(), vector.length, properties.dimension());
                    continue;
                }
                points.add(Map.of(
                        "id", chunk.chunkId(),
                        "vector", toList(vector),
                        "payload", payloadOf(chunk)
                ));
                if (points.size() >= 64) {
                    upsertBatch(points);
                    points.clear();
                }
            }
            if (!points.isEmpty()) {
                upsertBatch(points);
            }
        } catch (Exception ex) {
            log.warn("Qdrant upsert failed, in-memory fallback is still available. reason={}", ex.getMessage());
        }
    }

    @Override
    public List<RetrievalHit> search(String query, String materialId, int topK) {
        try {
            float[] vector = embeddingClient.embed(query);
            if (vector.length != properties.dimension()) {
                log.warn("Skip Qdrant search, query vector dimension {} != configured {}", vector.length, properties.dimension());
                return fallbackStore.search(query, materialId, topK);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("vector", toList(vector));
            body.put("limit", Math.max(1, topK));
            body.put("with_payload", true);
            if (materialId != null && !materialId.isBlank()) {
                body.put("filter", Map.of("must", List.of(
                        Map.of("key", "materialId", "match", Map.of("value", materialId))
                )));
            }
            JsonNode root = client.post()
                    .uri("/collections/{collection}/points/search", properties.collectionName())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            List<RetrievalHit> hits = parseHits(root);
            return hits.isEmpty() ? fallbackStore.search(query, materialId, topK) : hits;
        } catch (Exception ex) {
            log.warn("Qdrant search failed, using in-memory fallback. reason={}", ex.getMessage());
            return fallbackStore.search(query, materialId, topK);
        }
    }

    private void ensureCollection() {
        try {
            client.get()
                    .uri("/collections/{collection}", properties.collectionName())
                    .retrieve()
                    .toBodilessEntity();
            return;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() != 404) {
                throw ex;
            }
        }
        client.put()
                .uri("/collections/{collection}", properties.collectionName())
                .body(Map.of("vectors", Map.of("size", properties.dimension(), "distance", "Cosine")))
                .retrieve()
                .toBodilessEntity();
    }

    private void upsertBatch(List<Map<String, Object>> points) {
        client.put()
                .uri("/collections/{collection}/points?wait=true", properties.collectionName())
                .body(Map.of("points", points))
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> payloadOf(MaterialChunk chunk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chunkId", chunk.chunkId());
        payload.put("materialId", chunk.materialId());
        payload.put("chapterId", chunk.chapterId());
        payload.put("chapterTitle", chunk.chapterTitle());
        payload.put("chunkIndex", chunk.chunkIndex());
        payload.put("pageNo", chunk.pageNo());
        payload.put("sourceLabel", chunk.sourceLabel());
        payload.put("text", chunk.text());
        payload.put("keywords", chunk.keywords());
        payload.put("createdAt", chunk.createdAt() == null ? Instant.now().toString() : chunk.createdAt().toString());
        return payload;
    }

    private List<RetrievalHit> parseHits(JsonNode root) {
        List<RetrievalHit> hits = new ArrayList<>();
        JsonNode result = root == null ? null : root.path("result");
        if (result == null || !result.isArray()) {
            return hits;
        }
        for (JsonNode item : result) {
            MaterialChunk chunk = chunkFromPayload(item.path("payload"), item.path("id").asText(""));
            hits.add(new RetrievalHit(chunk, item.path("score").asDouble(0.0), "qdrant-vector cosine"));
        }
        return hits;
    }

    private MaterialChunk chunkFromPayload(JsonNode payload, String fallbackId) {
        List<String> keywords = new ArrayList<>();
        JsonNode keywordNode = payload.path("keywords");
        if (keywordNode.isArray()) {
            keywordNode.forEach(node -> keywords.add(node.asText()));
        }
        Integer pageNo = payload.hasNonNull("pageNo") ? payload.path("pageNo").asInt() : null;
        Instant createdAt = Instant.now();
        String rawCreatedAt = text(payload, "createdAt", "");
        if (!rawCreatedAt.isBlank()) {
            try {
                createdAt = Instant.parse(rawCreatedAt);
            } catch (Exception ignored) {
                createdAt = Instant.now();
            }
        }
        return new MaterialChunk(
                text(payload, "chunkId", fallbackId),
                text(payload, "materialId", ""),
                text(payload, "chapterId", "chapter-default"),
                text(payload, "chapterTitle", ""),
                payload.path("chunkIndex").asInt(0),
                pageNo,
                text(payload, "sourceLabel", ""),
                text(payload, "text", ""),
                keywords,
                createdAt
        );
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText(fallback);
    }

    private List<Float> toList(float[] vector) {
        return objectMapper.convertValue(vector, objectMapper.getTypeFactory().constructCollectionType(List.class, Float.class));
    }
}
