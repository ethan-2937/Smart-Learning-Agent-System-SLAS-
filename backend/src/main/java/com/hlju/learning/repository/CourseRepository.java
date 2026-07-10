package com.hlju.learning.repository;

import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    void saveCourse(CourseRecord course);

    Optional<CourseRecord> findCourse(String courseId);

    List<CourseRecord> findAllCourses();

    void saveChapter(CourseChapterRecord chapter);

    Optional<CourseChapterRecord> findChapter(String chapterId);

    List<CourseChapterRecord> findChaptersByCourse(String courseId);

    void saveMaterialBinding(MaterialCourseBindingRecord binding);

    Optional<MaterialCourseBindingRecord> findMaterialBinding(String materialId);

    void replaceKnowledgePointsByMaterial(String materialId, List<KnowledgePointRecord> points);

    List<KnowledgePointRecord> findKnowledgePoints(String courseId, String chapterId, String materialId);
}
