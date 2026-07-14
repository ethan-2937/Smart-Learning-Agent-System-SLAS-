package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentRunMetrics;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentStepReport;
import com.hlju.learning.domain.agent.AgentToolCall;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.po.AgentRunPo;
import com.hlju.learning.mapper.AgentRunMapper;
import com.hlju.learning.repository.AgentRunRepository;
import com.hlju.learning.util.JsonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "mybatis")
public class MyBatisAgentRunRepository implements AgentRunRepository {
    private final AgentRunMapper mapper;
    private final JsonCodec jsonCodec;

    public MyBatisAgentRunRepository(AgentRunMapper mapper, JsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void save(AgentRunRecord record) {
        mapper.upsertAgentRun(toPo(record));
    }

    @Override
    public Optional<AgentRunRecord> find(String runId) {
        return Optional.ofNullable(mapper.findAgentRun(runId)).map(this::toDomain);
    }

    @Override
    public List<AgentRunRecord> findAll() {
        return mapper.findAllAgentRuns().stream().map(this::toDomain).toList();
    }

    private AgentRunPo toPo(AgentRunRecord record) {
        AgentRunPo po = new AgentRunPo();
        po.setRunId(record.runId());
        po.setTaskId(record.taskId());
        po.setObjective(record.objective());
        po.setStatus(record.status().name());
        po.setStepsJson(jsonCodec.toJson(record.steps()));
        po.setToolCallsJson(jsonCodec.toJson(record.toolCalls()));
        po.setFinalAnswer(record.finalAnswer());
        po.setWorkflowMode(record.workflowMode().name());
        po.setFailedRole(record.failedRole() == null ? null : record.failedRole().name());
        po.setErrorSummary(record.errorSummary());
        po.setStartedAt(toLocal(record.startedAt()));
        po.setFinishedAt(toLocal(record.finishedAt()));
        po.setMetricsJson(jsonCodec.toJson(record.metrics()));
        po.setCreatedAt(LocalDateTime.ofInstant(record.createdAt(), ZoneOffset.UTC));
        return po;
    }

    private AgentRunRecord toDomain(AgentRunPo po) {
        List<AgentStepReport> steps = jsonCodec.readList(po.getStepsJson(), new TypeReference<>() {});
        List<AgentToolCall> toolCalls = jsonCodec.readList(po.getToolCallsJson(), new TypeReference<>() {});
        Instant createdAt = po.getCreatedAt() == null ? Instant.now() : po.getCreatedAt().toInstant(ZoneOffset.UTC);
        AgentWorkflowMode mode = po.getWorkflowMode() == null ? AgentWorkflowMode.RAG_MULTI_AGENT
                : AgentWorkflowMode.valueOf(po.getWorkflowMode());
        AgentRole failedRole = po.getFailedRole() == null ? null : AgentRole.valueOf(po.getFailedRole());
        AgentRunMetrics metrics = jsonCodec.read(po.getMetricsJson(), AgentRunMetrics.class);
        return new AgentRunRecord(po.getRunId(), po.getTaskId(), po.getObjective(), AgentRunStatus.valueOf(po.getStatus()),
                steps, toolCalls, po.getFinalAnswer(), createdAt, mode, failedRole, po.getErrorSummary(),
                toInstant(po.getStartedAt(), createdAt), toInstant(po.getFinishedAt(), null),
                metrics == null ? AgentRunMetrics.empty() : metrics);
    }

    private LocalDateTime toLocal(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value, Instant fallback) {
        return value == null ? fallback : value.toInstant(ZoneOffset.UTC);
    }
}
