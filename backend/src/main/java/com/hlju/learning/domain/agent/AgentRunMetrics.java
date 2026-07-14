package com.hlju.learning.domain.agent;

public record AgentRunMetrics(
        int generatedQuestionCount,
        int groundedQuestionCount,
        int evidenceHitCount,
        int reviewPassCount,
        int reviewRevisionCount,
        int reviewHumanCount,
        int reviewRejectCount,
        int duplicateCount,
        long durationMs,
        Integer tokenUsage
) {
    public static AgentRunMetrics empty() {
        return new AgentRunMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, null);
    }
}
