package com.hlju.learning.serviceimpl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentToolCall;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class AgentExecutionContext {
    private final AgentRole role;
    private final AgentExecutionRuntime runtime;
    private final ObjectMapper objectMapper;
    private final List<AgentToolCall> toolCalls = new ArrayList<>();

    public AgentExecutionContext(AgentRole role, AgentExecutionRuntime runtime, ObjectMapper objectMapper) {
        this.role = role;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
    }

    public <T> T call(String toolName, Object input, Supplier<T> action) {
        return call(toolName, input, action, Function.identity());
    }

    public <T, R> T call(String toolName, Object input, Supplier<T> action, Function<T, R> outputView) {
        String callId = runtime.newId();
        Instant startedAt = runtime.now();
        try {
            T result = action.get();
            Instant finishedAt = runtime.now();
            toolCalls.add(new AgentToolCall(toolName, json(input), json(outputView.apply(result)), true,
                    callId, role, startedAt, finishedAt, null));
            return result;
        } catch (RuntimeException ex) {
            Instant finishedAt = runtime.now();
            toolCalls.add(new AgentToolCall(toolName, json(input), null, false, callId, role,
                    startedAt, finishedAt, errorSummary(ex)));
            throw ex;
        }
    }

    public List<AgentToolCall> toolCalls() {
        return List.copyOf(toolCalls);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"serializationError\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }

    private String errorSummary(RuntimeException ex) {
        String message = ex.getMessage();
        return ex.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
