package com.hlju.learning.ai;

public class MockLlmClient implements LlmClient {
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "Mock LLM 已根据教材证据生成结构化结果。";
    }

    @Override
    public LlmCompletion completeWithMetadata(String systemPrompt, String userPrompt) {
        return new LlmCompletion(complete(systemPrompt, userPrompt), 0);
    }
}
