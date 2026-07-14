package com.hlju.learning.domain.agent;

import java.time.Instant;

public record AgentToolCall(
        String toolName,
        String input,
        String output,
        boolean success,
        String callId,
        AgentRole role,
        Instant startedAt,
        Instant finishedAt,
        String errorSummary
) {
}
