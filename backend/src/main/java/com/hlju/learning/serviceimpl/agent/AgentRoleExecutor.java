package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;

public interface AgentRoleExecutor<I, O> {
    AgentRole role();

    O execute(I input, AgentExecutionContext context);
}
