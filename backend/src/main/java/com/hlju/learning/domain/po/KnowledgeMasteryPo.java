package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class KnowledgeMasteryPo {
    private String studentId;
    private String courseId;
    private String chapterId;
    private String materialId;
    private String knowledgeName;
    private Integer totalAttempts;
    private Integer correctAttempts;
    private Double mastery;
    private LocalDateTime updatedAt;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getKnowledgeName() { return knowledgeName; }
    public void setKnowledgeName(String knowledgeName) { this.knowledgeName = knowledgeName; }
    public Integer getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(Integer totalAttempts) { this.totalAttempts = totalAttempts; }
    public Integer getCorrectAttempts() { return correctAttempts; }
    public void setCorrectAttempts(Integer correctAttempts) { this.correctAttempts = correctAttempts; }
    public Double getMastery() { return mastery; }
    public void setMastery(Double mastery) { this.mastery = mastery; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
