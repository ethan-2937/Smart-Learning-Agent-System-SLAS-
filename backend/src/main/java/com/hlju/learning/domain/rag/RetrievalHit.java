package com.hlju.learning.domain.rag;

import com.hlju.learning.domain.material.MaterialChunk;

public record RetrievalHit(MaterialChunk chunk, double score, String reason) {
}
