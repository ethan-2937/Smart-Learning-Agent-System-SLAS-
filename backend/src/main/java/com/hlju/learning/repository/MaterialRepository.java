package com.hlju.learning.repository;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository {
    void saveMaterial(MaterialRecord record);

    Optional<MaterialRecord> findMaterial(String materialId);

    List<MaterialRecord> findAllMaterials();

    void replaceChunks(String materialId, List<MaterialChunk> chunks);

    List<MaterialChunk> findChunks(String materialId);
}
