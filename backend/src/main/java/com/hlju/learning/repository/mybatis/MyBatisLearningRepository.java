package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.PracticeSetStatus;
import com.hlju.learning.domain.learning.WrongQuestionRecord;
import com.hlju.learning.domain.po.KnowledgeMasteryPo;
import com.hlju.learning.domain.po.PracticeAttemptPo;
import com.hlju.learning.domain.po.PracticeSetPo;
import com.hlju.learning.domain.po.WrongQuestionPo;
import com.hlju.learning.mapper.LearningMapper;
import com.hlju.learning.repository.LearningRepository;
import com.hlju.learning.util.JsonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "mybatis")
public class MyBatisLearningRepository implements LearningRepository {
    private final LearningMapper mapper;
    private final JsonCodec jsonCodec;

    public MyBatisLearningRepository(LearningMapper mapper, JsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void savePracticeSet(PracticeSetRecord practiceSet) {
        mapper.upsertPracticeSet(toPo(practiceSet));
    }

    @Override
    public Optional<PracticeSetRecord> findPracticeSet(String practiceId) {
        return Optional.ofNullable(mapper.findPracticeSet(practiceId)).map(this::toDomain);
    }

    @Override
    public List<PracticeSetRecord> findPracticeSets(String studentId) {
        return mapper.findPracticeSets(blankToNull(studentId)).stream().map(this::toDomain).toList();
    }

    @Override
    public void saveAttempt(PracticeAttemptRecord attempt) {
        mapper.insertAttempt(toPo(attempt));
    }

    @Override
    public List<PracticeAttemptRecord> findAttempts(String practiceId, String studentId) {
        return mapper.findAttempts(blankToNull(practiceId), blankToNull(studentId)).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WrongQuestionRecord> findWrongQuestion(String studentId, String questionId) {
        return Optional.ofNullable(mapper.findWrongQuestion(studentId, questionId)).map(this::toDomain);
    }

    @Override
    public void saveWrongQuestion(WrongQuestionRecord wrongQuestion) {
        mapper.upsertWrongQuestion(toPo(wrongQuestion));
    }

    @Override
    public List<WrongQuestionRecord> findWrongQuestions(String studentId) {
        return mapper.findWrongQuestions(blankToNull(studentId)).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<KnowledgeMasteryRecord> findMastery(String studentId, String materialId, String knowledgeName) {
        return Optional.ofNullable(mapper.findMastery(studentId, materialId, knowledgeName)).map(this::toDomain);
    }

    @Override
    public void saveMastery(KnowledgeMasteryRecord mastery) {
        mapper.upsertMastery(toPo(mastery));
    }

    @Override
    public List<KnowledgeMasteryRecord> findMasteryByStudent(String studentId) {
        return mapper.findMasteryByStudent(blankToNull(studentId)).stream().map(this::toDomain).toList();
    }

    private PracticeSetPo toPo(PracticeSetRecord record) {
        PracticeSetPo po = new PracticeSetPo();
        po.setPracticeId(record.practiceId());
        po.setTitle(record.title());
        po.setStudentId(record.studentId());
        po.setCourseId(record.courseId());
        po.setChapterId(record.chapterId());
        po.setMaterialId(record.materialId());
        po.setQuestionIdsJson(jsonCodec.toJson(record.questionIds()));
        po.setStatus(record.status().name());
        po.setCreatedAt(toLocal(record.createdAt()));
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private PracticeSetRecord toDomain(PracticeSetPo po) {
        List<String> questionIds = jsonCodec.readList(po.getQuestionIdsJson(), new TypeReference<>() {});
        return new PracticeSetRecord(po.getPracticeId(), po.getTitle(), po.getStudentId(), po.getCourseId(),
                po.getChapterId(), po.getMaterialId(), questionIds, PracticeSetStatus.valueOf(po.getStatus()),
                toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private PracticeAttemptPo toPo(PracticeAttemptRecord record) {
        PracticeAttemptPo po = new PracticeAttemptPo();
        po.setAttemptId(record.attemptId());
        po.setPracticeId(record.practiceId());
        po.setStudentId(record.studentId());
        po.setQuestionId(record.questionId());
        po.setAnswerText(record.answerText());
        po.setCorrect(record.correct());
        po.setScore(record.score());
        po.setExpectedAnswer(record.expectedAnswer());
        po.setFeedback(record.feedback());
        po.setKnowledgeNamesJson(jsonCodec.toJson(record.knowledgeNames()));
        po.setSubmittedAt(toLocal(record.submittedAt()));
        return po;
    }

    private PracticeAttemptRecord toDomain(PracticeAttemptPo po) {
        List<String> names = jsonCodec.readList(po.getKnowledgeNamesJson(), new TypeReference<>() {});
        return new PracticeAttemptRecord(po.getAttemptId(), po.getPracticeId(), po.getStudentId(), po.getQuestionId(),
                po.getAnswerText(), Boolean.TRUE.equals(po.getCorrect()), po.getScore() == null ? 0.0 : po.getScore(),
                po.getExpectedAnswer(), po.getFeedback(), names, toInstant(po.getSubmittedAt()));
    }

    private WrongQuestionPo toPo(WrongQuestionRecord record) {
        WrongQuestionPo po = new WrongQuestionPo();
        po.setStudentId(record.studentId());
        po.setQuestionId(record.questionId());
        po.setMaterialId(record.materialId());
        po.setPrompt(record.prompt());
        po.setExpectedAnswer(record.expectedAnswer());
        po.setLastAnswer(record.lastAnswer());
        po.setLastFeedback(record.lastFeedback());
        po.setWrongCount(record.wrongCount());
        po.setLastSubmittedAt(toLocal(record.lastSubmittedAt()));
        return po;
    }

    private WrongQuestionRecord toDomain(WrongQuestionPo po) {
        return new WrongQuestionRecord(po.getStudentId(), po.getQuestionId(), po.getMaterialId(), po.getPrompt(),
                po.getExpectedAnswer(), po.getLastAnswer(), po.getLastFeedback(), po.getWrongCount() == null ? 0 : po.getWrongCount(),
                toInstant(po.getLastSubmittedAt()));
    }

    private KnowledgeMasteryPo toPo(KnowledgeMasteryRecord record) {
        KnowledgeMasteryPo po = new KnowledgeMasteryPo();
        po.setStudentId(record.studentId());
        po.setCourseId(record.courseId());
        po.setChapterId(record.chapterId());
        po.setMaterialId(record.materialId());
        po.setKnowledgeName(record.knowledgeName());
        po.setTotalAttempts(record.totalAttempts());
        po.setCorrectAttempts(record.correctAttempts());
        po.setMastery(record.mastery());
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private KnowledgeMasteryRecord toDomain(KnowledgeMasteryPo po) {
        return new KnowledgeMasteryRecord(po.getStudentId(), po.getCourseId(), po.getChapterId(), po.getMaterialId(),
                po.getKnowledgeName(), po.getTotalAttempts() == null ? 0 : po.getTotalAttempts(),
                po.getCorrectAttempts() == null ? 0 : po.getCorrectAttempts(), po.getMastery() == null ? 0.0 : po.getMastery(),
                toInstant(po.getUpdatedAt()));
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
