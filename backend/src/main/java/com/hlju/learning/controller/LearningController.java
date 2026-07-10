package com.hlju.learning.controller;

import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearningController {
    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/api/practice/submit")
    public PracticeResult submit(@RequestBody @Valid PracticeSubmitRequest request) {
        return learningService.submitPractice(request);
    }
}
