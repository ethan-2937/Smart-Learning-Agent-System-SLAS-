package com.hlju.learning.domain.system;

public record RuntimeStatus(
        String aiProvider,
        String aiModel,
        boolean aiApiKeyConfigured,
        String aiBaseUrl,
        String embeddingProvider,
        String embeddingModel,
        boolean embeddingApiKeyConfigured,
        String vectorProvider,
        String vectorCollectionName,
        String qdrantUrl,
        int vectorDimension,
        int defaultTopK
) {
}
