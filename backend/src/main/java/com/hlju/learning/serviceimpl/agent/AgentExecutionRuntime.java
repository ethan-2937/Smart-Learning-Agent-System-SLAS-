package com.hlju.learning.serviceimpl.agent;

import java.time.Instant;

public interface AgentExecutionRuntime {
    Instant now();

    String newId();
}
