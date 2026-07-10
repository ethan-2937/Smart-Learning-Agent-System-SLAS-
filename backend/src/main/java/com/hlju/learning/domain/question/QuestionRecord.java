package com.hlju.learning.domain.question;

import com.hlju.learning.domain.material.SubjectPreset;

import java.time.Instant;
import java.util.List;

public record QuestionRecord(
        String questionId,
        String taskId,
        String materialId,
        QuestionType type,
        QuestionDifficulty difficulty,
        SubjectPreset subjectPreset,
        String prompt,
        List<QuestionOption> options,
        String answerText,
        String analysisText,
        List<QuestionSourceRef> sourceRefs,
        QuestionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public QuestionRecord withStatus(QuestionStatus nextStatus) {
        return new QuestionRecord(questionId, taskId, materialId, type, difficulty, subjectPreset, prompt, options,
                answerText, analysisText, sourceRefs, nextStatus, createdAt, Instant.now());
    }

    public QuestionRecord withContent(String nextPrompt, List<QuestionOption> nextOptions, String nextAnswerText,
                                      String nextAnalysisText, QuestionDifficulty nextDifficulty) {
        return new QuestionRecord(questionId, taskId, materialId, type,
                nextDifficulty == null ? difficulty : nextDifficulty,
                subjectPreset,
                nextPrompt == null || nextPrompt.isBlank() ? prompt : nextPrompt,
                nextOptions == null ? options : nextOptions,
                nextAnswerText == null || nextAnswerText.isBlank() ? answerText : nextAnswerText,
                nextAnalysisText == null ? analysisText : nextAnalysisText,
                sourceRefs, status, createdAt, Instant.now());
    }
}
