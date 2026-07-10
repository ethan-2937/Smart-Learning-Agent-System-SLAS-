package com.hlju.learning.vector;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryVectorStore implements VectorStore {
    private final Map<String, MaterialChunk> chunks = new ConcurrentHashMap<>();
    private final EmbeddingClient embeddingClient;

    public InMemoryVectorStore(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @Override
    public void upsertChunks(List<MaterialChunk> newChunks) {
        newChunks.forEach(chunk -> chunks.put(chunk.chunkId(), chunk));
    }

    @Override
    public List<RetrievalHit> search(String query, String materialId, int topK) {
        float[] queryVector = embeddingClient.embed(query);
        Set<String> queryTokens = tokenize(query);
        return chunks.values().stream()
                .filter(chunk -> materialId == null || materialId.isBlank() || materialId.equals(chunk.materialId()))
                .map(chunk -> new RetrievalHit(chunk, score(queryVector, queryTokens, chunk), "memory-vector + keyword hybrid"))
                .filter(hit -> hit.score() > 0.02)
                .sorted(Comparator.comparingDouble(RetrievalHit::score).reversed())
                .limit(Math.max(1, topK))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private double score(float[] queryVector, Set<String> queryTokens, MaterialChunk chunk) {
        float[] chunkVector = embeddingClient.embed(chunk.text());
        double vectorScore = cosine(queryVector, chunkVector);
        Set<String> chunkTokens = tokenize(chunk.text());
        long overlap = queryTokens.stream().filter(chunkTokens::contains).count();
        double keywordScore = queryTokens.isEmpty() ? 0.0 : (double) overlap / queryTokens.size();
        return vectorScore * 0.65 + keywordScore * 0.35;
    }

    private double cosine(float[] left, float[] right) {
        double dot = 0.0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+"))
                .stream()
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }
}
