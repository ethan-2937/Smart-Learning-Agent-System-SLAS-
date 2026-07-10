package com.hlju.learning.service;

import com.hlju.learning.domain.course.BindMaterialCourseRequest;
import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.CreateChapterRequest;
import com.hlju.learning.domain.course.CreateCourseRequest;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;

import java.util.List;

public interface CourseService {
    CourseRecord createCourse(CreateCourseRequest request);

    List<CourseRecord> listCourses();

    CourseRecord getCourse(String courseId);

    CourseChapterRecord createChapter(String courseId, CreateChapterRequest request);

    List<CourseChapterRecord> listChapters(String courseId);

    MaterialCourseBindingRecord bindMaterial(String materialId, BindMaterialCourseRequest request);

    MaterialCourseBindingRecord getMaterialBinding(String materialId);

    List<KnowledgePointRecord> refreshKnowledgePoints(String materialId);

    List<KnowledgePointRecord> listKnowledgePoints(String courseId, String chapterId, String materialId);
}
