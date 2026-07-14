package com.hlju.learning.serviceimpl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentRunMetrics;
import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentStepReport;
import com.hlju.learning.domain.agent.AgentToolCall;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.repository.AgentRunRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AgentRunTrace {
    private final String runId;
    private final String taskId;
    private final String objective;
    private final AgentWorkflowMode mode;
    private final AgentRunRepository repository;
    private final AgentExecutionRuntime runtime;
    private final ObjectMapper objectMapper;
    private final Instant startedAt;
    private final List<AgentStepReport> steps = new ArrayList<>();
    private final List<AgentToolCall> toolCalls = new ArrayList<>();
    private AgentRunRecord current;

    public AgentRunTrace(String taskId, String objective, AgentWorkflowMode mode,
                         AgentRunRepository repository, AgentExecutionRuntime runtime, ObjectMapper objectMapper) {
        this.runId = runtime.newId();
        this.taskId = taskId;
        this.objective = objective;
        this.mode = mode;
        this.repository = repository;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
        this.startedAt = runtime.now();
        save(AgentRunStatus.RUNNING, "Workflow is running.", null, null, null, AgentRunMetrics.empty());
    }

    public <I, O> O execute(AgentRoleExecutor<I, O> executor, String goal, I input) {
        Instant stepStartedAt = runtime.now();
        AgentStepReport running = new AgentStepReport(runtime.newId(), executor.role(), goal,
                "Role execution started.", AgentRunStatus.RUNNING, json(input), null,
                stepStartedAt, null, 1, null);
        steps.add(running);
        save(AgentRunStatus.RUNNING, "Workflow is running.", null, null, null, AgentRunMetrics.empty());

        AgentExecutionContext context = new AgentExecutionContext(executor.role(), runtime, objectMapper);
        try {
            O output = executor.execute(input, context);
            toolCalls.addAll(context.toolCalls());
            replaceLast(new AgentStepReport(running.stepId(), executor.role(), goal,
                    "Role completed with structured output.", AgentRunStatus.FINISHED,
                    running.inputJson(), json(output), stepStartedAt, runtime.now(), 1, null));
            save(AgentRunStatus.RUNNING, "Workflow is running.", null, null, null, AgentRunMetrics.empty());
            return output;
        } catch (RuntimeException ex) {
            toolCalls.addAll(context.toolCalls());
            String error = errorSummary(ex);
            replaceLast(new AgentStepReport(running.stepId(), executor.role(), goal,
                    "Role execution failed.", AgentRunStatus.FAILED, running.inputJson(), null,
                    stepStartedAt, runtime.now(), 1, error));
            save(AgentRunStatus.FAILED, "Workflow failed at " + executor.role() + ".",
                    executor.role(), error, runtime.now(), AgentRunMetrics.empty());
            throw new AgentStepFailedException(executor.role(), ex);
        }
    }

    public AgentRunRecord finish(String finalAnswer, AgentRunMetrics metrics) {
        save(AgentRunStatus.FINISHED, finalAnswer, null, null, runtime.now(), metrics);
        return current;
    }

    public AgentRunRecord current() {
        return current;
    }

    private void replaceLast(AgentStepReport report) {
        steps.set(steps.size() - 1, report);
    }

    private void save(AgentRunStatus status, String finalAnswer, AgentRole failedRole, String error,
                      Instant finishedAt, AgentRunMetrics metrics) {
        current = new AgentRunRecord(runId, taskId, objective, status, List.copyOf(steps), List.copyOf(toolCalls),
                finalAnswer, startedAt, mode, failedRole, error, startedAt, finishedAt, metrics);
        repository.save(current);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize agent trace payload", ex);
        }
    }

    private String errorSummary(RuntimeException ex) {
        Throwable source = ex.getCause() == null ? ex : ex.getCause();
        String message = source.getMessage();
        String summary = source.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return summary.length() <= 500 ? summary : summary.substring(0, 500);
    }
}
