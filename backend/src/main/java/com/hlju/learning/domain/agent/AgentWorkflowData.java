package com.hlju.learning.domain.agent;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;

import java.util.List;
import java.util.Map;

public final class AgentWorkflowData {
    private AgentWorkflowData() {
    }

    public enum ReviewDecision {
        PASS,
        REVISE,
        HUMAN_REVIEW,
        REJECT
    }

    public record WorkflowRequest(
            String taskId,
            MaterialRecord material,
            SubjectPreset subjectPreset,
            String topic,
            List<QuestionType> questionTypes,
            QuestionDifficulty difficulty,
            int count,
            AgentWorkflowMode mode
    ) {
    }

    public record MaterialUnderstandingInput(WorkflowRequest request) {
    }

    public record MaterialUnderstandingOutput(
            String theme,
            List<String> knowledgePoints,
            List<MaterialChunk> candidateChunks
    ) {
    }

    public record RetrievalPlanningInput(
            WorkflowRequest request,
            MaterialUnderstandingOutput understanding
    ) {
    }

    public record RetrievalPlanningOutput(
            List<String> queries,
            List<RetrievalHit> evidence,
            boolean fallbackUsed
    ) {
    }

    public record QuestionGenerationInput(
            WorkflowRequest request,
            MaterialUnderstandingOutput understanding,
            RetrievalPlanningOutput retrieval
    ) {
    }

    public record QuestionGenerationOutput(
            List<QuestionRecord> candidates,
            boolean grounded,
            Integer tokenUsage
    ) {
    }

    public record QualityReviewInput(
            WorkflowRequest request,
            QuestionGenerationOutput generation,
            RetrievalPlanningOutput retrieval
    ) {
    }

    public record QuestionReview(
            String questionId,
            ReviewDecision decision,
            List<String> reasons
    ) {
    }

    public record QualityReviewOutput(List<QuestionReview> reviews) {
    }

    public record DedupDifficultyInput(
            WorkflowRequest request,
            QuestionGenerationOutput generation,
            QualityReviewOutput review
    ) {
    }

    public record DedupDifficultyOutput(
            List<QuestionRecord> candidates,
            List<String> duplicateQuestionIds,
            Map<String, QuestionDifficulty> assessedDifficulties
    ) {
    }

    public record TeachingCompositionInput(
            WorkflowRequest request,
            DedupDifficultyOutput deduplication,
            QualityReviewOutput review
    ) {
    }

    public record TeachingCompositionOutput(
            List<QuestionRecord> candidates,
            List<String> teacherRecommendations
    ) {
    }

    public record ExecutionResult(AgentRunRecord run, List<QuestionRecord> questions) {
    }
}
