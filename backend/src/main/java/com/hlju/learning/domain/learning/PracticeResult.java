package com.hlju.learning.domain.learning;

public record PracticeResult(String questionId, boolean correct, String expectedAnswer, String feedback) {
}
