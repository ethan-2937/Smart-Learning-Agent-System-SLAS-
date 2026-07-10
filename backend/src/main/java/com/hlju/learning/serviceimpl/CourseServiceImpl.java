package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.course.BindMaterialCourseRequest;
import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.CreateChapterRequest;
import com.hlju.learning.domain.course.CreateCourseRequest;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.repository.CourseRepository;
import com.hlju.learning.repository.MaterialRepository;
import com.hlju.learning.service.CourseService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final MaterialRepository materialRepository;

    public CourseServiceImpl(CourseRepository courseRepository, MaterialRepository materialRepository) {
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
    }

    @Override
    public CourseRecord createCourse(CreateCourseRequest request) {
        Instant now = Instant.now();
        CourseRecord course = new CourseRecord(UUID.randomUUID().toString(), request.name().trim(),
                request.subjectPreset() == null ? SubjectPreset.GENERAL : request.subjectPreset(),
                request.description(), now, now);
        courseRepository.saveCourse(course);
        return course;
    }

    @Override
    public List<CourseRecord> listCourses() {
        return courseRepository.findAllCourses();
    }

    @Override
    public CourseRecord getCourse(String courseId) {
        return courseRepository.findCourse(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    @Override
    public CourseChapterRecord createChapter(String courseId, CreateChapterRequest request) {
        getCourse(courseId);
        Instant now = Instant.now();
        int order = request.chapterOrder() == null ? courseRepository.findChaptersByCourse(courseId).size() + 1 : request.chapterOrder();
        CourseChapterRecord chapter = new CourseChapterRecord(UUID.randomUUID().toString(), courseId,
                request.title().trim(), order, request.description(), now, now);
        courseRepository.saveChapter(chapter);
        return chapter;
    }

    @Override
    public List<CourseChapterRecord> listChapters(String courseId) {
        getCourse(courseId);
        return courseRepository.findChaptersByCourse(courseId);
    }

    @Override
    public MaterialCourseBindingRecord bindMaterial(String materialId, BindMaterialCourseRequest request) {
        materialRepository.findMaterial(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        getCourse(request.courseId());
        if (request.chapterId() != null && !request.chapterId().isBlank()) {
            CourseChapterRecord chapter = courseRepository.findChapter(request.chapterId())
                    .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + request.chapterId()));
            if (!request.courseId().equals(chapter.courseId())) {
                throw new IllegalArgumentException("Chapter does not belong to selected course");
            }
        }
        Instant now = Instant.now();
        MaterialCourseBindingRecord current = courseRepository.findMaterialBinding(materialId).orElse(null);
        MaterialCourseBindingRecord binding = new MaterialCourseBindingRecord(materialId, request.courseId(),
                blankToNull(request.chapterId()), current == null ? now : current.createdAt(), now);
        courseRepository.saveMaterialBinding(binding);
        return binding;
    }

    @Override
    public MaterialCourseBindingRecord getMaterialBinding(String materialId) {
        return courseRepository.findMaterialBinding(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material binding not found: " + materialId));
    }

    @Override
    public List<KnowledgePointRecord> refreshKnowledgePoints(String materialId) {
        var material = materialRepository.findMaterial(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        MaterialCourseBindingRecord binding = courseRepository.findMaterialBinding(materialId)
                .orElse(new MaterialCourseBindingRecord(materialId, null, null, Instant.now(), Instant.now()));
        List<MaterialChunk> chunks = materialRepository.findChunks(materialId);
        Map<String, KnowledgePointRecord> dedup = new LinkedHashMap<>();
        int rank = 0;
        for (MaterialChunk chunk : chunks) {
            List<String> keywords = chunk.keywords() == null ? List.of() : chunk.keywords();
            for (String keyword : keywords) {
                String name = normalizeKeyword(keyword);
                if (name.isBlank() || dedup.containsKey(name)) {
                    continue;
                }
                double weight = Math.max(0.2, 1.0 - rank * 0.03);
                String chapterId = binding.chapterId() == null ? chunk.chapterId() : binding.chapterId();
                dedup.put(name, new KnowledgePointRecord(UUID.randomUUID().toString(), binding.courseId(), chapterId,
                        materialId, chunk.chunkId(), name,
                        "从教材《" + material.title() + "》中自动抽取的候选知识点，需教师复核。",
                        shorten(chunk.text(), 180), weight, Instant.now()));
                rank++;
                if (dedup.size() >= 40) {
                    break;
                }
            }
            if (dedup.size() >= 40) {
                break;
            }
        }
        List<KnowledgePointRecord> points = new ArrayList<>(dedup.values());
        courseRepository.replaceKnowledgePointsByMaterial(materialId, points);
        return points;
    }

    @Override
    public List<KnowledgePointRecord> listKnowledgePoints(String courseId, String chapterId, String materialId) {
        return courseRepository.findKnowledgePoints(courseId, chapterId, materialId);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2) {
            return "";
        }
        return value;
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
