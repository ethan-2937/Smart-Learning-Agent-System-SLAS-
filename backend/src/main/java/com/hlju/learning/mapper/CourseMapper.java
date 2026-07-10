package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.CourseChapterPo;
import com.hlju.learning.domain.po.CoursePo;
import com.hlju.learning.domain.po.KnowledgePointPo;
import com.hlju.learning.domain.po.MaterialCourseBindingPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper {
    void upsertCourse(@Param("po") CoursePo po);

    CoursePo findCourse(@Param("courseId") String courseId);

    List<CoursePo> findAllCourses();

    void upsertChapter(@Param("po") CourseChapterPo po);

    CourseChapterPo findChapter(@Param("chapterId") String chapterId);

    List<CourseChapterPo> findChaptersByCourse(@Param("courseId") String courseId);

    void upsertMaterialBinding(@Param("po") MaterialCourseBindingPo po);

    MaterialCourseBindingPo findMaterialBinding(@Param("materialId") String materialId);

    void deleteKnowledgePointsByMaterial(@Param("materialId") String materialId);

    void insertKnowledgePoint(@Param("po") KnowledgePointPo po);

    List<KnowledgePointPo> findKnowledgePoints(@Param("courseId") String courseId,
                                               @Param("chapterId") String chapterId,
                                               @Param("materialId") String materialId);
}
