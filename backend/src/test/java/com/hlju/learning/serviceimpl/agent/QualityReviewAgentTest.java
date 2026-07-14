package com.hlju.learning.serviceimpl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.QualityReviewInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningOutput;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.agent.AgentWorkflowData.WorkflowRequest;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.MaterialStatus;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionOption;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QualityReviewAgentTest {
    @Test
    void independentlyProducesPassRejectRevisionAndHumanReviewDecisions() {
        MaterialChunk chunk = chunk();
        QuestionSourceRef grounded = new QuestionSourceRef("material-1", "chunk-1", "Terms", 1,
                chunk.text(), 0.9);
        QuestionSourceRef ungrounded = new QuestionSourceRef("material-1", null, "UNGROUNDED", null,
                "Teacher confirmation required", 0);
        QuestionRecord pass = question("pass", QuestionType.SINGLE_CHOICE, "A", "grounded analysis",
                List.of(new QuestionOption("A", "Thirty days", true), new QuestionOption("B", "Never", false)), grounded);
        QuestionRecord reject = question("reject", QuestionType.SINGLE_CHOICE, "A", "analysis",
                List.of(new QuestionOption("A", "Thirty days", true), new QuestionOption("B", "Immediately", true)), grounded);
        QuestionRecord revise = question("revise", QuestionType.SHORT_ANSWER, "Thirty days", "", List.of(), grounded);
        QuestionRecord human = question("human", QuestionType.SHORT_ANSWER, "Needs confirmation", "analysis",
                List.of(), ungrounded);
        QualityReviewInput input = new QualityReviewInput(request(),
                new QuestionGenerationOutput(List.of(pass, reject, revise, human), true, 0),
                new RetrievalPlanningOutput(List.of("payment"),
                        List.of(new RetrievalHit(chunk, 0.9, "fixture")), false));

        var output = new QualityReviewAgent().execute(input,
                new AgentExecutionContext(AgentRole.QUALITY_REVIEW, new TestRuntime(),
                        new ObjectMapper().findAndRegisterModules()));

        assertThat(output.reviews()).extracting(review -> review.decision())
                .containsExactly(ReviewDecision.PASS, ReviewDecision.REJECT,
                        ReviewDecision.REVISE, ReviewDecision.HUMAN_REVIEW);
    }

    private QuestionRecord question(String id, QuestionType type, String answer, String analysis,
                                    List<QuestionOption> options, QuestionSourceRef source) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new QuestionRecord(id, "task-1", "material-1", type, QuestionDifficulty.MEDIUM,
                SubjectPreset.BUSINESS_ENGLISH, "Question " + id, options, answer, analysis,
                List.of(source), QuestionStatus.PENDING_REVIEW, now, now);
    }

    private WorkflowRequest request() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MaterialRecord material = new MaterialRecord("material-1", "Payment Terms", SubjectPreset.BUSINESS_ENGLISH,
                "terms.txt", "text/plain", "terms.txt", MaterialStatus.INDEXED, 1, null, now, now);
        return new WorkflowRequest("task-1", material, SubjectPreset.BUSINESS_ENGLISH, "payment",
                List.of(QuestionType.SINGLE_CHOICE), QuestionDifficulty.MEDIUM, 4,
                AgentWorkflowMode.RAG_MULTI_AGENT);
    }

    private MaterialChunk chunk() {
        return new MaterialChunk("chunk-1", "material-1", "chapter-1", "Terms", 0, 1, "fixture",
                "Payment is due within thirty days.", List.of("payment"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class TestRuntime implements AgentExecutionRuntime {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Instant now() {
            return Instant.parse("2026-01-01T00:00:00Z").plusMillis(sequence.incrementAndGet());
        }

        @Override
        public String newId() {
            return "id-" + sequence.incrementAndGet();
        }
    }
}
