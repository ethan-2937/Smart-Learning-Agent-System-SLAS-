package com.hlju.learning.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.config.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingClient.class);

    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final EmbeddingClient fallback;
    private final RestClient client;

    public OpenAiCompatibleEmbeddingClient(EmbeddingProperties properties, ObjectMapper objectMapper, EmbeddingClient fallback) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
        this.client = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties.timeoutSeconds()))
                .build();
    }

    @Override
    public float[] embed(String text) {
        if (isBlank(properties.baseUrl()) || isBlank(properties.apiKey()) || isBlank(properties.model())) {
            return fallback.embed(text);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.model());
            body.put("input", truncate(text));
            JsonNode root = client.post()
                    .uri(pathOrDefault(properties.embeddingPath(), "/v1/embeddings"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            float[] vector = parseEmbedding(root);
            return vector.length == 0 ? fallback.embed(text) : normalize(vector);
        } catch (Exception ex) {
            log.warn("Remote embedding call failed, using mock fallback. reason={}", ex.getMessage());
            return fallback.embed(text);
        }
    }

    private float[] parseEmbedding(JsonNode root) {
        JsonNode embedding = root == null ? null : root.path("data").path(0).path("embedding");
        if (embedding == null || !embedding.isArray()) {
            return new float[0];
        }
        List<Float> values = new ArrayList<>();
        embedding.forEach(node -> values.add((float) node.asDouble()));
        return objectMapper.convertValue(values, float[].class);
    }

    private float[] normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return vector;
        }
        float length = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
        return vector;
    }

    private String truncate(String text) {
        String value = text == null ? "" : text;
        int maxInputChars = properties.maxInputChars() <= 0 ? 6000 : properties.maxInputChars();
        return value.length() <= maxInputChars ? value : value.substring(0, maxInputChars);
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
        int millis = Math.max(5, timeoutSeconds <= 0 ? 60 : timeoutSeconds) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(millis);
        factory.setReadTimeout(millis);
        return factory;
    }
}
