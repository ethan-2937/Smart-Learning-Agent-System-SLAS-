package com.hlju.learning.vector;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;

import java.util.List;

public interface VectorStore {
    void upsertChunks(List<MaterialChunk> chunks);

    List<RetrievalHit> search(String query, String materialId, int topK);
}
