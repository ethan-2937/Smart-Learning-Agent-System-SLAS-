package com.hlju.learning.controller;

import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.course.BindMaterialCourseRequest;
import com.hlju.learning.service.CourseService;
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
    private final CourseService courseService;

    public MaterialController(MaterialService materialService, CourseService courseService) {
        this.materialService = materialService;
        this.courseService = courseService;
    }

    @PostMapping("/api/materials/upload")
    public MaterialRecord upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "title", required = false) String title,
                                 @RequestParam(value = "subjectPreset", required = false) SubjectPreset subjectPreset,
                                 @RequestParam(value = "courseId", required = false) String courseId,
                                 @RequestParam(value = "chapterId", required = false) String chapterId) {
        MaterialRecord record = materialService.uploadMaterial(file, title, subjectPreset);
        if (courseId != null && !courseId.isBlank()) {
            courseService.bindMaterial(record.materialId(), new BindMaterialCourseRequest(courseId, chapterId));
        }
        return record;
    }

    @PostMapping("/api/materials/{materialId}/parse")
    public MaterialRecord parse(@PathVariable String materialId) {
        MaterialRecord record = materialService.parseAndIndex(materialId);
        if ("INDEXED".equals(record.status().name())) {
            courseService.refreshKnowledgePoints(materialId);
        }
        return record;
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
