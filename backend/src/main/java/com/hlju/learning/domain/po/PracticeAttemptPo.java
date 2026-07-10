package com.hlju.learning.domain.po;

import java.time.LocalDateTime;

public class PracticeAttemptPo {
    private String attemptId;
    private String practiceId;
    private String studentId;
    private String questionId;
    private String answerText;
    private Boolean correct;
    private Double score;
    private String expectedAnswer;
    private String feedback;
    private String knowledgeNamesJson;
    private LocalDateTime submittedAt;

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }
    public String getPracticeId() { return practiceId; }
    public void setPracticeId(String practiceId) { this.practiceId = practiceId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getKnowledgeNamesJson() { return knowledgeNamesJson; }
    public void setKnowledgeNamesJson(String knowledgeNamesJson) { this.knowledgeNamesJson = knowledgeNamesJson; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
