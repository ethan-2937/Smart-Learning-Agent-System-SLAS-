package com.hlju.learning.domain.question;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchQuestionStatusRequest(
        @NotEmpty(message = "Question ids cannot be empty")
        List<String> questionIds,
        boolean approved
) {
}
