package com.hlju.learning.repository.mybatis;

import com.hlju.learning.domain.course.CourseChapterRecord;
import com.hlju.learning.domain.course.CourseRecord;
import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.po.CourseChapterPo;
import com.hlju.learning.domain.po.CoursePo;
import com.hlju.learning.domain.po.KnowledgePointPo;
import com.hlju.learning.domain.po.MaterialCourseBindingPo;
import com.hlju.learning.mapper.CourseMapper;
import com.hlju.learning.repository.CourseRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "mybatis")
public class MyBatisCourseRepository implements CourseRepository {
    private final CourseMapper mapper;

    public MyBatisCourseRepository(CourseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveCourse(CourseRecord course) {
        mapper.upsertCourse(toPo(course));
    }

    @Override
    public Optional<CourseRecord> findCourse(String courseId) {
        return Optional.ofNullable(mapper.findCourse(courseId)).map(this::toDomain);
    }

    @Override
    public List<CourseRecord> findAllCourses() {
        return mapper.findAllCourses().stream().map(this::toDomain).toList();
    }

    @Override
    public void saveChapter(CourseChapterRecord chapter) {
        mapper.upsertChapter(toPo(chapter));
    }

    @Override
    public Optional<CourseChapterRecord> findChapter(String chapterId) {
        return Optional.ofNullable(mapper.findChapter(chapterId)).map(this::toDomain);
    }

    @Override
    public List<CourseChapterRecord> findChaptersByCourse(String courseId) {
        return mapper.findChaptersByCourse(courseId).stream().map(this::toDomain).toList();
    }

    @Override
    public void saveMaterialBinding(MaterialCourseBindingRecord binding) {
        mapper.upsertMaterialBinding(toPo(binding));
    }

    @Override
    public Optional<MaterialCourseBindingRecord> findMaterialBinding(String materialId) {
        return Optional.ofNullable(mapper.findMaterialBinding(materialId)).map(this::toDomain);
    }

    @Override
    @Transactional
    public void replaceKnowledgePointsByMaterial(String materialId, List<KnowledgePointRecord> points) {
        mapper.deleteKnowledgePointsByMaterial(materialId);
        points.stream().map(this::toPo).forEach(mapper::insertKnowledgePoint);
    }

    @Override
    public List<KnowledgePointRecord> findKnowledgePoints(String courseId, String chapterId, String materialId) {
        return mapper.findKnowledgePoints(blankToNull(courseId), blankToNull(chapterId), blankToNull(materialId))
                .stream().map(this::toDomain).toList();
    }

    private CoursePo toPo(CourseRecord record) {
        CoursePo po = new CoursePo();
        po.setCourseId(record.courseId());
        po.setName(record.name());
        po.setSubjectPreset(record.subjectPreset().name());
        po.setDescription(record.description());
        po.setCreatedAt(toLocal(record.createdAt()));
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private CourseRecord toDomain(CoursePo po) {
        return new CourseRecord(po.getCourseId(), po.getName(), SubjectPreset.valueOf(po.getSubjectPreset()),
                po.getDescription(), toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private CourseChapterPo toPo(CourseChapterRecord record) {
        CourseChapterPo po = new CourseChapterPo();
        po.setChapterId(record.chapterId());
        po.setCourseId(record.courseId());
        po.setTitle(record.title());
        po.setChapterOrder(record.chapterOrder());
        po.setDescription(record.description());
        po.setCreatedAt(toLocal(record.createdAt()));
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private CourseChapterRecord toDomain(CourseChapterPo po) {
        return new CourseChapterRecord(po.getChapterId(), po.getCourseId(), po.getTitle(),
                po.getChapterOrder() == null ? 0 : po.getChapterOrder(), po.getDescription(),
                toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private MaterialCourseBindingPo toPo(MaterialCourseBindingRecord record) {
        MaterialCourseBindingPo po = new MaterialCourseBindingPo();
        po.setMaterialId(record.materialId());
        po.setCourseId(record.courseId());
        po.setChapterId(record.chapterId());
        po.setCreatedAt(toLocal(record.createdAt()));
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private MaterialCourseBindingRecord toDomain(MaterialCourseBindingPo po) {
        return new MaterialCourseBindingRecord(po.getMaterialId(), po.getCourseId(), po.getChapterId(),
                toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private KnowledgePointPo toPo(KnowledgePointRecord record) {
        KnowledgePointPo po = new KnowledgePointPo();
        po.setKnowledgePointId(record.knowledgePointId());
        po.setCourseId(record.courseId());
        po.setChapterId(record.chapterId());
        po.setMaterialId(record.materialId());
        po.setChunkId(record.chunkId());
        po.setName(record.name());
        po.setDescription(record.description());
        po.setSourceSnippet(record.sourceSnippet());
        po.setWeight(record.weight());
        po.setCreatedAt(toLocal(record.createdAt()));
        return po;
    }

    private KnowledgePointRecord toDomain(KnowledgePointPo po) {
        return new KnowledgePointRecord(po.getKnowledgePointId(), po.getCourseId(), po.getChapterId(), po.getMaterialId(),
                po.getChunkId(), po.getName(), po.getDescription(), po.getSourceSnippet(),
                po.getWeight() == null ? 0.0 : po.getWeight(), toInstant(po.getCreatedAt()));
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.now() : value.toInstant(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
