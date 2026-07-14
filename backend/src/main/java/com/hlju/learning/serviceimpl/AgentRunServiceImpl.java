package com.hlju.learning.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentRunMetrics;
import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentStep;
import com.hlju.learning.domain.agent.AgentWorkflowData.DedupDifficultyInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.DedupDifficultyOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.ExecutionResult;
import com.hlju.learning.domain.agent.AgentWorkflowData.MaterialUnderstandingInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.MaterialUnderstandingOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionReview;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.agent.AgentWorkflowData.TeachingCompositionInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.TeachingCompositionOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.WorkflowRequest;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.agent.AgentWorkflowTemplate;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.repository.AgentRunRepository;
import com.hlju.learning.service.AgentRunService;
import com.hlju.learning.serviceimpl.agent.AgentExecutionRuntime;
import com.hlju.learning.serviceimpl.agent.AgentRoleExecutor;
import com.hlju.learning.serviceimpl.agent.AgentRunTrace;
import com.hlju.learning.serviceimpl.agent.AgentStepFailedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
public class AgentRunServiceImpl implements AgentRunService {
    private static final Map<AgentRole, String> GOALS = Map.of(
            AgentRole.MATERIAL_UNDERSTANDING, "Analyze material topic, knowledge points, and candidate evidence.",
            AgentRole.RETRIEVAL_PLANNING, "Plan queries and execute evidence retrieval.",
            AgentRole.QUESTION_GENERATION, "Generate structured reviewable candidates from the available context.",
            AgentRole.QUALITY_REVIEW, "Independently review grounding, answers, format, and reviewability.",
            AgentRole.DEDUP_DIFFICULTY, "Remove duplicate or rejected candidates and assess difficulty.",
            AgentRole.TEACHING_COMPOSER, "Compose final candidates and teacher review recommendations."
    );

    private final AgentRunRepository repository;
    private final AgentExecutionRuntime runtime;
    private final ObjectMapper objectMapper;
    private final Map<AgentRole, AgentRoleExecutor<?, ?>> executors;

    public AgentRunServiceImpl(AgentRunRepository repository, AgentExecutionRuntime runtime,
                               ObjectMapper objectMapper, List<AgentRoleExecutor<?, ?>> executors) {
        this.repository = repository;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
        this.executors = new EnumMap<>(AgentRole.class);
        executors.forEach(executor -> this.executors.put(executor.role(), executor));
        for (AgentRole role : AgentRole.values()) {
            if (!this.executors.containsKey(role)) {
                throw new IllegalArgumentException("Missing agent executor for role " + role);
            }
        }
    }

    @Override
    public AgentWorkflowTemplate getWorkflowTemplate() {
        return new AgentWorkflowTemplate("smart-learning-agent-workflow",
                "Executable evidence-grounded multi-agent question workflow", List.of(
                step(AgentRole.MATERIAL_UNDERSTANDING, "listMaterialChunks", "analyzeMaterialEvidence"),
                step(AgentRole.RETRIEVAL_PLANNING, "planRetrievalQueries", "retrieveChunks",
                        "selectMaterialFallbackEvidence"),
                step(AgentRole.QUESTION_GENERATION, "generateQuestionDrafts", "llm.complete"),
                step(AgentRole.QUALITY_REVIEW, "reviewQuestionCandidates"),
                step(AgentRole.DEDUP_DIFFICULTY, "listExistingQuestions", "deduplicateAndAssessDifficulty"),
                step(AgentRole.TEACHING_COMPOSER, "composeTeacherReviewPackage")
        ));
    }

    @Override
    public ExecutionResult runQuestionWorkflow(WorkflowRequest request) {
        AgentRunTrace trace = new AgentRunTrace(request.taskId(),
                "Generate reviewable questions using mode " + request.mode(), request.mode(),
                repository, runtime, objectMapper);
        try {
            MaterialUnderstandingOutput understanding = null;
            RetrievalPlanningOutput retrieval = null;
            if (request.mode() != AgentWorkflowMode.DIRECT) {
                understanding = execute(trace, AgentRole.MATERIAL_UNDERSTANDING,
                        new MaterialUnderstandingInput(request));
                retrieval = execute(trace, AgentRole.RETRIEVAL_PLANNING,
                        new RetrievalPlanningInput(request, understanding));
            }

            QuestionGenerationOutput generation = execute(trace, AgentRole.QUESTION_GENERATION,
                    new QuestionGenerationInput(request, understanding, retrieval));
            List<QuestionRecord> finalQuestions = generation.candidates();
            QualityReviewOutput review = null;
            DedupDifficultyOutput deduplication = null;
            if (request.mode() == AgentWorkflowMode.RAG_MULTI_AGENT) {
                review = execute(trace, AgentRole.QUALITY_REVIEW,
                        new QualityReviewInput(request, generation, retrieval));
                deduplication = execute(trace, AgentRole.DEDUP_DIFFICULTY,
                        new DedupDifficultyInput(request, generation, review));
                TeachingCompositionOutput composition = execute(trace, AgentRole.TEACHING_COMPOSER,
                        new TeachingCompositionInput(request, deduplication, review));
                finalQuestions = composition.candidates();
            }
            AgentRunMetrics metrics = metrics(trace, generation, retrieval, review, deduplication);
            String finalAnswer = "Workflow finished in " + request.mode() + " mode with "
                    + finalQuestions.size() + " reviewable candidate(s).";
            return new ExecutionResult(trace.finish(finalAnswer, metrics), finalQuestions);
        } catch (AgentStepFailedException ex) {
            return new ExecutionResult(trace.current(), List.of());
        }
    }

    @Override
    public AgentRunRecord getRun(String runId) {
        return repository.find(runId)
                .orElseThrow(() -> new IllegalArgumentException("Agent run not found: " + runId));
    }

    @Override
    public List<AgentRunRecord> listRuns() {
        return repository.findAll();
    }

    @Override
    public List<AgentRunRecord> listRuns(AgentWorkflowMode mode) {
        return repository.findAll().stream().filter(run -> run.workflowMode() == mode).toList();
    }

    @SuppressWarnings("unchecked")
    private <I, O> O execute(AgentRunTrace trace, AgentRole role, I input) {
        AgentRoleExecutor<I, O> executor = (AgentRoleExecutor<I, O>) executors.get(role);
        return trace.execute(executor, GOALS.get(role), input);
    }

    private AgentStep step(AgentRole role, String... tools) {
        return new AgentStep(role, GOALS.get(role), List.of(tools));
    }

    private AgentRunMetrics metrics(AgentRunTrace trace, QuestionGenerationOutput generation,
                                    RetrievalPlanningOutput retrieval, QualityReviewOutput review,
                                    DedupDifficultyOutput deduplication) {
        List<QuestionReview> reviews = review == null ? List.of() : review.reviews();
        long durationMs = Math.max(0, Duration.between(trace.current().startedAt(), runtime.now()).toMillis());
        return new AgentRunMetrics(generation.candidates().size(),
                generation.grounded() ? generation.candidates().size() : 0,
                retrieval == null ? 0 : retrieval.evidence().size(),
                count(reviews, ReviewDecision.PASS), count(reviews, ReviewDecision.REVISE),
                count(reviews, ReviewDecision.HUMAN_REVIEW), count(reviews, ReviewDecision.REJECT),
                deduplication == null ? 0 : deduplication.duplicateQuestionIds().size(), durationMs,
                generation.tokenUsage());
    }

    private int count(List<QuestionReview> reviews, ReviewDecision decision) {
        Predicate<QuestionReview> matches = review -> review.decision() == decision;
        return (int) reviews.stream().filter(matches).count();
    }
}
