package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.GenerationTaskPo;
import com.hlju.learning.domain.po.QuestionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {
    void upsertTask(@Param("po") GenerationTaskPo po);

    GenerationTaskPo findTask(@Param("taskId") String taskId);

    List<GenerationTaskPo> findAllTasks();

    void upsertQuestion(@Param("po") QuestionPo po);

    QuestionPo findQuestion(@Param("questionId") String questionId);

    List<QuestionPo> findAllQuestions();

    List<QuestionPo> findQuestionsByTaskId(@Param("taskId") String taskId);
}
