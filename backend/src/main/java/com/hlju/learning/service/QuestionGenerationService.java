package com.hlju.learning.service;

import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.QuestionRecord;

import java.util.List;

public interface QuestionGenerationService {
    GenerationTaskRecord generate(GenerateQuestionRequest request);

    List<GenerationTaskRecord> listTasks();

    GenerationTaskRecord getTask(String taskId);

    List<QuestionRecord> listQuestions();

    QuestionRecord updateQuestionStatus(String questionId, boolean approved);
}
