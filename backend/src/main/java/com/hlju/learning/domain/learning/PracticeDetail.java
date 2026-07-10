package com.hlju.learning.domain.learning;

import com.hlju.learning.domain.question.QuestionRecord;

import java.util.List;

public record PracticeDetail(
        PracticeSetRecord practice,
        List<QuestionRecord> questions,
        List<PracticeAttemptRecord> attempts
) {
}
