package com.hlju.learning.domain.material;

import java.time.Instant;
import java.util.List;

public record MaterialChunk(
        String chunkId,
        String materialId,
        String chapterId,
        String chapterTitle,
        int chunkIndex,
        Integer pageNo,
        String sourceLabel,
        String text,
        List<String> keywords,
        Instant createdAt
) {
}
