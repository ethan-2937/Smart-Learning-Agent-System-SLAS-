package com.hlju.learning.domain.course;

import jakarta.validation.constraints.NotBlank;

public record BindMaterialCourseRequest(
        @NotBlank(message = "Course id cannot be empty")
        String courseId,
        String chapterId
) {
}
