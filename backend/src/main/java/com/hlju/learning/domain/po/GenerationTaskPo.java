package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class GenerationTaskPo {
    private String taskId;
    private String materialId;
    private String subjectPreset;
    private String topic;
    private String questionTypesJson;
    private String difficulty;
    private Integer requestedCount;
    private String status;
    private String agentRunId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getSubjectPreset() {
        return subjectPreset;
    }

    public void setSubjectPreset(String subjectPreset) {
        this.subjectPreset = subjectPreset;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getQuestionTypesJson() {
        return questionTypesJson;
    }

    public void setQuestionTypesJson(String questionTypesJson) {
        this.questionTypesJson = questionTypesJson;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(Integer requestedCount) {
        this.requestedCount = requestedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAgentRunId() {
        return agentRunId;
    }

    public void setAgentRunId(String agentRunId) {
        this.agentRunId = agentRunId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
