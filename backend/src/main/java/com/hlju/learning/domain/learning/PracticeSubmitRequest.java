package com.hlju.learning.domain.learning;

import jakarta.validation.constraints.NotBlank;

public record PracticeSubmitRequest(@NotBlank String questionId, String studentId, String answerText) {
}
