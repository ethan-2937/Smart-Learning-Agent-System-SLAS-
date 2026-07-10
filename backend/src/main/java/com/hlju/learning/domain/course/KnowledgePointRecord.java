package com.hlju.learning.domain.course;

import java.time.Instant;

public record KnowledgePointRecord(
        String knowledgePointId,
        String courseId,
        String chapterId,
        String materialId,
        String chunkId,
        String name,
        String description,
        String sourceSnippet,
        double weight,
        Instant createdAt
) {
}
