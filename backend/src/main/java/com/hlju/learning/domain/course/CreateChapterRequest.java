package com.hlju.learning.domain.course;

import jakarta.validation.constraints.NotBlank;

public record CreateChapterRequest(
        @NotBlank(message = "Chapter title cannot be empty")
        String title,
        Integer chapterOrder,
        String description
) {
}
