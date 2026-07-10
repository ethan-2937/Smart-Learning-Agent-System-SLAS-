package com.hlju.learning.service;

import com.hlju.learning.domain.learning.PracticeResult;
import com.hlju.learning.domain.learning.PracticeSubmitRequest;

public interface LearningService {
    PracticeResult submitPractice(PracticeSubmitRequest request);
}
