package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.agent.AgentRunRecord;
import com.hlju.learning.repository.AgentRunRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryAgentRunRepository implements AgentRunRepository {
    private final Map<String, AgentRunRecord> runs = new ConcurrentHashMap<>();

    @Override
    public void save(AgentRunRecord record) {
        runs.put(record.runId(), record);
    }

    @Override
    public Optional<AgentRunRecord> find(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    @Override
    public List<AgentRunRecord> findAll() {
        return runs.values().stream()
                .sorted(Comparator.comparing(AgentRunRecord::createdAt).reversed())
                .toList();
    }
}
