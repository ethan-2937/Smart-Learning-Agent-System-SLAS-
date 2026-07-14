package com.hlju.learning.ai;

public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);

    default LlmCompletion completeWithMetadata(String systemPrompt, String userPrompt) {
        return new LlmCompletion(complete(systemPrompt, userPrompt), null);
    }
}
