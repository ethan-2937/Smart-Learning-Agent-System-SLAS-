package com.hlju.learning.serviceimpl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.ai.LlmClient;
import com.hlju.learning.ai.LlmCompletion;
import com.hlju.learning.config.AiProperties;
import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionGenerationOutput;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.question.QuestionOption;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QuestionGenerationAgent implements AgentRoleExecutor<QuestionGenerationInput, QuestionGenerationOutput> {
    private static final String UNGROUNDED_MARKER =
            "DIRECT mode used no retrieved material evidence; teacher confirmation is required.";

    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final AgentExecutionRuntime runtime;

    public QuestionGenerationAgent(LlmClient llmClient, AiProperties aiProperties, ObjectMapper objectMapper,
                                   AgentExecutionRuntime runtime) {
        this.llmClient = llmClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.runtime = runtime;
    }

    @Override
    public AgentRole role() {
        return AgentRole.QUESTION_GENERATION;
    }

    @Override
    public QuestionGenerationOutput execute(QuestionGenerationInput input, AgentExecutionContext context) {
        List<RetrievalHit> evidence = input.retrieval() == null ? List.of() : input.retrieval().evidence();
        return context.call("generateQuestionDrafts",
                Map.of("mode", input.request().mode(), "requestedCount", input.request().count(),
                        "evidenceCount", evidence.size()),
                () -> generate(input, evidence, context),
                output -> Map.of("candidateIds", output.candidates().stream()
                                .map(QuestionRecord::questionId).toList(),
                        "grounded", output.grounded(),
                        "tokenUsage", String.valueOf(output.tokenUsage())));
    }

    private QuestionGenerationOutput generate(QuestionGenerationInput input, List<RetrievalHit> evidence,
                                               AgentExecutionContext context) {
        List<QuestionRecord> candidates = new ArrayList<>();
        boolean grounded = !evidence.isEmpty();
        int tokenUsage = 0;
        boolean tokenUsageUnavailable = false;
        for (int index = 0; index < input.request().count(); index++) {
            QuestionType type = input.request().questionTypes().get(index % input.request().questionTypes().size());
            RetrievalHit hit = grounded ? evidence.get(index % evidence.size()) : null;
            GeneratedQuestion generated = isRemoteAiEnabled()
                    ? buildRemote(input, type, hit, index + 1, context)
                    : new GeneratedQuestion(buildDeterministic(input, type, hit, index + 1), 0);
            candidates.add(generated.question());
            if (generated.tokenUsage() == null) {
                tokenUsageUnavailable = true;
            } else {
                tokenUsage += generated.tokenUsage();
            }
        }
        return new QuestionGenerationOutput(List.copyOf(candidates), grounded,
                tokenUsageUnavailable ? null : tokenUsage);
    }

    private GeneratedQuestion buildRemote(QuestionGenerationInput input, QuestionType type, RetrievalHit hit, int index,
                                          AgentExecutionContext context) {
        String source = hit == null ? UNGROUNDED_MARKER : shorten(hit.chunk().text(), 600);
        String systemPrompt = "You generate one reviewable teaching question. Return one JSON object only.";
        String userPrompt = """
                Mode: %s
                Subject: %s
                Type: %s
                Difficulty: %s
                Topic: %s
                Evidence: %s
                Return {"prompt":"...","options":[{"label":"A","text":"...","correct":true}],
                "answerText":"...","analysisText":"..."}. Use an empty options array when not applicable.
                """.formatted(input.request().mode(), input.request().subjectPreset(), type,
                input.request().difficulty(), safeTopic(input), source);
        LlmCompletion completion = context.call("llm.complete",
                Map.of("provider", aiProperties.provider(), "model", aiProperties.model(), "questionIndex", index),
                () -> llmClient.completeWithMetadata(systemPrompt, userPrompt),
                value -> Map.of("responseChars", value.content() == null ? 0 : value.content().length(),
                        "tokenUsage", String.valueOf(value.tokenUsage())));
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(completion.content()));
            String prompt = root.path("prompt").asText("").trim();
            String answer = root.path("answerText").asText("").trim();
            if (prompt.isBlank() || answer.isBlank()) {
                throw new IllegalStateException("LLM response omitted prompt or answerText");
            }
            List<QuestionOption> options = parseOptions(root.path("options"));
            String analysis = root.path("analysisText").asText("").trim();
            return new GeneratedQuestion(record(input, type, hit, index, prompt, options, answer, analysis),
                    completion.tokenUsage());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("LLM response was not valid question JSON", ex);
        }
    }

    private QuestionRecord buildDeterministic(QuestionGenerationInput input, QuestionType type, RetrievalHit hit, int index) {
        String source = hit == null ? safeTopic(input) : shorten(hit.chunk().text(), 160);
        String prompt;
        List<QuestionOption> options = List.of();
        String answer;
        String analysis;
        switch (type) {
            case TRUE_FALSE -> {
                prompt = "True or false: the learning focus includes this content: " + shorten(source, 100);
                answer = "True";
                analysis = hit == null ? UNGROUNDED_MARKER : "The statement is checked against the cited source excerpt.";
            }
            case FILL_BLANK -> {
                String keyword = hit != null && !hit.chunk().keywords().isEmpty()
                        ? hit.chunk().keywords().getFirst() : safeTopic(input);
                prompt = "Fill in the blank with the key concept: ______.";
                answer = keyword;
                analysis = hit == null ? UNGROUNDED_MARKER : "The answer comes from the cited material keyword.";
            }
            case TRANSLATION -> {
                prompt = "Translate or paraphrase this content: " + source;
                answer = hit == null ? "Teacher confirmation required for the direct-mode reference answer."
                        : "A faithful translation or paraphrase of the cited excerpt.";
                analysis = hit == null ? UNGROUNDED_MARKER : "Compare the response with the cited source meaning.";
            }
            case WRITING, SPEAKING_PROMPT -> {
                prompt = "Respond to this Business English topic using the given focus: " + source;
                answer = "Cover the main concept, its context, and an appropriate application.";
                analysis = hit == null ? UNGROUNDED_MARKER : "The response should stay consistent with the cited excerpt.";
            }
            case DICTATION -> {
                prompt = "Dictation: write the following key sentence after it is read aloud: " + shorten(source, 100);
                answer = shorten(source, 100);
                analysis = hit == null ? UNGROUNDED_MARKER : "The reference sentence is retained in the source citation.";
            }
            case SHORT_ANSWER -> {
                prompt = "Summarize the key point of this content: " + source;
                answer = hit == null ? "A teacher-reviewed summary of " + safeTopic(input) + "."
                        : "A concise summary consistent with the cited excerpt.";
                analysis = hit == null ? UNGROUNDED_MARKER : "The teacher can verify the answer against the citation.";
            }
            default -> {
                prompt = "Which option best reflects this learning content? " + source;
                options = List.of(
                        new QuestionOption("A", "Use the stated concept in its relevant context", true),
                        new QuestionOption("B", "Ignore the stated learning focus", false),
                        new QuestionOption("C", "Replace the content with an unrelated claim", false),
                        new QuestionOption("D", "Treat an unsupported guess as evidence", false));
                answer = "A";
                analysis = hit == null ? UNGROUNDED_MARKER : "Option A is the only option consistent with the citation.";
            }
        }
        return record(input, type, hit, index, prompt, options, answer, analysis);
    }

    private QuestionRecord record(QuestionGenerationInput input, QuestionType type, RetrievalHit hit, int index,
                                  String prompt, List<QuestionOption> options, String answer, String analysis) {
        QuestionSourceRef sourceRef = hit == null
                ? new QuestionSourceRef(input.request().material().materialId(), null, "UNGROUNDED_DIRECT_MODE",
                null, UNGROUNDED_MARKER, 0)
                : sourceRef(input, hit);
        var now = runtime.now();
        return new QuestionRecord(runtime.newId(), input.request().taskId(), input.request().material().materialId(),
                type, input.request().difficulty(), input.request().subjectPreset(), "Q" + index + ". " + prompt,
                options, answer, analysis, List.of(sourceRef), QuestionStatus.PENDING_REVIEW, now, now);
    }

    private QuestionSourceRef sourceRef(QuestionGenerationInput input, RetrievalHit hit) {
        MaterialChunk chunk = hit.chunk();
        return new QuestionSourceRef(input.request().material().materialId(), chunk.chunkId(), chunk.chapterTitle(),
                chunk.pageNo(), shorten(chunk.text(), 180), hit.score());
    }

    private List<QuestionOption> parseOptions(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<QuestionOption> options = new ArrayList<>();
        node.forEach(item -> {
            String label = item.path("label").asText("").trim();
            String text = item.path("text").asText("").trim();
            if (!label.isBlank() && !text.isBlank()) {
                options.add(new QuestionOption(label, text, item.path("correct").asBoolean(false)));
            }
        });
        return List.copyOf(options);
    }

    private String extractJsonObject(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : raw;
    }

    private boolean isRemoteAiEnabled() {
        String provider = aiProperties.provider();
        return "openai-compatible".equalsIgnoreCase(provider)
                || "deepseek".equalsIgnoreCase(provider)
                || "deepseek-v4-flash".equalsIgnoreCase(provider);
    }

    private String safeTopic(QuestionGenerationInput input) {
        String topic = input.request().topic();
        return topic == null || topic.isBlank() ? input.request().material().title() : topic.trim();
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private record GeneratedQuestion(QuestionRecord question, Integer tokenUsage) {
    }
}
