package com.hlju.learning.domain.question;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateQuestionRequest(
        @NotBlank(message = "Question prompt cannot be empty")
        String prompt,
        List<QuestionOption> options,
        @NotBlank(message = "Answer cannot be empty")
        String answerText,
        String analysisText,
        QuestionDifficulty difficulty
) {
}
