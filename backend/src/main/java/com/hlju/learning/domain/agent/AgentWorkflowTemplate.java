package com.hlju.learning.domain.agent;

import java.util.List;

public record AgentWorkflowTemplate(String workflowId, String title, List<AgentStep> steps) {
}
