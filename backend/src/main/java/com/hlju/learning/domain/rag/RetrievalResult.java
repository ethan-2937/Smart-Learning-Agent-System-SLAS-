package com.hlju.learning.domain.rag;

import java.util.List;

public record RetrievalResult(String query, List<RetrievalHit> hits) {
}
