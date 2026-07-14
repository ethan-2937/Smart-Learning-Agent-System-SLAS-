package com.hlju.learning.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentWorkflowData.DedupDifficultyOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.MaterialUnderstandingOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionReview;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.agent.AgentWorkflowData.TeachingCompositionOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.WorkflowRequest;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.MaterialStatus;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.hlju.learning.repository.memory.MemoryAgentRunRepository;
import com.hlju.learning.serviceimpl.agent.AgentExecutionContext;
import com.hlju.learning.serviceimpl.agent.AgentExecutionRuntime;
import com.hlju.learning.serviceimpl.agent.AgentRoleExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunServiceImplTest {
    @Test
    void orchestratesRealOutputsInOrderAndRecordsActualCalls() {
        List<AgentRole> order = new ArrayList<>();
        MaterialChunk chunk = chunk();
        RetrievalHit hit = new RetrievalHit(chunk, 0.9, "stub retrieval");
        QuestionRecord question = question("draft-from-query-from-understood", chunk);
        Map<AgentRole, Function<Object, Object>> actions = new LinkedHashMap<>();
        actions.put(AgentRole.MATERIAL_UNDERSTANDING,
                input -> new MaterialUnderstandingOutput("understood", List.of("payment"), List.of(chunk)));
        actions.put(AgentRole.RETRIEVAL_PLANNING, input -> {
            RetrievalPlanningInput typed = (RetrievalPlanningInput) input;
            return new RetrievalPlanningOutput(List.of("query-from-" + typed.understanding().theme()), List.of(hit), false);
        });
        actions.put(AgentRole.QUESTION_GENERATION, input -> new QuestionGenerationOutput(List.of(question), true, 0));
        actions.put(AgentRole.QUALITY_REVIEW, input -> new QualityReviewOutput(List.of(
                new QuestionReview(question.questionId(), ReviewDecision.PASS, List.of("checked")))));
        actions.put(AgentRole.DEDUP_DIFFICULTY, input -> new DedupDifficultyOutput(
                List.of(question), List.of(), Map.of(question.questionId(), QuestionDifficulty.MEDIUM)));
        actions.put(AgentRole.TEACHING_COMPOSER,
                input -> new TeachingCompositionOutput(List.of(question), List.of("teacher review")));

        AgentRunServiceImpl service = service(order, actions);
        var result = service.runQuestionWorkflow(request(AgentWorkflowMode.RAG_MULTI_AGENT));

        assertThat(order).containsExactly(AgentRole.values());
        assertThat(result.run().status()).isEqualTo(AgentRunStatus.FINISHED);
        assertThat(result.run().steps()).hasSize(6).allMatch(step -> step.status() == AgentRunStatus.FINISHED);
        assertThat(result.run().steps().get(2).inputJson()).contains("query-from-understood");
        assertThat(result.run().steps().get(3).inputJson()).contains("draft-from-query-from-understood");
        assertThat(result.run().toolCalls()).hasSize(6).allMatch(call -> call.success() && call.role() != null);
        assertThat(result.run().toolCalls()).allSatisfy(call -> {
            assertThat(call.startedAt()).isNotNull();
            assertThat(call.finishedAt()).isAfterOrEqualTo(call.startedAt());
        });
        assertThat(result.run().metrics().reviewPassCount()).isOne();
    }

    @Test
    void failedRoleIsPersistedAndLaterRolesAreNotPretendedFinished() {
        List<AgentRole> order = new ArrayList<>();
        Map<AgentRole, Function<Object, Object>> actions = successfulActions();
        actions.put(AgentRole.RETRIEVAL_PLANNING, input -> {
            throw new IllegalStateException("vector store unavailable");
        });
        AgentRunServiceImpl service = service(order, actions);

        var result = service.runQuestionWorkflow(request(AgentWorkflowMode.RAG_MULTI_AGENT));

        assertThat(order).containsExactly(AgentRole.MATERIAL_UNDERSTANDING, AgentRole.RETRIEVAL_PLANNING);
        assertThat(result.questions()).isEmpty();
        assertThat(result.run().status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(result.run().failedRole()).isEqualTo(AgentRole.RETRIEVAL_PLANNING);
        assertThat(result.run().errorSummary()).contains("vector store unavailable");
        assertThat(result.run().steps()).extracting(step -> step.status())
                .containsExactly(AgentRunStatus.FINISHED, AgentRunStatus.FAILED);
        assertThat(result.run().toolCalls().getLast().success()).isFalse();
    }

    private AgentRunServiceImpl service(List<AgentRole> order, Map<AgentRole, Function<Object, Object>> actions) {
        List<AgentRoleExecutor<?, ?>> executors = new ArrayList<>();
        actions.forEach((role, action) -> executors.add(stub(role, order, action)));
        return new AgentRunServiceImpl(new MemoryAgentRunRepository(), new TestRuntime(),
                new ObjectMapper().findAndRegisterModules(), executors);
    }

    private AgentRoleExecutor<Object, Object> stub(AgentRole role, List<AgentRole> order,
                                                    Function<Object, Object> action) {
        return new AgentRoleExecutor<>() {
            @Override
            public AgentRole role() {
                return role;
            }

            @Override
            public Object execute(Object input, AgentExecutionContext context) {
                order.add(role);
                return context.call("execute-" + role, Map.of("role", role), () -> action.apply(input));
            }
        };
    }

    private Map<AgentRole, Function<Object, Object>> successfulActions() {
        MaterialChunk chunk = chunk();
        RetrievalHit hit = new RetrievalHit(chunk, 0.9, "stub");
        QuestionRecord question = question("draft", chunk);
        Map<AgentRole, Function<Object, Object>> actions = new LinkedHashMap<>();
        actions.put(AgentRole.MATERIAL_UNDERSTANDING,
                input -> new MaterialUnderstandingOutput("topic", List.of("payment"), List.of(chunk)));
        actions.put(AgentRole.RETRIEVAL_PLANNING,
                input -> new RetrievalPlanningOutput(List.of("query"), List.of(hit), false));
        actions.put(AgentRole.QUESTION_GENERATION, input -> new QuestionGenerationOutput(List.of(question), true, 0));
        actions.put(AgentRole.QUALITY_REVIEW, input -> new QualityReviewOutput(List.of(
                new QuestionReview(question.questionId(), ReviewDecision.PASS, List.of("checked")))));
        actions.put(AgentRole.DEDUP_DIFFICULTY, input -> new DedupDifficultyOutput(
                List.of(question), List.of(), Map.of(question.questionId(), QuestionDifficulty.MEDIUM)));
        actions.put(AgentRole.TEACHING_COMPOSER,
                input -> new TeachingCompositionOutput(List.of(question), List.of("review")));
        return actions;
    }

    private WorkflowRequest request(AgentWorkflowMode mode) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MaterialRecord material = new MaterialRecord("material-1", "Payment Terms", SubjectPreset.BUSINESS_ENGLISH,
                "terms.txt", "text/plain", "terms.txt", MaterialStatus.INDEXED, 1, null, now, now);
        return new WorkflowRequest("task-1", material, SubjectPreset.BUSINESS_ENGLISH, "payment",
                List.of(QuestionType.SHORT_ANSWER), QuestionDifficulty.MEDIUM, 1, mode);
    }

    private MaterialChunk chunk() {
        return new MaterialChunk("chunk-1", "material-1", "chapter-1", "Trade Terms", 0, 1,
                "fixture", "Payment is due within thirty days.", List.of("payment"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private QuestionRecord question(String prompt, MaterialChunk chunk) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        QuestionSourceRef source = new QuestionSourceRef(chunk.materialId(), chunk.chunkId(), chunk.chapterTitle(),
                chunk.pageNo(), chunk.text(), 0.9);
        return new QuestionRecord("question-1", "task-1", chunk.materialId(), QuestionType.SHORT_ANSWER,
                QuestionDifficulty.MEDIUM, SubjectPreset.BUSINESS_ENGLISH, prompt, List.of(), "Thirty days",
                "The answer is stated in the source.", List.of(source), QuestionStatus.PENDING_REVIEW, now, now);
    }

    private static final class TestRuntime implements AgentExecutionRuntime {
        private final AtomicInteger sequence = new AtomicInteger();
        private final Instant base = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public Instant now() {
            return base.plusMillis(sequence.incrementAndGet());
        }

        @Override
        public String newId() {
            return "id-" + sequence.incrementAndGet();
        }
    }
}
