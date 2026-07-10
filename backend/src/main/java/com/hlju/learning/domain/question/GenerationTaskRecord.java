package com.hlju.learning.domain.question;

import com.hlju.learning.domain.material.SubjectPreset;

import java.time.Instant;
import java.util.List;

public record GenerationTaskRecord(
        String taskId,
        String materialId,
        SubjectPreset subjectPreset,
        String topic,
        List<QuestionType> questionTypes,
        QuestionDifficulty difficulty,
        int requestedCount,
        GenerationTaskStatus status,
        String agentRunId,
        List<QuestionRecord> questions,
        Instant createdAt,
        Instant updatedAt
) {
    public GenerationTaskRecord withResult(GenerationTaskStatus nextStatus, String nextAgentRunId, List<QuestionRecord> nextQuestions) {
        return new GenerationTaskRecord(taskId, materialId, subjectPreset, topic, questionTypes, difficulty,
                requestedCount, nextStatus, nextAgentRunId, nextQuestions, createdAt, Instant.now());
    }
}
