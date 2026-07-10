package com.hlju.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, String baseUrl, String chatPath, String apiKey, String model,
                           double temperature, int timeoutSeconds, int maxOutputTokens) {
}
