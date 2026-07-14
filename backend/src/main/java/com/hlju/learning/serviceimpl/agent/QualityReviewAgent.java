package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionReview;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.question.QuestionOption;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QualityReviewAgent implements AgentRoleExecutor<QualityReviewInput, QualityReviewOutput> {
    @Override
    public AgentRole role() {
        return AgentRole.QUALITY_REVIEW;
    }

    @Override
    public QualityReviewOutput execute(QualityReviewInput input, AgentExecutionContext context) {
        return context.call("reviewQuestionCandidates",
                Map.of("candidateCount", input.generation().candidates().size(),
                        "evidenceCount", input.retrieval().evidence().size()),
                () -> review(input),
                output -> Map.of("decisions", output.reviews().stream()
                        .collect(java.util.stream.Collectors.groupingBy(QuestionReview::decision,
                                java.util.stream.Collectors.counting()))));
    }

    private QualityReviewOutput review(QualityReviewInput input) {
        Map<String, String> evidenceByChunk = new HashMap<>();
        input.retrieval().evidence().forEach(hit -> evidenceByChunk.put(hit.chunk().chunkId(), hit.chunk().text()));
        return new QualityReviewOutput(input.generation().candidates().stream()
                .map(question -> reviewQuestion(question, evidenceByChunk))
                .toList());
    }

    private QuestionReview reviewQuestion(QuestionRecord question, Map<String, String> evidenceByChunk) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(question.prompt()) || isBlank(question.answerText())) {
            reasons.add("Question prompt and answer must both be present.");
            return new QuestionReview(question.questionId(), ReviewDecision.REJECT, reasons);
        }
        if (question.type() == QuestionType.SINGLE_CHOICE) {
            long correctCount = question.options().stream().filter(QuestionOption::correct).count();
            boolean answerMatches = question.options().stream()
                    .anyMatch(option -> option.correct() && option.label().equalsIgnoreCase(question.answerText().trim()));
            if (correctCount != 1 || !answerMatches) {
                reasons.add("Single-choice questions require one correct option matching the answer.");
                return new QuestionReview(question.questionId(), ReviewDecision.REJECT, reasons);
            }
        }
        if (question.type() == QuestionType.TRUE_FALSE
                && !("true".equalsIgnoreCase(question.answerText().trim())
                || "false".equalsIgnoreCase(question.answerText().trim()))) {
            reasons.add("True/false questions require a boolean reference answer.");
            return new QuestionReview(question.questionId(), ReviewDecision.REJECT, reasons);
        }
        if (question.sourceRefs() == null || question.sourceRefs().isEmpty()
                || question.sourceRefs().stream().anyMatch(this::requiresHumanReview)
                || question.sourceRefs().stream().anyMatch(ref -> !matchesEvidence(ref, evidenceByChunk))) {
            reasons.add("Retrieved evidence is missing or cannot be matched to the candidate source reference.");
            return new QuestionReview(question.questionId(), ReviewDecision.HUMAN_REVIEW, reasons);
        }
        if (question.type() == QuestionType.FILL_BLANK && question.sourceRefs().stream()
                .noneMatch(ref -> normalize(ref.snippet()).contains(normalize(question.answerText())))) {
            reasons.add("The fill-blank answer cannot be found in the cited evidence.");
            return new QuestionReview(question.questionId(), ReviewDecision.HUMAN_REVIEW, reasons);
        }
        if (isBlank(question.analysisText())) {
            reasons.add("Add an evidence-grounded explanation before teacher approval.");
            return new QuestionReview(question.questionId(), ReviewDecision.REVISE, reasons);
        }
        reasons.add("Grounding, answer uniqueness, format, and reviewability checks passed.");
        return new QuestionReview(question.questionId(), ReviewDecision.PASS, reasons);
    }

    private boolean requiresHumanReview(QuestionSourceRef ref) {
        return ref == null || ref.chunkId() == null || ref.chunkId().isBlank()
                || ref.snippet() == null || ref.snippet().isBlank() || ref.score() <= 0;
    }

    private boolean matchesEvidence(QuestionSourceRef ref, Map<String, String> evidenceByChunk) {
        String source = evidenceByChunk.get(ref.chunkId());
        if (source == null) {
            return false;
        }
        String snippet = normalize(ref.snippet()).replaceFirst("\\.\\.\\.$", "");
        return !snippet.isBlank() && normalize(source).contains(snippet);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
