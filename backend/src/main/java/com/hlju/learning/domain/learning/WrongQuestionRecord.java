package com.hlju.learning.domain.learning;

import java.time.Instant;

public record WrongQuestionRecord(
        String studentId,
        String questionId,
        String materialId,
        String prompt,
        String expectedAnswer,
        String lastAnswer,
        String lastFeedback,
        int wrongCount,
        Instant lastSubmittedAt
) {
}
