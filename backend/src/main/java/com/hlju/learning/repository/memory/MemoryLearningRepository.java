package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.WrongQuestionRecord;
import com.hlju.learning.repository.LearningRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryLearningRepository implements LearningRepository {
    private final Map<String, PracticeSetRecord> practiceSets = new ConcurrentHashMap<>();
    private final Map<String, PracticeAttemptRecord> attempts = new ConcurrentHashMap<>();
    private final Map<String, WrongQuestionRecord> wrongQuestions = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeMasteryRecord> masteryRecords = new ConcurrentHashMap<>();

    @Override
    public void savePracticeSet(PracticeSetRecord practiceSet) {
        practiceSets.put(practiceSet.practiceId(), practiceSet);
    }

    @Override
    public Optional<PracticeSetRecord> findPracticeSet(String practiceId) {
        return Optional.ofNullable(practiceSets.get(practiceId));
    }

    @Override
    public List<PracticeSetRecord> findPracticeSets(String studentId) {
        return practiceSets.values().stream()
                .filter(set -> studentId == null || studentId.isBlank() || studentId.equals(set.studentId()))
                .sorted(Comparator.comparing(PracticeSetRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public void saveAttempt(PracticeAttemptRecord attempt) {
        attempts.put(attempt.attemptId(), attempt);
    }

    @Override
    public List<PracticeAttemptRecord> findAttempts(String practiceId, String studentId) {
        return attempts.values().stream()
                .filter(attempt -> practiceId == null || practiceId.isBlank() || practiceId.equals(attempt.practiceId()))
                .filter(attempt -> studentId == null || studentId.isBlank() || studentId.equals(attempt.studentId()))
                .sorted(Comparator.comparing(PracticeAttemptRecord::submittedAt).reversed())
                .toList();
    }

    @Override
    public Optional<WrongQuestionRecord> findWrongQuestion(String studentId, String questionId) {
        return Optional.ofNullable(wrongQuestions.get(studentId + "::" + questionId));
    }

    @Override
    public void saveWrongQuestion(WrongQuestionRecord wrongQuestion) {
        wrongQuestions.put(wrongQuestion.studentId() + "::" + wrongQuestion.questionId(), wrongQuestion);
    }

    @Override
    public List<WrongQuestionRecord> findWrongQuestions(String studentId) {
        return wrongQuestions.values().stream()
                .filter(record -> studentId == null || studentId.isBlank() || studentId.equals(record.studentId()))
                .sorted(Comparator.comparing(WrongQuestionRecord::lastSubmittedAt).reversed())
                .toList();
    }

    @Override
    public Optional<KnowledgeMasteryRecord> findMastery(String studentId, String materialId, String knowledgeName) {
        return Optional.ofNullable(masteryRecords.get(studentId + "::" + materialId + "::" + knowledgeName));
    }

    @Override
    public void saveMastery(KnowledgeMasteryRecord mastery) {
        masteryRecords.put(mastery.studentId() + "::" + mastery.materialId() + "::" + mastery.knowledgeName(), mastery);
    }

    @Override
    public List<KnowledgeMasteryRecord> findMasteryByStudent(String studentId) {
        return masteryRecords.values().stream()
                .filter(record -> studentId == null || studentId.isBlank() || studentId.equals(record.studentId()))
                .sorted(Comparator.comparingDouble(KnowledgeMasteryRecord::mastery).thenComparing(KnowledgeMasteryRecord::updatedAt).reversed())
                .toList();
    }
}
