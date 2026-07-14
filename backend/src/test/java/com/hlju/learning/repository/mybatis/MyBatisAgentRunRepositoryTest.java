package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentRunMetrics;
import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentStepReport;
import com.hlju.learning.domain.agent.AgentToolCall;
import com.hlju.learning.domain.agent.AgentWorkflowMode;
import com.hlju.learning.domain.po.AgentRunPo;
import com.hlju.learning.mapper.AgentRunMapper;
import com.hlju.learning.util.JsonCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisAgentRunRepositoryTest {
    @Test
    void mapsExpandedExecutionTraceInBothDirections() {
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        MyBatisAgentRunRepository repository = new MyBatisAgentRunRepository(mapper,
                new JsonCodec(new ObjectMapper().findAndRegisterModules()));
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant finish = start.plusSeconds(2);
        AgentStepReport step = new AgentStepReport("step-1", AgentRole.QUALITY_REVIEW, "review", "failed",
                AgentRunStatus.FAILED, "{\"candidateCount\":1}", null, start, finish, 1, "invalid answer");
        AgentToolCall call = new AgentToolCall("reviewQuestionCandidates", "{}", null, false, "call-1",
                AgentRole.QUALITY_REVIEW, start, finish, "invalid answer");
        AgentRunMetrics metrics = new AgentRunMetrics(1, 1, 1, 0, 0, 0, 1, 0, 2000, 0);
        AgentRunRecord record = new AgentRunRecord("run-1", "task-1", "objective", AgentRunStatus.FAILED,
                List.of(step), List.of(call), "failed", start, AgentWorkflowMode.RAG_MULTI_AGENT,
                AgentRole.QUALITY_REVIEW, "invalid answer", start, finish, metrics);

        repository.save(record);

        ArgumentCaptor<AgentRunPo> captor = ArgumentCaptor.forClass(AgentRunPo.class);
        verify(mapper).upsertAgentRun(captor.capture());
        AgentRunPo po = captor.getValue();
        assertThat(po.getWorkflowMode()).isEqualTo("RAG_MULTI_AGENT");
        assertThat(po.getFailedRole()).isEqualTo("QUALITY_REVIEW");
        assertThat(po.getStepsJson()).contains("inputJson");
        assertThat(po.getMetricsJson()).contains("groundedQuestionCount");

        when(mapper.findAgentRun("run-1")).thenReturn(po);
        AgentRunRecord restored = repository.find("run-1").orElseThrow();
        assertThat(restored.failedRole()).isEqualTo(AgentRole.QUALITY_REVIEW);
        assertThat(restored.steps()).containsExactly(step);
        assertThat(restored.toolCalls()).containsExactly(call);
        assertThat(restored.metrics()).isEqualTo(metrics);
    }
}
