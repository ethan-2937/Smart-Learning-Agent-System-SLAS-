package com.hlju.learning.service;

import com.hlju.learning.domain.learning.KnowledgeMasteryRecord;
import com.hlju.learning.domain.learning.PracticeAttemptRecord;
import com.hlju.learning.domain.learning.PracticeCreateRequest;
import com.hlju.learning.domain.learning.PracticeDetail;
import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSetRecord;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;
import com.hlju.learning.domain.learning.WrongQuestionRecord;

import java.util.List;

public interface LearningService {
    PracticeDetail createPractice(PracticeCreateRequest request);

    PracticeDetail getPractice(String practiceId);

    List<PracticeSetRecord> listPractices(String studentId);

    PracticeResult submitPractice(PracticeSubmitRequest request);

    List<PracticeAttemptRecord> listAttempts(String practiceId, String studentId);

    List<WrongQuestionRecord> listWrongQuestions(String studentId);

    List<KnowledgeMasteryRecord> listMastery(String studentId);
}
