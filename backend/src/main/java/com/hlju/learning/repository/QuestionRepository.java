package com.hlju.learning.repository;

import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.QuestionRecord;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository {
    void saveTask(GenerationTaskRecord task);

    Optional<GenerationTaskRecord> findTask(String taskId);

    List<GenerationTaskRecord> findAllTasks();

    void saveQuestions(List<QuestionRecord> questions);

    List<QuestionRecord> findAllQuestions();

    List<QuestionRecord> findQuestionsByTaskId(String taskId);

    Optional<QuestionRecord> findQuestion(String questionId);

    void saveQuestion(QuestionRecord question);
}
