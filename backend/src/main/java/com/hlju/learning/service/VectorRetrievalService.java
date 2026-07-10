package com.hlju.learning.service;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalResult;

import java.util.List;

public interface VectorRetrievalService {
    void indexChunks(List<MaterialChunk> chunks);

    RetrievalResult retrieve(String query, String materialId, Integer topK);
}
