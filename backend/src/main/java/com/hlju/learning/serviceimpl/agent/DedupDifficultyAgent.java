package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.DedupDifficultyInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.DedupDifficultyOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionReview;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.repository.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DedupDifficultyAgent implements AgentRoleExecutor<DedupDifficultyInput, DedupDifficultyOutput> {
    private final QuestionRepository questionRepository;

    public DedupDifficultyAgent(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public AgentRole role() {
        return AgentRole.DEDUP_DIFFICULTY;
    }

    @Override
    public DedupDifficultyOutput execute(DedupDifficultyInput input, AgentExecutionContext context) {
        List<QuestionRecord> existing = context.call("listExistingQuestions",
                Map.of("materialId", input.request().material().materialId()),
                questionRepository::findAllQuestions,
                values -> Map.of("questionCount", values.size()));
        return context.call("deduplicateAndAssessDifficulty",
                Map.of("candidateCount", input.generation().candidates().size(), "existingCount", existing.size()),
                () -> process(input, existing),
                output -> Map.of("acceptedCount", output.candidates().size(),
                        "duplicateQuestionIds", output.duplicateQuestionIds(),
                        "assessedDifficulties", output.assessedDifficulties()));
    }

    private DedupDifficultyOutput process(DedupDifficultyInput input, List<QuestionRecord> existing) {
        Map<String, ReviewDecision> decisions = new LinkedHashMap<>();
        input.review().reviews().forEach(review -> decisions.put(review.questionId(), review.decision()));
        Set<String> seenPrompts = new HashSet<>();
        existing.stream()
                .filter(question -> input.request().material().materialId().equals(question.materialId()))
                .map(QuestionRecord::prompt)
                .map(this::normalize)
                .forEach(seenPrompts::add);

        List<QuestionRecord> accepted = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        Map<String, QuestionDifficulty> assessed = new LinkedHashMap<>();
        for (QuestionRecord candidate : input.generation().candidates()) {
            if (decisions.get(candidate.questionId()) == ReviewDecision.REJECT) {
                continue;
            }
            String normalized = normalize(candidate.prompt());
            if (!seenPrompts.add(normalized)) {
                duplicates.add(candidate.questionId());
                continue;
            }
            QuestionDifficulty difficulty = assessDifficulty(candidate);
            assessed.put(candidate.questionId(), difficulty);
            accepted.add(withDifficulty(candidate, difficulty));
        }
        return new DedupDifficultyOutput(List.copyOf(accepted), List.copyOf(duplicates), Map.copyOf(assessed));
    }

    private QuestionDifficulty assessDifficulty(QuestionRecord question) {
        int length = question.prompt() == null ? 0 : question.prompt().length();
        if (length > 260 || question.type().name().contains("WRITING")) {
            return QuestionDifficulty.HARD;
        }
        if (length < 100 && (question.type().name().contains("TRUE_FALSE")
                || question.type().name().contains("FILL_BLANK"))) {
            return QuestionDifficulty.EASY;
        }
        return QuestionDifficulty.MEDIUM;
    }

    private QuestionRecord withDifficulty(QuestionRecord question, QuestionDifficulty difficulty) {
        return new QuestionRecord(question.questionId(), question.taskId(), question.materialId(), question.type(),
                difficulty, question.subjectPreset(), question.prompt(), question.options(), question.answerText(),
                question.analysisText(), question.sourceRefs(), question.status(), question.createdAt(), question.updatedAt());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
