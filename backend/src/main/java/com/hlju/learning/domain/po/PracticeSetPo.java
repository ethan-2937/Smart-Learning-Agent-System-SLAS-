package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class PracticeSetPo {
    private String practiceId;
    private String title;
    private String studentId;
    private String courseId;
    private String chapterId;
    private String materialId;
    private String questionIdsJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getQuestionIdsJson() { return questionIdsJson; }
    public void setQuestionIdsJson(String questionIdsJson) { this.questionIdsJson = questionIdsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
