package com.hlju.learning.domain.agent;

import java.time.Instant;

public record AgentStepReport(
        String stepId,
        AgentRole role,
        String goal,
        String summary,
        AgentRunStatus status,
        String inputJson,
        String outputJson,
        Instant startedAt,
        Instant finishedAt,
        int attempt,
        String errorSummary
) {
}
