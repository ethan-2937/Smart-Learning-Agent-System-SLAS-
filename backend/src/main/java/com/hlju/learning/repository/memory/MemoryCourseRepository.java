package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;
import com.hlju.learning.repository.CourseRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryCourseRepository implements CourseRepository {
    private final Map<String, CourseRecord> courses = new ConcurrentHashMap<>();
    private final Map<String, CourseChapterRecord> chapters = new ConcurrentHashMap<>();
    private final Map<String, MaterialCourseBindingRecord> bindings = new ConcurrentHashMap<>();
    private final Map<String, KnowledgePointRecord> points = new ConcurrentHashMap<>();

    @Override
    public void saveCourse(CourseRecord course) {
        courses.put(course.courseId(), course);
    }

    @Override
    public Optional<CourseRecord> findCourse(String courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }

    @Override
    public List<CourseRecord> findAllCourses() {
        return courses.values().stream()
                .sorted(Comparator.comparing(CourseRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public void saveChapter(CourseChapterRecord chapter) {
        chapters.put(chapter.chapterId(), chapter);
    }

    @Override
    public Optional<CourseChapterRecord> findChapter(String chapterId) {
        return Optional.ofNullable(chapters.get(chapterId));
    }

    @Override
    public List<CourseChapterRecord> findChaptersByCourse(String courseId) {
        return chapters.values().stream()
                .filter(chapter -> courseId.equals(chapter.courseId()))
                .sorted(Comparator.comparingInt(CourseChapterRecord::chapterOrder).thenComparing(CourseChapterRecord::createdAt))
                .toList();
    }

    @Override
    public void saveMaterialBinding(MaterialCourseBindingRecord binding) {
        bindings.put(binding.materialId(), binding);
    }

    @Override
    public Optional<MaterialCourseBindingRecord> findMaterialBinding(String materialId) {
        return Optional.ofNullable(bindings.get(materialId));
    }

    @Override
    public void replaceKnowledgePointsByMaterial(String materialId, List<KnowledgePointRecord> newPoints) {
        points.entrySet().removeIf(entry -> materialId.equals(entry.getValue().materialId()));
        newPoints.forEach(point -> points.put(point.knowledgePointId(), point));
    }

    @Override
    public List<KnowledgePointRecord> findKnowledgePoints(String courseId, String chapterId, String materialId) {
        return points.values().stream()
                .filter(point -> courseId == null || courseId.isBlank() || courseId.equals(point.courseId()))
                .filter(point -> chapterId == null || chapterId.isBlank() || chapterId.equals(point.chapterId()))
                .filter(point -> materialId == null || materialId.isBlank() || materialId.equals(point.materialId()))
                .sorted(Comparator.comparingDouble(KnowledgePointRecord::weight).reversed().thenComparing(KnowledgePointRecord::createdAt))
                .toList();
    }
}
