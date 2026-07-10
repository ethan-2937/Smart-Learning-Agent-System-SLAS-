package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.AgentRunPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentRunMapper {
    void upsertAgentRun(@Param("po") AgentRunPo po);

    AgentRunPo findAgentRun(@Param("runId") String runId);

    List<AgentRunPo> findAllAgentRuns();
}
