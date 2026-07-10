package com.hlju.learning.repository;

import com.hlju.learning.domain.agent.AgentRunRecord;

import java.util.List;
import java.util.Optional;

public interface AgentRunRepository {
    void save(AgentRunRecord record);

    Optional<AgentRunRecord> find(String runId);

    List<AgentRunRecord> findAll();
}
