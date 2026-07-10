package com.hlju.learning.domain.agent;

public record AgentStepReport(
        String stepId,
        AgentRole role,
        String goal,
        String summary,
        AgentRunStatus status
) {
}
