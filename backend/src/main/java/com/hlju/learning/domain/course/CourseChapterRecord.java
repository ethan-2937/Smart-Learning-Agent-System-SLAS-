package com.hlju.learning.domain.course;

import java.time.Instant;

public record CourseChapterRecord(
        String chapterId,
        String courseId,
        String title,
        int chapterOrder,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
