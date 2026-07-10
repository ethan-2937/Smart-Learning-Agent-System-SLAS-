package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.course.KnowledgePointRecord;
import com.hlju.learning.domain.course.MaterialCourseBindingRecord;
import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeCreateRequest;
import com.hlju.learning.domain.learning.PracticeDetail;
import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.PracticeSetStatus;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.domain.learning.WrongQuestionRecord;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.repository.CourseRepository;
import com.hlju.learning.repository.LearningRepository;
import com.hlju.learning.service.LearningService;
import com.hlju.learning.service.QuestionGenerationService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LearningServiceImpl implements LearningService {
    private final QuestionGenerationService questionGenerationService;
    private final LearningRepository learningRepository;
    private final CourseRepository courseRepository;

    public LearningServiceImpl(QuestionGenerationService questionGenerationService,
                               LearningRepository learningRepository,
                               CourseRepository courseRepository) {
        this.questionGenerationService = questionGenerationService;
        this.learningRepository = learningRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public PracticeDetail createPractice(PracticeCreateRequest request) {
        String studentId = safeStudentId(request.studentId());
        int count = Math.max(1, Math.min(30, request.count() == null ? 10 : request.count()));
        List<QuestionRecord> candidates = questionGenerationService.listQuestions().stream()
                .filter(question -> request.materialId() == null || request.materialId().isBlank() || request.materialId().equals(question.materialId()))
                .filter(question -> matchCourseFilter(question, request.courseId(), request.chapterId()))
                .toList();
        List<QuestionRecord> approved = candidates.stream()
                .filter(question -> question.status() == QuestionStatus.APPROVED)
                .limit(count)
                .toList();
        List<QuestionRecord> selected = approved.isEmpty() ? candidates.stream().limit(count).toList() : approved;
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("No available questions for this practice scope");
        }
        Instant now = Instant.now();
        PracticeSetRecord practice = new PracticeSetRecord(UUID.randomUUID().toString(),
                "Practice - " + now.toString(), studentId, blankToNull(request.courseId()), blankToNull(request.chapterId()),
                blankToNull(request.materialId()), selected.stream().map(QuestionRecord::questionId).toList(),
                PracticeSetStatus.ACTIVE, now, now);
        learningRepository.savePracticeSet(practice);
        return new PracticeDetail(practice, selected, List.of());
    }

    @Override
    public PracticeDetail getPractice(String practiceId) {
        PracticeSetRecord practice = learningRepository.findPracticeSet(practiceId)
                .orElseThrow(() -> new IllegalArgumentException("Practice not found: " + practiceId));
        List<QuestionRecord> questions = practice.questionIds().stream().map(this::findQuestion).toList();
        List<PracticeAttemptRecord> attempts = learningRepository.findAttempts(practiceId, practice.studentId());
        return new PracticeDetail(practice, questions, attempts);
    }

    @Override
    public List<PracticeSetRecord> listPractices(String studentId) {
        return learningRepository.findPracticeSets(blankToNull(studentId));
    }

    @Override
    public PracticeResult submitPractice(PracticeSubmitRequest request) {
        QuestionRecord question = findQuestion(request.questionId());
        String studentId = safeStudentId(request.studentId());
        String practiceId = blankToNull(request.practiceId());
        double score = scoreAnswer(question, request.answerText());
        boolean correct = score >= 0.99;
        List<KnowledgePointRecord> points = findKnowledgePointsFor(question);
        List<String> knowledgeNames = points.isEmpty()
                ? List.of(question.type().name().toLowerCase(Locale.ROOT))
                : points.stream().map(KnowledgePointRecord::name).distinct().limit(3).toList();
        String feedback = correct
                ? "回答正确，知识掌握度已更新。"
                : "回答不完全正确，已加入错题本，建议结合解析复习。";
        PracticeAttemptRecord attempt = new PracticeAttemptRecord(UUID.randomUUID().toString(), practiceId, studentId,
                question.questionId(), request.answerText(), correct, score, question.answerText(), feedback,
                knowledgeNames, Instant.now());
        learningRepository.saveAttempt(attempt);
        if (!correct) {
            saveWrongQuestion(studentId, question, request.answerText(), feedback);
        }
        updateMastery(studentId, question, points, knowledgeNames, correct);
        return new PracticeResult(practiceId, attempt.attemptId(), question.questionId(), correct, score,
                question.answerText(), feedback, knowledgeNames);
    }

    @Override
    public List<PracticeAttemptRecord> listAttempts(String practiceId, String studentId) {
        return learningRepository.findAttempts(blankToNull(practiceId), blankToNull(studentId));
    }

    @Override
    public List<WrongQuestionRecord> listWrongQuestions(String studentId) {
        return learningRepository.findWrongQuestions(blankToNull(studentId));
    }

    @Override
    public List<KnowledgeMasteryRecord> listMastery(String studentId) {
        return learningRepository.findMasteryByStudent(blankToNull(studentId));
    }

    private boolean matchCourseFilter(QuestionRecord question, String courseId, String chapterId) {
        if ((courseId == null || courseId.isBlank()) && (chapterId == null || chapterId.isBlank())) {
            return true;
        }
        return courseRepository.findMaterialBinding(question.materialId())
                .map(binding -> (courseId == null || courseId.isBlank() || courseId.equals(binding.courseId()))
                        && (chapterId == null || chapterId.isBlank() || chapterId.equals(binding.chapterId())))
                .orElse(false);
    }

    private QuestionRecord findQuestion(String questionId) {
        return questionGenerationService.listQuestions().stream()
                .filter(item -> item.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    private double scoreAnswer(QuestionRecord question, String answerText) {
        String expected = normalize(question.answerText());
        String actual = normalize(answerText);
        if (expected.isBlank() || actual.isBlank()) {
            return 0.0;
        }
        if (expected.equals(actual)) {
            return 1.0;
        }
        boolean correctOption = question.options() != null && question.options().stream()
                .anyMatch(option -> option.correct() && normalize(option.label()).equals(actual));
        if (correctOption) {
            return 1.0;
        }
        if (expected.length() >= 6 && (expected.contains(actual) || actual.contains(expected))) {
            return 0.8;
        }
        Set<String> expectedTokens = tokens(expected);
        Set<String> actualTokens = tokens(actual);
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
            return 0.0;
        }
        long overlap = actualTokens.stream().filter(expectedTokens::contains).count();
        return Math.min(0.95, (double) overlap / expectedTokens.size());
    }

    private void saveWrongQuestion(String studentId, QuestionRecord question, String answerText, String feedback) {
        WrongQuestionRecord previous = learningRepository.findWrongQuestion(studentId, question.questionId()).orElse(null);
        int wrongCount = previous == null ? 1 : previous.wrongCount() + 1;
        WrongQuestionRecord record = new WrongQuestionRecord(studentId, question.questionId(), question.materialId(),
                question.prompt(), question.answerText(), answerText, feedback, wrongCount, Instant.now());
        learningRepository.saveWrongQuestion(record);
    }

    private void updateMastery(String studentId, QuestionRecord question, List<KnowledgePointRecord> points,
                               List<String> fallbackNames, boolean correct) {
        if (points.isEmpty()) {
            fallbackNames.forEach(name -> updateOneMastery(studentId, question.materialId(), null, null, name, correct));
            return;
        }
        points.stream().limit(3).forEach(point -> updateOneMastery(studentId, point.materialId(), point.courseId(),
                point.chapterId(), point.name(), correct));
    }

    private void updateOneMastery(String studentId, String materialId, String courseId, String chapterId,
                                  String knowledgeName, boolean correct) {
        KnowledgeMasteryRecord current = learningRepository.findMastery(studentId, materialId, knowledgeName)
                .orElse(new KnowledgeMasteryRecord(studentId, courseId, chapterId, materialId, knowledgeName, 0, 0, 0.0, Instant.now()));
        learningRepository.saveMastery(current.addAttempt(correct));
    }

    private List<KnowledgePointRecord> findKnowledgePointsFor(QuestionRecord question) {
        List<String> chunkIds = question.sourceRefs() == null ? List.of() : question.sourceRefs().stream()
                .map(QuestionSourceRef::chunkId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        List<KnowledgePointRecord> points = courseRepository.findKnowledgePoints(null, null, question.materialId());
        if (chunkIds.isEmpty()) {
            return points.stream().limit(3).toList();
        }
        List<KnowledgePointRecord> matched = points.stream()
                .filter(point -> chunkIds.contains(point.chunkId()))
                .limit(3)
                .toList();
        return matched.isEmpty() ? points.stream().limit(3).toList() : matched;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private Set<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> output = new LinkedHashSet<>();
        for (String token : value.split("[^a-z0-9\u4e00-\u9fa5]+")) {
            if (token.length() >= 2) {
                output.add(token);
            }
        }
        return output;
    }

    private String safeStudentId(String studentId) {
        return studentId == null || studentId.isBlank() ? "demo-student" : studentId.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
