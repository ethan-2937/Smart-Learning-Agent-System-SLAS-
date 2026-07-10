package com.hlju.learning.controller;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.service.MaterialService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/api/materials/upload")
    public MaterialRecord upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "title", required = false) String title,
                                 @RequestParam(value = "subjectPreset", required = false) SubjectPreset subjectPreset) {
        return materialService.uploadMaterial(file, title, subjectPreset);
    }

    @PostMapping("/api/materials/{materialId}/parse")
    public MaterialRecord parse(@PathVariable String materialId) {
        return materialService.parseAndIndex(materialId);
    }

    @GetMapping("/api/materials")
    public List<MaterialRecord> list() {
        return materialService.listMaterials();
    }

    @GetMapping("/api/materials/{materialId}")
    public MaterialRecord get(@PathVariable String materialId) {
        return materialService.getMaterial(materialId);
    }

    @GetMapping("/api/materials/{materialId}/chunks")
    public List<MaterialChunk> chunks(@PathVariable String materialId) {
        return materialService.listChunks(materialId);
    }
}
