package com.hlju.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String jwtSecret, String issuer, long tokenTtlHours) {
    public String safeJwtSecret() {
        return jwtSecret == null || jwtSecret.isBlank()
                ? "smart-learning-agent-local-development-secret-change-in-production"
                : jwtSecret;
    }

    public String safeIssuer() {
        return issuer == null || issuer.isBlank() ? "smart-learning-agent" : issuer;
    }

    public long safeTokenTtlHours() {
        return tokenTtlHours <= 0 ? 12 : tokenTtlHours;
    }
}
