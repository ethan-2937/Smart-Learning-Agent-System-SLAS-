package com.hlju.learning.domain.learning;

import java.time.Instant;

public record KnowledgeMasteryRecord(
        String studentId,
        String courseId,
        String chapterId,
        String materialId,
        String knowledgeName,
        int totalAttempts,
        int correctAttempts,
        double mastery,
        Instant updatedAt
) {
    public KnowledgeMasteryRecord addAttempt(boolean correct) {
        int nextTotal = totalAttempts + 1;
        int nextCorrect = correctAttempts + (correct ? 1 : 0);
        double nextMastery = nextTotal == 0 ? 0.0 : (double) nextCorrect / nextTotal;
        return new KnowledgeMasteryRecord(studentId, courseId, chapterId, materialId, knowledgeName,
                nextTotal, nextCorrect, nextMastery, Instant.now());
    }
}
