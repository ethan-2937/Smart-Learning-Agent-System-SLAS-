package com.hlju.learning.domain.agent;

public record AgentToolCall(String toolName, String input, String output, boolean success) {
}
