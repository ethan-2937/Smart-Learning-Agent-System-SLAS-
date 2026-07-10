package com.hlju.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingProperties(String provider, String baseUrl, String embeddingPath, String apiKey,
                                  String model, int timeoutSeconds, int maxInputChars) {
}
