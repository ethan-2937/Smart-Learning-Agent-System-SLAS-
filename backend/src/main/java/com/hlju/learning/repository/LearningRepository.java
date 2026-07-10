package com.hlju.learning.repository;

import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.WrongQuestionRecord;

import java.util.List;
import java.util.Optional;

public interface LearningRepository {
    void savePracticeSet(PracticeSetRecord practiceSet);

    Optional<PracticeSetRecord> findPracticeSet(String practiceId);

    List<PracticeSetRecord> findPracticeSets(String studentId);

    void saveAttempt(PracticeAttemptRecord attempt);

    List<PracticeAttemptRecord> findAttempts(String practiceId, String studentId);

    Optional<WrongQuestionRecord> findWrongQuestion(String studentId, String questionId);

    void saveWrongQuestion(WrongQuestionRecord wrongQuestion);

    List<WrongQuestionRecord> findWrongQuestions(String studentId);

    Optional<KnowledgeMasteryRecord> findMastery(String studentId, String materialId, String knowledgeName);

    void saveMastery(KnowledgeMasteryRecord mastery);

    List<KnowledgeMasteryRecord> findMasteryByStudent(String studentId);
}
