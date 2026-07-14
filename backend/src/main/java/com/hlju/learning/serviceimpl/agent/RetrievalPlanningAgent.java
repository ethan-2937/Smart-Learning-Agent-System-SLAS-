package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.RetrievalPlanningOutput;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.hlju.learning.domain.rag.RetrievalResult;
import com.hlju.learning.service.VectorRetrievalService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RetrievalPlanningAgent implements AgentRoleExecutor<RetrievalPlanningInput, RetrievalPlanningOutput> {
    private final VectorRetrievalService retrievalService;

    public RetrievalPlanningAgent(VectorRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public AgentRole role() {
        return AgentRole.RETRIEVAL_PLANNING;
    }

    @Override
    public RetrievalPlanningOutput execute(RetrievalPlanningInput input, AgentExecutionContext context) {
        List<String> queries = context.call("planRetrievalQueries",
                Map.of("theme", input.understanding().theme(), "knowledgePoints", input.understanding().knowledgePoints()),
                () -> planQueries(input));
        Map<String, RetrievalHit> hitsByChunk = new LinkedHashMap<>();
        int topK = Math.max(6, input.request().count());
        for (String query : queries) {
            RetrievalResult result = context.call("retrieveChunks",
                    Map.of("query", query, "materialId", input.request().material().materialId(), "topK", topK),
                    () -> retrievalService.retrieve(query, input.request().material().materialId(), topK),
                    value -> Map.of("query", value.query(), "hitCount", value.hits().size()));
            result.hits().forEach(hit -> hitsByChunk.merge(hit.chunk().chunkId(), hit,
                    (left, right) -> left.score() >= right.score() ? left : right));
        }
        List<RetrievalHit> evidence = hitsByChunk.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalHit::score).reversed())
                .limit(topK)
                .toList();
        boolean fallbackUsed = evidence.isEmpty();
        if (fallbackUsed) {
            evidence = context.call("selectMaterialFallbackEvidence",
                    Map.of("candidateChunkCount", input.understanding().candidateChunks().size(), "limit", topK),
                    () -> fallbackEvidence(input.understanding().candidateChunks(), topK),
                    values -> Map.of("hitCount", values.size()));
        }
        return new RetrievalPlanningOutput(queries, evidence, fallbackUsed);
    }

    private List<String> planQueries(RetrievalPlanningInput input) {
        List<String> queries = new ArrayList<>();
        queries.add(input.request().subjectPreset() + " " + input.understanding().theme());
        input.understanding().knowledgePoints().stream().limit(2)
                .map(point -> input.understanding().theme() + " " + point)
                .filter(query -> !queries.contains(query))
                .forEach(queries::add);
        return List.copyOf(queries);
    }

    private List<RetrievalHit> fallbackEvidence(List<MaterialChunk> chunks, int topK) {
        return chunks.stream().limit(topK)
                .map(chunk -> new RetrievalHit(chunk, 0.2, "fallback material evidence"))
                .toList();
    }
}
