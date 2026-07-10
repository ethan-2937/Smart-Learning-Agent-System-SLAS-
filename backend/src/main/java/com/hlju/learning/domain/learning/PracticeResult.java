package com.hlju.learning.domain.learning;

import java.util.List;

public record PracticeResult(String practiceId, String attemptId, String questionId, boolean correct, double score,
                             String expectedAnswer, String feedback, List<String> knowledgeNames) {
}
