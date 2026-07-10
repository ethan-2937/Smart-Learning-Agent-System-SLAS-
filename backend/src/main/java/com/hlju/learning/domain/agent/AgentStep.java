package com.hlju.learning.domain.agent;

import java.util.List;

public record AgentStep(AgentRole role, String goal, List<String> toolNames) {
}
