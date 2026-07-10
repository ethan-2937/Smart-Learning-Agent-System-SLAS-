package com.hlju.learning.domain.course;

import com.hlju.learning.domain.material.SubjectPreset;

import java.time.Instant;

public record CourseRecord(
        String courseId,
        String name,
        SubjectPreset subjectPreset,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
