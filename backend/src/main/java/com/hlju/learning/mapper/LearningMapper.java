package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.KnowledgeMasteryPo;
import com.hlju.learning.domain.po.PracticeAttemptPo;
import com.hlju.learning.domain.po.PracticeSetPo;
import com.hlju.learning.domain.po.WrongQuestionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningMapper {
    void upsertPracticeSet(@Param("po") PracticeSetPo po);

    PracticeSetPo findPracticeSet(@Param("practiceId") String practiceId);

    List<PracticeSetPo> findPracticeSets(@Param("studentId") String studentId);

    void insertAttempt(@Param("po") PracticeAttemptPo po);

    List<PracticeAttemptPo> findAttempts(@Param("practiceId") String practiceId, @Param("studentId") String studentId);

    WrongQuestionPo findWrongQuestion(@Param("studentId") String studentId, @Param("questionId") String questionId);

    void upsertWrongQuestion(@Param("po") WrongQuestionPo po);

    List<WrongQuestionPo> findWrongQuestions(@Param("studentId") String studentId);

    KnowledgeMasteryPo findMastery(@Param("studentId") String studentId,
                                   @Param("materialId") String materialId,
                                   @Param("knowledgeName") String knowledgeName);

    void upsertMastery(@Param("po") KnowledgeMasteryPo po);

    List<KnowledgeMasteryPo> findMasteryByStudent(@Param("studentId") String studentId);
}
