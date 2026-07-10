package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.repository.MaterialRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryMaterialRepository implements MaterialRepository {
    private final Map<String, MaterialRecord> materials = new ConcurrentHashMap<>();
    private final Map<String, List<MaterialChunk>> chunksByMaterial = new ConcurrentHashMap<>();

    @Override
    public void saveMaterial(MaterialRecord record) {
        materials.put(record.materialId(), record);
    }

    @Override
    public Optional<MaterialRecord> findMaterial(String materialId) {
        return Optional.ofNullable(materials.get(materialId));
    }

    @Override
    public List<MaterialRecord> findAllMaterials() {
        return materials.values().stream()
                .sorted(Comparator.comparing(MaterialRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public void replaceChunks(String materialId, List<MaterialChunk> chunks) {
        chunksByMaterial.put(materialId, List.copyOf(chunks));
    }

    @Override
    public List<MaterialChunk> findChunks(String materialId) {
        return chunksByMaterial.getOrDefault(materialId, List.of());
    }
}
