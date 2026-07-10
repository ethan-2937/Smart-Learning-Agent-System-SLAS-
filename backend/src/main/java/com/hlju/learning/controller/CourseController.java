package com.hlju.learning.controller;

import com.hlju.learning.domain.course.BindMaterialCourseRequest;
import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.CreateChapterRequest;
import com.hlju.learning.domain.course.CreateCourseRequest;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;
import com.hlju.learning.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/api/courses")
    public CourseRecord createCourse(@RequestBody @Valid CreateCourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping("/api/courses")
    public List<CourseRecord> listCourses() {
        return courseService.listCourses();
    }

    @GetMapping("/api/courses/{courseId}")
    public CourseRecord getCourse(@PathVariable String courseId) {
        return courseService.getCourse(courseId);
    }

    @PostMapping("/api/courses/{courseId}/chapters")
    public CourseChapterRecord createChapter(@PathVariable String courseId, @RequestBody @Valid CreateChapterRequest request) {
        return courseService.createChapter(courseId, request);
    }

    @GetMapping("/api/courses/{courseId}/chapters")
    public List<CourseChapterRecord> listChapters(@PathVariable String courseId) {
        return courseService.listChapters(courseId);
    }

    @PostMapping("/api/materials/{materialId}/course-binding")
    public MaterialCourseBindingRecord bindMaterial(@PathVariable String materialId,
                                                    @RequestBody @Valid BindMaterialCourseRequest request) {
        return courseService.bindMaterial(materialId, request);
    }

    @GetMapping("/api/materials/{materialId}/course-binding")
    public MaterialCourseBindingRecord getBinding(@PathVariable String materialId) {
        return courseService.getMaterialBinding(materialId);
    }

    @PostMapping("/api/materials/{materialId}/knowledge-points/refresh")
    public List<KnowledgePointRecord> refreshKnowledgePoints(@PathVariable String materialId) {
        return courseService.refreshKnowledgePoints(materialId);
    }

    @GetMapping("/api/knowledge-points")
    public List<KnowledgePointRecord> listKnowledgePoints(@RequestParam(value = "courseId", required = false) String courseId,
                                                          @RequestParam(value = "chapterId", required = false) String chapterId,
                                                          @RequestParam(value = "materialId", required = false) String materialId) {
        return courseService.listKnowledgePoints(courseId, chapterId, materialId);
    }
}
