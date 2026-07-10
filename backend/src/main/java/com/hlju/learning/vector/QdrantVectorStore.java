package com.hlju.learning.vector;

import com.hlju.learning.config.VectorProperties;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class QdrantVectorStore implements VectorStore {
    private final VectorProperties properties;
    private final InMemoryVectorStore fallbackStore;

    public QdrantVectorStore(VectorProperties properties, InMemoryVectorStore fallbackStore) {
        this.properties = properties;
        this.fallbackStore = fallbackStore;
    }

    @PostConstruct
    void init() {
        if (!"qdrant".equalsIgnoreCase(properties.provider())) {
            return;
        }
        try {
            RestClient.create(properties.qdrantUrl())
                    .put()
                    .uri("/collections/{collection}", properties.collectionName())
                    .body("{\"vectors\":{\"size\":" + properties.dimension() + ",\"distance\":\"Cosine\"}}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // Keep the demo stable even when Qdrant is not running.
        }
    }

    @Override
    public void upsertChunks(List<MaterialChunk> chunks) {
        // Initial version keeps an in-memory fallback; real Qdrant HTTP upsert is the next iteration.
        fallbackStore.upsertChunks(chunks);
    }

    @Override
    public List<RetrievalHit> search(String query, String materialId, int topK) {
        return fallbackStore.search(query, materialId, topK);
    }
}
