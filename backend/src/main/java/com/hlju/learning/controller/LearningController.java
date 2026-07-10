package com.hlju.learning.controller;

import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeCreateRequest;
import com.hlju.learning.domain.learning.PracticeDetail;
import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.domain.learning.WrongQuestionRecord;
import com.hlju.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LearningController {
    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/api/practice/sets")
    public PracticeDetail createPractice(@RequestBody PracticeCreateRequest request) {
        return learningService.createPractice(request);
    }

    @GetMapping("/api/practice/sets")
    public List<PracticeSetRecord> listPractices(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listPractices(studentId);
    }

    @GetMapping("/api/practice/sets/{practiceId}")
    public PracticeDetail getPractice(@PathVariable String practiceId) {
        return learningService.getPractice(practiceId);
    }

    @PostMapping("/api/practice/submit")
    public PracticeResult submit(@RequestBody @Valid PracticeSubmitRequest request) {
        return learningService.submitPractice(request);
    }

    @GetMapping("/api/practice/attempts")
    public List<PracticeAttemptRecord> listAttempts(@RequestParam(value = "practiceId", required = false) String practiceId,
                                                    @RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listAttempts(practiceId, studentId);
    }

    @GetMapping("/api/practice/wrong-questions")
    public List<WrongQuestionRecord> listWrongQuestions(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listWrongQuestions(studentId);
    }

    @GetMapping("/api/practice/mastery")
    public List<KnowledgeMasteryRecord> listMastery(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listMastery(studentId);
    }
}
