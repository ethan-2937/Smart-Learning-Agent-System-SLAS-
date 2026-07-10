package com.hlju.learning.controller;

import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeCreateRequest;
import com.hlju.learning.domain.learning.PracticeDetail;
import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.domain.learning.WrongQuestionRecord;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.security.AuthException;
import com.hlju.learning.security.CurrentUserHolder;
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
        PracticeCreateRequest scopedRequest = new PracticeCreateRequest(effectiveStudentId(request.studentId()),
                request.courseId(), request.chapterId(), request.materialId(), request.count());
        return learningService.createPractice(scopedRequest);
    }

    @GetMapping("/api/practice/sets")
    public List<PracticeSetRecord> listPractices(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listPractices(effectiveStudentId(studentId));
    }

    @GetMapping("/api/practice/sets/{practiceId}")
    public PracticeDetail getPractice(@PathVariable String practiceId) {
        PracticeDetail detail = learningService.getPractice(practiceId);
        ensurePracticeVisible(detail.practice().studentId());
        return detail;
    }

    @PostMapping("/api/practice/submit")
    public PracticeResult submit(@RequestBody @Valid PracticeSubmitRequest request) {
        if (request.practiceId() != null && !request.practiceId().isBlank()) {
            ensurePracticeVisible(learningService.getPractice(request.practiceId()).practice().studentId());
        }
        return learningService.submitPractice(new PracticeSubmitRequest(request.practiceId(), request.questionId(),
                effectiveStudentId(request.studentId()), request.answerText()));
    }

    @GetMapping("/api/practice/attempts")
    public List<PracticeAttemptRecord> listAttempts(@RequestParam(value = "practiceId", required = false) String practiceId,
                                                    @RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listAttempts(practiceId, effectiveStudentId(studentId));
    }

    @GetMapping("/api/practice/wrong-questions")
    public List<WrongQuestionRecord> listWrongQuestions(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listWrongQuestions(effectiveStudentId(studentId));
    }

    @GetMapping("/api/practice/mastery")
    public List<KnowledgeMasteryRecord> listMastery(@RequestParam(value = "studentId", required = false) String studentId) {
        return learningService.listMastery(effectiveStudentId(studentId));
    }

    private String effectiveStudentId(String requestedStudentId) {
        AuthUser user = CurrentUserHolder.getRequired();
        if (canManageLearning(user)) {
            return requestedStudentId;
        }
        return user.userId();
    }

    private void ensurePracticeVisible(String practiceStudentId) {
        AuthUser user = CurrentUserHolder.getRequired();
        if (canManageLearning(user) || user.userId().equals(practiceStudentId)) {
            return;
        }
        throw new AuthException("只能查看自己的练习记录");
    }

    private boolean canManageLearning(AuthUser user) {
        return user.roles().contains("ADMIN") || user.roles().contains("TEACHER");
    }
}
