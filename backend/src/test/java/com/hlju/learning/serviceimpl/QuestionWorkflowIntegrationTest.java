package com.hlju.learning.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.ai.LlmClient;
import com.hlju.learning.config.AiProperties;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.MaterialStatus;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.GenerationTaskStatus;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.hlju.learning.domain.rag.RetrievalResult;
import com.hlju.learning.repository.memory.MemoryAgentRunRepository;
import com.hlju.learning.repository.memory.MemoryQuestionRepository;
import com.hlju.learning.service.MaterialService;
import com.hlju.learning.service.VectorRetrievalService;
import com.hlju.learning.serviceimpl.agent.AgentExecutionRuntime;
import com.hlju.learning.serviceimpl.agent.AgentRoleExecutor;
import com.hlju.learning.serviceimpl.agent.DedupDifficultyAgent;
import com.hlju.learning.serviceimpl.agent.MaterialUnderstandingAgent;
import com.hlju.learning.serviceimpl.agent.QualityReviewAgent;
import com.hlju.learning.serviceimpl.agent.QuestionGenerationAgent;
import com.hlju.learning.serviceimpl.agent.RetrievalPlanningAgent;
import com.hlju.learning.serviceimpl.agent.TeachingComposerAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionWorkflowIntegrationTest {
    @ParameterizedTest
    @EnumSource(AgentWorkflowMode.class)
    void allExperimentModesRunDeterministicallyWithoutNetworkAndKeepReviewInvariants(AgentWorkflowMode mode) {
        Fixture fixture = fixture(false);

        var task = fixture.service().generate(request(mode));

        assertThat(task.status()).isEqualTo(GenerationTaskStatus.FINISHED);
        assertThat(task.questions()).hasSize(2)
                .allMatch(question -> question.status() == QuestionStatus.PENDING_REVIEW)
                .allMatch(question -> question.sourceRefs() != null && !question.sourceRefs().isEmpty());
        var run = fixture.agentRepository().find(task.agentRunId()).orElseThrow();
        assertThat(run.workflowMode()).isEqualTo(mode);
        assertThat(run.status()).isEqualTo(AgentRunStatus.FINISHED);
        assertThat(run.steps()).hasSize(expectedStepCount(mode));
        assertThat(run.toolCalls()).allMatch(call -> call.success() && call.startedAt() != null && call.finishedAt() != null);
        if (mode == AgentWorkflowMode.DIRECT) {
            assertThat(task.questions()).allMatch(question -> question.sourceRefs().getFirst().chunkId() == null)
                    .allMatch(question -> question.sourceRefs().getFirst().snippet().contains("teacher confirmation"));
        } else {
            assertThat(task.questions()).allMatch(question -> "chunk-1".equals(question.sourceRefs().getFirst().chunkId()));
        }
        verify(fixture.llmClient(), never()).completeWithMetadata(anyString(), anyString());
    }

    @Test
    void roleFailureLeavesGenerationTaskFailedAndPersistsFailedRun() {
        Fixture fixture = fixture(true);

        var task = fixture.service().generate(request(AgentWorkflowMode.RAG_MULTI_AGENT));

        assertThat(task.status()).isEqualTo(GenerationTaskStatus.FAILED);
        assertThat(task.questions()).isEmpty();
        var run = fixture.agentRepository().find(task.agentRunId()).orElseThrow();
        assertThat(run.status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(run.failedRole()).isEqualTo(com.hlju.learning.domain.agent.AgentRole.RETRIEVAL_PLANNING);
        assertThat(run.steps()).hasSize(2);
        assertThat(fixture.questionRepository().findAllQuestions()).isEmpty();
    }

    @Test
    void runCollectionCanFilterThreeModesAndExposeDeterministicObservationFields() {
        Fixture fixture = fixture(false);
        for (AgentWorkflowMode mode : AgentWorkflowMode.values()) {
            fixture.service().generate(request(mode));
        }

        for (AgentWorkflowMode mode : AgentWorkflowMode.values()) {
            var runs = fixture.agentService().listRuns(mode);
            assertThat(runs).hasSize(1);
            assertThat(runs.getFirst().metrics().generatedQuestionCount()).isEqualTo(2);
            assertThat(runs.getFirst().metrics().tokenUsage()).isZero();
        }
        assertThat(fixture.agentService().listRuns(AgentWorkflowMode.DIRECT).getFirst()
                .metrics().groundedQuestionCount()).isZero();
        assertThat(fixture.agentService().listRuns(AgentWorkflowMode.RAG_ONLY).getFirst()
                .metrics().groundedQuestionCount()).isEqualTo(2);
    }

    @Test
    void omittedModeUsesFullMultiAgentWorkflowForExistingApiClients() throws Exception {
        String oldPayload = """
                {"materialId":"material-1","subjectPreset":"BUSINESS_ENGLISH","topic":"payment",
                 "questionTypes":["SHORT_ANSWER"],"difficulty":"MEDIUM","count":1}
                """;
        GenerateQuestionRequest request = new ObjectMapper().findAndRegisterModules()
                .readValue(oldPayload, GenerateQuestionRequest.class);

        assertThat(request.resolvedWorkflowMode()).isEqualTo(AgentWorkflowMode.RAG_MULTI_AGENT);
    }

    private Fixture fixture(boolean failRetrieval) {
        TestRuntime runtime = new TestRuntime();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MaterialRecord material = material();
        MaterialChunk chunk = chunk();
        MaterialService materialService = mock(MaterialService.class);
        when(materialService.getMaterial("material-1")).thenReturn(material);
        when(materialService.listChunks("material-1")).thenReturn(List.of(chunk));
        VectorRetrievalService retrievalService = mock(VectorRetrievalService.class);
        if (failRetrieval) {
            when(retrievalService.retrieve(anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalStateException("retrieval unavailable"));
        } else {
            when(retrievalService.retrieve(anyString(), anyString(), anyInt()))
                    .thenAnswer(invocation -> new RetrievalResult(invocation.getArgument(0),
                            List.of(new RetrievalHit(chunk, 0.91, "deterministic fixture"))));
        }
        LlmClient llmClient = mock(LlmClient.class);
        AiProperties properties = new AiProperties("mock", "", "/chat/completions", "", "mock-model",
                0.2, 10, 1000, false, "medium");
        MemoryQuestionRepository questionRepository = new MemoryQuestionRepository();
        MemoryAgentRunRepository agentRepository = new MemoryAgentRunRepository();
        List<AgentRoleExecutor<?, ?>> executors = List.of(
                new MaterialUnderstandingAgent(materialService),
                new RetrievalPlanningAgent(retrievalService),
                new QuestionGenerationAgent(llmClient, properties, objectMapper, runtime),
                new QualityReviewAgent(),
                new DedupDifficultyAgent(questionRepository),
                new TeachingComposerAgent());
        AgentRunServiceImpl agentService = new AgentRunServiceImpl(agentRepository, runtime, objectMapper, executors);
        QuestionGenerationServiceImpl service = new QuestionGenerationServiceImpl(materialService, agentService,
                questionRepository, runtime);
        return new Fixture(service, agentService, questionRepository, agentRepository, llmClient);
    }

    private GenerateQuestionRequest request(AgentWorkflowMode mode) {
        return new GenerateQuestionRequest("material-1", SubjectPreset.BUSINESS_ENGLISH, "payment terms",
                List.of(QuestionType.SINGLE_CHOICE, QuestionType.SHORT_ANSWER),
                QuestionDifficulty.MEDIUM, 2, mode);
    }

    private int expectedStepCount(AgentWorkflowMode mode) {
        return switch (mode) {
            case DIRECT -> 1;
            case RAG_ONLY -> 3;
            case RAG_MULTI_AGENT -> 6;
        };
    }

    private MaterialRecord material() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new MaterialRecord("material-1", "Payment Terms", SubjectPreset.BUSINESS_ENGLISH,
                "terms.txt", "text/plain", "terms.txt", MaterialStatus.INDEXED, 1, null, now, now);
    }

    private MaterialChunk chunk() {
        return new MaterialChunk("chunk-1", "material-1", "chapter-1", "Trade Terms", 0, 1,
                "fixture", "Payment is due within thirty days after the invoice date.",
                List.of("payment", "invoice"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private record Fixture(QuestionGenerationServiceImpl service, AgentRunServiceImpl agentService,
                           MemoryQuestionRepository questionRepository, MemoryAgentRunRepository agentRepository,
                           LlmClient llmClient) {
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
