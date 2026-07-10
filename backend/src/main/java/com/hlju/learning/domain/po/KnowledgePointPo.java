package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class KnowledgePointPo {
    private String knowledgePointId;
    private String courseId;
    private String chapterId;
    private String materialId;
    private String chunkId;
    private String name;
    private String description;
    private String sourceSnippet;
    private Double weight;
    private LocalDateTime createdAt;

    public String getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(String knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceSnippet() { return sourceSnippet; }
    public void setSourceSnippet(String sourceSnippet) { this.sourceSnippet = sourceSnippet; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
