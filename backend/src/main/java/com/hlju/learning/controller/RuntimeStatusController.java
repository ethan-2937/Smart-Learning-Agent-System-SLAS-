package com.hlju.learning.controller;

import com.hlju.learning.config.AiProperties;
import com.hlju.learning.config.EmbeddingProperties;
import com.hlju.learning.config.VectorProperties;
import com.hlju.learning.domain.system.RuntimeStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuntimeStatusController {
    private final AiProperties aiProperties;
    private final EmbeddingProperties embeddingProperties;
    private final VectorProperties vectorProperties;

    public RuntimeStatusController(AiProperties aiProperties, EmbeddingProperties embeddingProperties,
                                   VectorProperties vectorProperties) {
        this.aiProperties = aiProperties;
        this.embeddingProperties = embeddingProperties;
        this.vectorProperties = vectorProperties;
    }

    @GetMapping("/api/runtime/status")
    public RuntimeStatus status() {
        return new RuntimeStatus(
                safe(aiProperties.provider()),
                safe(aiProperties.model()),
                hasText(aiProperties.apiKey()),
                safe(aiProperties.baseUrl()),
                safe(embeddingProperties.provider()),
                safe(embeddingProperties.model()),
                hasText(embeddingProperties.apiKey()),
                safe(vectorProperties.provider()),
                safe(vectorProperties.collectionName()),
                safe(vectorProperties.qdrantUrl()),
                vectorProperties.dimension(),
                vectorProperties.topK()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
