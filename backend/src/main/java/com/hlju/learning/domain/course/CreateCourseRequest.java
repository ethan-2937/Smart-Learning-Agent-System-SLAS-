package com.hlju.learning.domain.course;

import com.hlju.learning.domain.material.SubjectPreset;
import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank(message = "Course name cannot be empty")
        String name,
        SubjectPreset subjectPreset,
        String description
) {
}
