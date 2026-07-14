package com.hlju.learning.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final LlmClient fallback;
    private final RestClient client;

    public OpenAiCompatibleLlmClient(AiProperties properties, ObjectMapper objectMapper, LlmClient fallback) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
        this.client = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties.timeoutSeconds()))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return completeWithMetadata(systemPrompt, userPrompt).content();
    }

    @Override
    public LlmCompletion completeWithMetadata(String systemPrompt, String userPrompt) {
        if (isBlank(properties.baseUrl()) || isBlank(properties.apiKey())) {
            return fallback.completeWithMetadata(systemPrompt, userPrompt);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.model());
            body.put("messages", messages(systemPrompt, userPrompt));
            body.put("temperature", properties.temperature());
            if (properties.maxOutputTokens() > 0) {
                body.put("max_tokens", properties.maxOutputTokens());
            }
            if (isDeepSeekProvider() && properties.thinkingEnabled() != null) {
                body.put("thinking", Map.of("type", properties.thinkingEnabled() ? "enabled" : "disabled"));
                if (properties.thinkingEnabled() && !isBlank(properties.reasoningEffort())) {
                    body.put("reasoning_effort", properties.reasoningEffort());
                }
            }
            JsonNode root = client.post()
                    .uri(pathOrDefault(properties.chatPath(), "/chat/completions"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = root == null ? "" : root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return fallback.completeWithMetadata(systemPrompt, userPrompt);
            }
            JsonNode usage = root.path("usage").path("total_tokens");
            return new LlmCompletion(content, usage.isIntegralNumber() ? usage.intValue() : null);
        } catch (Exception ex) {
            log.warn("Remote LLM call failed, using mock fallback. reason={}", ex.getMessage());
            return fallback.completeWithMetadata(systemPrompt, userPrompt);
        }
    }

    private List<Map<String, String>> messages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt));
        return messages;
    }

    private boolean isDeepSeekProvider() {
        String provider = properties.provider();
        return "deepseek".equalsIgnoreCase(provider) || "deepseek-v4-flash".equalsIgnoreCase(provider);
    }

    private String pathOrDefault(String path, String fallbackPath) {
        String normalized = isBlank(path) ? fallbackPath : path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        int millis = Math.max(5, timeoutSeconds <= 0 ? 120 : timeoutSeconds) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(millis);
        factory.setReadTimeout(millis);
        return factory;
    }
}
