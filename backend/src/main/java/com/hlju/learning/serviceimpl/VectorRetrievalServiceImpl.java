package com.hlju.learning.serviceimpl;

import com.hlju.learning.config.VectorProperties;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalResult;
import com.hlju.learning.service.VectorRetrievalService;
import com.hlju.learning.vector.InMemoryVectorStore;
import com.hlju.learning.vector.QdrantVectorStore;
import com.hlju.learning.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorRetrievalServiceImpl implements VectorRetrievalService {
    private final VectorProperties properties;
    private final InMemoryVectorStore memoryStore;
    private final QdrantVectorStore qdrantStore;

    public VectorRetrievalServiceImpl(VectorProperties properties, InMemoryVectorStore memoryStore, QdrantVectorStore qdrantStore) {
        this.properties = properties;
        this.memoryStore = memoryStore;
        this.qdrantStore = qdrantStore;
    }

    @Override
    public void indexChunks(List<MaterialChunk> chunks) {
        store().upsertChunks(chunks);
    }

    @Override
    public RetrievalResult retrieve(String query, String materialId, Integer topK) {
        int limit = topK == null || topK <= 0 ? properties.topK() : topK;
        return new RetrievalResult(query, store().search(query, materialId, limit));
    }

    private VectorStore store() {
        return "qdrant".equalsIgnoreCase(properties.provider()) ? qdrantStore : memoryStore;
    }
}
