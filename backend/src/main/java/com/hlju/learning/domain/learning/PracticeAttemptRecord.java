package com.hlju.learning.domain.learning;

import java.time.Instant;
import java.util.List;

public record PracticeAttemptRecord(
        String attemptId,
        String practiceId,
        String studentId,
        String questionId,
        String answerText,
        boolean correct,
        double score,
        String expectedAnswer,
        String feedback,
        List<String> knowledgeNames,
        Instant submittedAt
) {
}
