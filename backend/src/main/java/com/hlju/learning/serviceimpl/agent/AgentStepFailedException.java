package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;

public class AgentStepFailedException extends RuntimeException {
    private final AgentRole role;

    public AgentStepFailedException(AgentRole role, Throwable cause) {
        super(cause);
        this.role = role;
    }

    public AgentRole role() {
        return role;
    }
}
