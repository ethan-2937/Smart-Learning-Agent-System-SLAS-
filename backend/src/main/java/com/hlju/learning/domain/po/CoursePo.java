package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class CoursePo {
    private String courseId;
    private String name;
    private String subjectPreset;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubjectPreset() { return subjectPreset; }
    public void setSubjectPreset(String subjectPreset) { this.subjectPreset = subjectPreset; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
