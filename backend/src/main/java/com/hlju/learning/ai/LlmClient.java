package com.hlju.learning.ai;

public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
}
