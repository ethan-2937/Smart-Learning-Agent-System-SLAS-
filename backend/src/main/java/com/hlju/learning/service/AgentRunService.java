package com.hlju.learning.service;

import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentWorkflowTemplate;
import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.rag.RetrievalHit;

import java.util.List;

public interface AgentRunService {
    AgentWorkflowTemplate getWorkflowTemplate();

    AgentRunRecord runQuestionWorkflow(String taskId, GenerateQuestionRequest request, List<RetrievalHit> hits, List<QuestionRecord> questions);

    AgentRunRecord getRun(String runId);

    List<AgentRunRecord> listRuns();
}
