package com.hlju.learning.domain.learning;

import java.time.Instant;
import java.util.List;

public record PracticeSetRecord(
        String practiceId,
        String title,
        String studentId,
        String courseId,
        String chapterId,
        String materialId,
        List<String> questionIds,
        PracticeSetStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public PracticeSetRecord withStatus(PracticeSetStatus nextStatus) {
        return new PracticeSetRecord(practiceId, title, studentId, courseId, chapterId, materialId,
                questionIds, nextStatus, createdAt, Instant.now());
    }
}
