package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.MaterialUnderstandingInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.MaterialUnderstandingOutput;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.service.MaterialService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class MaterialUnderstandingAgent implements AgentRoleExecutor<MaterialUnderstandingInput, MaterialUnderstandingOutput> {
    private final MaterialService materialService;

    public MaterialUnderstandingAgent(MaterialService materialService) {
        this.materialService = materialService;
    }

    @Override
    public AgentRole role() {
        return AgentRole.MATERIAL_UNDERSTANDING;
    }

    @Override
    public MaterialUnderstandingOutput execute(MaterialUnderstandingInput input, AgentExecutionContext context) {
        List<MaterialChunk> chunks = context.call("listMaterialChunks",
                input.request().material().materialId(),
                () -> materialService.listChunks(input.request().material().materialId()),
                values -> java.util.Map.of("chunkCount", values.size()));
        return context.call("analyzeMaterialEvidence",
                java.util.Map.of("topic", safeTopic(input), "chunkCount", chunks.size()),
                () -> analyze(input, chunks),
                output -> java.util.Map.of(
                        "theme", output.theme(),
                        "knowledgePoints", output.knowledgePoints(),
                        "candidateChunkIds", output.candidateChunks().stream().map(MaterialChunk::chunkId).toList()));
    }

    private MaterialUnderstandingOutput analyze(MaterialUnderstandingInput input, List<MaterialChunk> chunks) {
        LinkedHashSet<String> knowledgePoints = new LinkedHashSet<>();
        chunks.forEach(chunk -> knowledgePoints.addAll(chunk.keywords()));
        String theme = safeTopic(input);
        if (knowledgePoints.isEmpty()) {
            knowledgePoints.add(theme);
        }
        return new MaterialUnderstandingOutput(theme, new ArrayList<>(knowledgePoints).stream().limit(12).toList(), chunks);
    }

    private String safeTopic(MaterialUnderstandingInput input) {
        String topic = input.request().topic();
        return topic == null || topic.isBlank() ? input.request().material().title() : topic.trim();
    }
}
