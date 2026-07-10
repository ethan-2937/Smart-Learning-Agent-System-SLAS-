package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.po.GenerationTaskPo;
import com.hlju.learning.domain.po.QuestionPo;
import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.GenerationTaskStatus;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionOption;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.mapper.QuestionMapper;
import com.hlju.learning.repository.QuestionRepository;
import com.hlju.learning.util.JsonCodec;
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
public class MyBatisQuestionRepository implements QuestionRepository {
    private final QuestionMapper mapper;
    private final JsonCodec jsonCodec;

    public MyBatisQuestionRepository(QuestionMapper mapper, JsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void saveTask(GenerationTaskRecord task) {
        mapper.upsertTask(toPo(task));
    }

    @Override
    public Optional<GenerationTaskRecord> findTask(String taskId) {
        return Optional.ofNullable(mapper.findTask(taskId)).map(this::toDomain);
    }

    @Override
    public List<GenerationTaskRecord> findAllTasks() {
        return mapper.findAllTasks().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveQuestions(List<QuestionRecord> questions) {
        questions.forEach(this::saveQuestion);
    }

    @Override
    public List<QuestionRecord> findAllQuestions() {
        return mapper.findAllQuestions().stream().map(this::toDomain).toList();
    }

    @Override
    public List<QuestionRecord> findQuestionsByTaskId(String taskId) {
        return mapper.findQuestionsByTaskId(taskId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<QuestionRecord> findQuestion(String questionId) {
        return Optional.ofNullable(mapper.findQuestion(questionId)).map(this::toDomain);
    }

    @Override
    public void saveQuestion(QuestionRecord question) {
        mapper.upsertQuestion(toPo(question));
    }

    private GenerationTaskPo toPo(GenerationTaskRecord task) {
        GenerationTaskPo po = new GenerationTaskPo();
        po.setTaskId(task.taskId());
        po.setMaterialId(task.materialId());
        po.setSubjectPreset(task.subjectPreset().name());
        po.setTopic(task.topic());
        po.setQuestionTypesJson(jsonCodec.toJson(task.questionTypes()));
        po.setDifficulty(task.difficulty() == null ? null : task.difficulty().name());
        po.setRequestedCount(task.requestedCount());
        po.setStatus(task.status().name());
        po.setAgentRunId(task.agentRunId());
        po.setCreatedAt(toLocal(task.createdAt()));
        po.setUpdatedAt(toLocal(task.updatedAt()));
        return po;
    }

    private GenerationTaskRecord toDomain(GenerationTaskPo po) {
        List<QuestionType> types = jsonCodec.readList(po.getQuestionTypesJson(), new TypeReference<>() {});
        QuestionDifficulty difficulty = po.getDifficulty() == null ? null : QuestionDifficulty.valueOf(po.getDifficulty());
        return new GenerationTaskRecord(po.getTaskId(), po.getMaterialId(), SubjectPreset.valueOf(po.getSubjectPreset()),
                po.getTopic(), types, difficulty, po.getRequestedCount() == null ? 0 : po.getRequestedCount(),
                GenerationTaskStatus.valueOf(po.getStatus()), po.getAgentRunId(), findQuestionsByTaskId(po.getTaskId()),
                toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private QuestionPo toPo(QuestionRecord question) {
        QuestionPo po = new QuestionPo();
        po.setQuestionId(question.questionId());
        po.setTaskId(question.taskId());
        po.setMaterialId(question.materialId());
        po.setType(question.type().name());
        po.setDifficulty(question.difficulty().name());
        po.setSubjectPreset(question.subjectPreset().name());
        po.setPrompt(question.prompt());
        po.setOptionsJson(jsonCodec.toJson(question.options()));
        po.setAnswerText(question.answerText());
        po.setAnalysisText(question.analysisText());
        po.setSourceRefsJson(jsonCodec.toJson(question.sourceRefs()));
        po.setStatus(question.status().name());
        po.setCreatedAt(toLocal(question.createdAt()));
        po.setUpdatedAt(toLocal(question.updatedAt()));
        return po;
    }

    private QuestionRecord toDomain(QuestionPo po) {
        List<QuestionOption> options = jsonCodec.readList(po.getOptionsJson(), new TypeReference<>() {});
        List<QuestionSourceRef> refs = jsonCodec.readList(po.getSourceRefsJson(), new TypeReference<>() {});
        return new QuestionRecord(po.getQuestionId(), po.getTaskId(), po.getMaterialId(), QuestionType.valueOf(po.getType()),
                QuestionDifficulty.valueOf(po.getDifficulty()), SubjectPreset.valueOf(po.getSubjectPreset()), po.getPrompt(),
                options, po.getAnswerText(), po.getAnalysisText(), refs, QuestionStatus.valueOf(po.getStatus()),
                toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.now() : value.toInstant(ZoneOffset.UTC);
    }
}
