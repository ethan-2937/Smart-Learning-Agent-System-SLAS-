package com.hlju.learning.domain.learning;

public record PracticeCreateRequest(
        String studentId,
        String courseId,
        String chapterId,
        String materialId,
        Integer count
) {
}
