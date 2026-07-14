package com.hlju.learning.serviceimpl.agent;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SystemAgentExecutionRuntime implements AgentExecutionRuntime {
    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}
