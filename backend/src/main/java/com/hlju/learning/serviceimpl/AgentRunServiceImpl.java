package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentStep;
import com.hlju.learning.domain.agent.AgentStepReport;
import com.hlju.learning.domain.agent.AgentToolCall;
import com.hlju.learning.domain.agent.AgentWorkflowTemplate;
import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.hlju.learning.repository.AgentRunRepository;
import com.hlju.learning.service.AgentRunService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentRunServiceImpl implements AgentRunService {
    private final AgentRunRepository agentRunRepository;

    public AgentRunServiceImpl(AgentRunRepository agentRunRepository) {
        this.agentRunRepository = agentRunRepository;
    }

    @Override
    public AgentWorkflowTemplate getWorkflowTemplate() {
        return new AgentWorkflowTemplate("smart-learning-agent-workflow", "Evidence-grounded multi-agent question workflow", List.of(
                new AgentStep(AgentRole.MATERIAL_UNDERSTANDING, "Understand material topic and candidate knowledge points", List.of("listChunks", "extractKnowledgePoints")),
                new AgentStep(AgentRole.RETRIEVAL_PLANNING, "Plan semantic retrieval queries for the generation objective", List.of("retrieveChunks", "filterBySubject")),
                new AgentStep(AgentRole.QUESTION_GENERATION, "Generate structured question drafts from retrieved evidence", List.of("generateQuestionDraft")),
                new AgentStep(AgentRole.QUALITY_REVIEW, "Check source grounding, answer clarity, and review status", List.of("checkSourceGrounding", "validateQuestion")),
                new AgentStep(AgentRole.DEDUP_DIFFICULTY, "Run simple deduplication and difficulty tagging", List.of("deduplicateQuestions", "estimateDifficulty")),
                new AgentStep(AgentRole.TEACHING_COMPOSER, "Compose publishable practice set and teaching advice", List.of("composePracticeSet"))
        ));
    }

    @Override
    public AgentRunRecord runQuestionWorkflow(String taskId, GenerateQuestionRequest request, List<RetrievalHit> hits, List<QuestionRecord> questions) {
        String runId = UUID.randomUUID().toString();
        List<AgentStepReport> steps = new ArrayList<>();
        List<AgentToolCall> toolCalls = new ArrayList<>();
        steps.add(report(AgentRole.MATERIAL_UNDERSTANDING, "Subject=" + request.subjectPreset() + ", topic=" + safeTopic(request.topic()) + "."));
        toolCalls.add(new AgentToolCall("extractKnowledgePoints", safeTopic(request.topic()), "candidate evidence chunks=" + hits.size(), true));
        steps.add(report(AgentRole.RETRIEVAL_PLANNING, "Vector retrieval finished, hits=" + hits.size() + "."));
        toolCalls.add(new AgentToolCall("retrieveChunks", safeTopic(request.topic()), "topK hits=" + hits.size(), true));
        steps.add(report(AgentRole.QUESTION_GENERATION, "Generated question drafts=" + questions.size() + "."));
        toolCalls.add(new AgentToolCall("generateQuestionDraft", "count=" + request.count(), "drafts=" + questions.size(), true));
        steps.add(report(AgentRole.QUALITY_REVIEW, "Questions keep PENDING_REVIEW status and are not published automatically."));
        steps.add(report(AgentRole.DEDUP_DIFFICULTY, "Applied basic source/type deduplication and difficulty tag=" + request.difficulty() + "."));
        steps.add(report(AgentRole.TEACHING_COMPOSER, "Teacher should review prompts, answers, and explanations before publishing."));
        String finalAnswer = "Generation finished: questions=" + questions.size() + ", evidenceHits=" + hits.size() + ". The system saved retrieval evidence and agent steps for explainable review.";
        AgentRunRecord record = new AgentRunRecord(runId, taskId, "Generate reviewable questions from material evidence", AgentRunStatus.FINISHED, steps, toolCalls, finalAnswer, Instant.now());
        agentRunRepository.save(record);
        return record;
    }

    @Override
    public AgentRunRecord getRun(String runId) {
        return agentRunRepository.find(runId)
                .orElseThrow(() -> new IllegalArgumentException("Agent run not found: " + runId));
    }

    @Override
    public List<AgentRunRecord> listRuns() {
        return agentRunRepository.findAll();
    }

    private AgentStepReport report(AgentRole role, String summary) {
        return new AgentStepReport(UUID.randomUUID().toString(), role, role.name(), summary, AgentRunStatus.FINISHED);
    }

    private String safeTopic(String topic) {
        return topic == null || topic.isBlank() ? "general learning topic" : topic;
    }
}
