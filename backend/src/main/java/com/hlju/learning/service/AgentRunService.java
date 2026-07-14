package com.hlju.learning.service;

import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentWorkflowTemplate;
import com.hlju.learning.domain.agent.AgentWorkflowData.ExecutionResult;
import com.hlju.learning.domain.agent.AgentWorkflowData.WorkflowRequest;
import com.hlju.learning.domain.agent.AgentWorkflowMode;

import java.util.List;

public interface AgentRunService {
    AgentWorkflowTemplate getWorkflowTemplate();

    ExecutionResult runQuestionWorkflow(WorkflowRequest request);

    AgentRunRecord getRun(String runId);

    List<AgentRunRecord> listRuns();

    List<AgentRunRecord> listRuns(AgentWorkflowMode mode);
}
