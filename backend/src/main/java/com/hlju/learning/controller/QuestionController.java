package com.hlju.learning.controller;

import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.service.QuestionGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuestionController {
    private final QuestionGenerationService questionGenerationService;

    public QuestionController(QuestionGenerationService questionGenerationService) {
        this.questionGenerationService = questionGenerationService;
    }

    @PostMapping("/api/questions/generation-tasks")
    public GenerationTaskRecord generate(@RequestBody @Valid GenerateQuestionRequest request) {
        return questionGenerationService.generate(request);
    }

    @GetMapping("/api/questions/generation-tasks")
    public List<GenerationTaskRecord> listTasks() {
        return questionGenerationService.listTasks();
    }

    @GetMapping("/api/questions/generation-tasks/{taskId}")
    public GenerationTaskRecord getTask(@PathVariable String taskId) {
        return questionGenerationService.getTask(taskId);
    }

    @GetMapping("/api/questions")
    public List<QuestionRecord> listQuestions() {
        return questionGenerationService.listQuestions();
    }

    @PostMapping("/api/questions/{questionId}/approve")
    public QuestionRecord approve(@PathVariable String questionId) {
        return questionGenerationService.updateQuestionStatus(questionId, true);
    }

    @PostMapping("/api/questions/{questionId}/reject")
    public QuestionRecord reject(@PathVariable String questionId) {
        return questionGenerationService.updateQuestionStatus(questionId, false);
    }
}
