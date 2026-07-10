package com.hlju.learning.domain.course;

import java.time.Instant;

public record MaterialCourseBindingRecord(
        String materialId,
        String courseId,
        String chapterId,
        Instant createdAt,
        Instant updatedAt
) {
}
