package com.hlju.learning.domain.question;

import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.material.SubjectPreset;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GenerateQuestionRequest(
        @NotBlank String materialId,
        SubjectPreset subjectPreset,
        String topic,
        List<QuestionType> questionTypes,
        QuestionDifficulty difficulty,
        @Min(1) @Max(20) int count,
        AgentWorkflowMode workflowMode
) {
    public AgentWorkflowMode resolvedWorkflowMode() {
        return workflowMode == null ? AgentWorkflowMode.RAG_MULTI_AGENT : workflowMode;
    }
}
