package com.hlju.learning.domain.agent;

import java.time.Instant;
import java.util.List;

public record AgentRunRecord(
        String runId,
        String taskId,
        String objective,
        AgentRunStatus status,
        List<AgentStepReport> steps,
        List<AgentToolCall> toolCalls,
        String finalAnswer,
        Instant createdAt,
        AgentWorkflowMode workflowMode,
        AgentRole failedRole,
        String errorSummary,
        Instant startedAt,
        Instant finishedAt,
        AgentRunMetrics metrics
) {
}
