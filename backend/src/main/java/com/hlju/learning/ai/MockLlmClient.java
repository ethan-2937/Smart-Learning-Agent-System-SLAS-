package com.hlju.learning.ai;

import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "Mock LLM 已根据教材证据生成结构化结果。";
    }
}
