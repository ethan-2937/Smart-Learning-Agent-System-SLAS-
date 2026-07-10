package com.hlju.learning.domain.question;

public record QuestionSourceRef(
        String materialId,
        String chunkId,
        String chapterTitle,
        Integer pageNo,
        String snippet,
        double score
) {
}
