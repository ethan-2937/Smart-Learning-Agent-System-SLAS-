package com.hlju.learning.vector;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorStoreTest {
    private final EmbeddingClient embeddingClient = text -> {
        String value = text == null ? "" : text.toLowerCase();
        if (value.contains("payment")) {
            return new float[]{1.0f, 0.0f};
        }
        return value.contains("carrier") ? new float[]{0.2f, 0.0f} : new float[]{0.0f, 1.0f};
    };

    @Test
    void searchRanksRelevantChunksAndFiltersByMaterial() {
        InMemoryVectorStore store = new InMemoryVectorStore(embeddingClient);
        store.upsertChunks(List.of(
                chunk("payment", "material-a", "Payment is due within thirty days."),
                chunk("shipping", "material-a", "Shipping uses a confirmed carrier."),
                chunk("other", "material-b", "Payment is due immediately.")
        ));

        List<RetrievalHit> hits = store.search("payment terms", "material-a", 2);

        assertThat(hits).extracting(hit -> hit.chunk().chunkId())
                .containsExactly("payment", "shipping");
        assertThat(hits).allMatch(hit -> hit.chunk().materialId().equals("material-a"));
        assertThat(hits.get(0).score()).isGreaterThan(hits.get(1).score());
    }

    private MaterialChunk chunk(String id, String materialId, String text) {
        return new MaterialChunk(
                id,
                materialId,
                "chapter-1",
                "Trade Terms",
                0,
                1,
                "test-fixture",
                text,
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
