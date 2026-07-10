package com.hlju.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vector")
public record VectorProperties(String provider, String collectionName, String qdrantUrl, int dimension, int topK) {
}
