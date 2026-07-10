package com.hlju.learning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.ai.LlmClient;
import com.hlju.learning.ai.MockLlmClient;
import com.hlju.learning.ai.OpenAiCompatibleLlmClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfig {
    @Bean
    public LlmClient llmClient(AiProperties properties, ObjectMapper objectMapper) {
        if (isRemoteProvider(properties.provider())) {
            return new OpenAiCompatibleLlmClient(properties, objectMapper, new MockLlmClient());
        }
        return new MockLlmClient();
    }

    private boolean isRemoteProvider(String provider) {
        return "openai-compatible".equalsIgnoreCase(provider)
                || "deepseek".equalsIgnoreCase(provider)
                || "deepseek-v4-flash".equalsIgnoreCase(provider);
    }
}
