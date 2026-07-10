package com.hlju.learning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.vector.EmbeddingClient;
import com.hlju.learning.vector.MockEmbeddingClient;
import com.hlju.learning.vector.OpenAiCompatibleEmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingClientConfig {
    @Bean
    public EmbeddingClient embeddingClient(VectorProperties vectorProperties,
                                           EmbeddingProperties embeddingProperties,
                                           ObjectMapper objectMapper) {
        MockEmbeddingClient fallback = new MockEmbeddingClient(vectorProperties);
        if ("openai-compatible".equalsIgnoreCase(embeddingProperties.provider())) {
            return new OpenAiCompatibleEmbeddingClient(embeddingProperties, objectMapper, fallback);
        }
        return fallback;
    }
}
