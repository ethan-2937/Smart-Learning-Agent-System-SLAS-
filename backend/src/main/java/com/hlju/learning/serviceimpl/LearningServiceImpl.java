package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.service.LearningService;
import com.hlju.learning.service.QuestionGenerationService;
import org.springframework.stereotype.Service;

@Service
public class LearningServiceImpl implements LearningService {
    private final QuestionGenerationService questionGenerationService;

    public LearningServiceImpl(QuestionGenerationService questionGenerationService) {
        this.questionGenerationService = questionGenerationService;
    }

    @Override
    public PracticeResult submitPractice(PracticeSubmitRequest request) {
        QuestionRecord question = questionGenerationService.listQuestions().stream()
                .filter(item -> item.questionId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("题目不存在：" + request.questionId()));
        String expected = question.answerText() == null ? "" : question.answerText().trim();
        String actual = request.answerText() == null ? "" : request.answerText().trim();
        boolean correct = !expected.isBlank() && expected.equalsIgnoreCase(actual);
        String feedback = correct ? "回答正确" : "已记录作答。开放题建议由教师或 AI 二次评分。";
        return new PracticeResult(question.questionId(), correct, question.answerText(), feedback);
    }
}
