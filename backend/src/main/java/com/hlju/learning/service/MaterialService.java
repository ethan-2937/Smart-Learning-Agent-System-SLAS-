package com.hlju.learning.service;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialService {
    MaterialRecord uploadMaterial(MultipartFile file, String title, SubjectPreset subjectPreset);

    MaterialRecord parseAndIndex(String materialId);

    List<MaterialRecord> listMaterials();

    MaterialRecord getMaterial(String materialId);

    List<MaterialChunk> listChunks(String materialId);
}
