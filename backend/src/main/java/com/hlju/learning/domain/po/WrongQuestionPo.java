package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class WrongQuestionPo {
    private String studentId;
    private String questionId;
    private String materialId;
    private String prompt;
    private String expectedAnswer;
    private String lastAnswer;
    private String lastFeedback;
    private Integer wrongCount;
    private LocalDateTime lastSubmittedAt;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public String getLastAnswer() { return lastAnswer; }
    public void setLastAnswer(String lastAnswer) { this.lastAnswer = lastAnswer; }
    public String getLastFeedback() { return lastFeedback; }
    public void setLastFeedback(String lastFeedback) { this.lastFeedback = lastFeedback; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public LocalDateTime getLastSubmittedAt() { return lastSubmittedAt; }
    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) { this.lastSubmittedAt = lastSubmittedAt; }
}
