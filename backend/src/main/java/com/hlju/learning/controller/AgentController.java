package com.hlju.learning.controller;

import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.domain.agent.AgentWorkflowTemplate;
import com.hlju.learning.service.AgentRunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AgentController {
    private final AgentRunService agentRunService;

    public AgentController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @GetMapping("/api/agents/workflow-template")
    public AgentWorkflowTemplate workflowTemplate() {
        return agentRunService.getWorkflowTemplate();
    }

    @GetMapping("/api/agents/runs")
    public List<AgentRunRecord> listRuns() {
        return agentRunService.listRuns();
    }

    @GetMapping("/api/agents/runs/{runId}")
    public AgentRunRecord getRun(@PathVariable String runId) {
        return agentRunService.getRun(runId);
    }
}
