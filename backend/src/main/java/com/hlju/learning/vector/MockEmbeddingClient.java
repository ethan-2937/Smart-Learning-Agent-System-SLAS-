package com.hlju.learning.vector;

import com.hlju.learning.config.VectorProperties;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MockEmbeddingClient implements EmbeddingClient {
    private final int dimension;

    public MockEmbeddingClient(VectorProperties properties) {
        this.dimension = Math.max(16, properties.dimension());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }
        byte[] bytes = text.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int index = Math.floorMod(bytes[i] + i * 31, dimension);
            vector[index] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) return;
        float length = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
    }
}
