package com.hlju.learning.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.ai.LlmClient;
import com.hlju.learning.config.AiProperties;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.question.GenerateQuestionRequest;
import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.GenerationTaskStatus;
import com.hlju.learning.domain.question.QuestionDifficulty;
import com.hlju.learning.domain.question.QuestionOption;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionSourceRef;
import com.hlju.learning.domain.question.QuestionStatus;
import com.hlju.learning.domain.question.QuestionType;
import com.hlju.learning.domain.rag.RetrievalHit;
import com.hlju.learning.domain.rag.RetrievalResult;
import com.hlju.learning.repository.QuestionRepository;
import com.hlju.learning.service.AgentRunService;
import com.hlju.learning.service.MaterialService;
import com.hlju.learning.service.QuestionGenerationService;
import com.hlju.learning.service.VectorRetrievalService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuestionGenerationServiceImpl implements QuestionGenerationService {
    private final MaterialService materialService;
    private final VectorRetrievalService retrievalService;
    private final AgentRunService agentRunService;
    private final QuestionRepository questionRepository;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public QuestionGenerationServiceImpl(MaterialService materialService, VectorRetrievalService retrievalService,
                                         AgentRunService agentRunService, QuestionRepository questionRepository,
                                         LlmClient llmClient, AiProperties aiProperties, ObjectMapper objectMapper) {
        this.materialService = materialService;
        this.retrievalService = retrievalService;
        this.agentRunService = agentRunService;
        this.questionRepository = questionRepository;
        this.llmClient = llmClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GenerationTaskRecord generate(GenerateQuestionRequest request) {
        MaterialRecord material = materialService.getMaterial(request.materialId());
        List<QuestionType> types = request.questionTypes() == null || request.questionTypes().isEmpty()
                ? List.of(QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE, QuestionType.SHORT_ANSWER)
                : request.questionTypes();
        QuestionDifficulty difficulty = request.difficulty() == null ? QuestionDifficulty.MEDIUM : request.difficulty();
        SubjectPreset subjectPreset = request.subjectPreset() == null ? material.subjectPreset() : request.subjectPreset();
        int count = Math.max(1, Math.min(20, request.count()));
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        GenerationTaskRecord task = new GenerationTaskRecord(taskId, material.materialId(), subjectPreset, request.topic(),
                types, difficulty, count, GenerationTaskStatus.RUNNING, null, List.of(), now, now);
        questionRepository.saveTask(task);

        RetrievalResult retrievalResult = retrievalService.retrieve(buildQuery(material, request), material.materialId(), Math.max(6, count));
        List<RetrievalHit> hits = retrievalResult.hits();
        if (hits.isEmpty()) {
            hits = materialService.listChunks(material.materialId()).stream()
                    .limit(Math.max(1, count))
                    .map(chunk -> new RetrievalHit(chunk, 0.2, "fallback material chunk"))
                    .toList();
        }
        List<QuestionRecord> generated = generateQuestions(taskId, material, subjectPreset, types, difficulty, count, hits);
        questionRepository.saveQuestions(generated);
        var agentRun = agentRunService.runQuestionWorkflow(taskId, request, hits, generated);
        GenerationTaskRecord finished = task.withResult(GenerationTaskStatus.FINISHED, agentRun.runId(), generated);
        questionRepository.saveTask(finished);
        return finished;
    }

    @Override
    public List<GenerationTaskRecord> listTasks() {
        return questionRepository.findAllTasks();
    }

    @Override
    public GenerationTaskRecord getTask(String taskId) {
        return questionRepository.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + taskId));
    }

    @Override
    public List<QuestionRecord> listQuestions() {
        return questionRepository.findAllQuestions();
    }

    @Override
    public QuestionRecord updateQuestionStatus(String questionId, boolean approved) {
        QuestionRecord record = questionRepository.findQuestion(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        QuestionRecord updated = record.withStatus(approved ? QuestionStatus.APPROVED : QuestionStatus.REJECTED);
        questionRepository.saveQuestion(updated);
        return updated;
    }

    private String buildQuery(MaterialRecord material, GenerateQuestionRequest request) {
        String topic = request.topic() == null || request.topic().isBlank() ? material.title() : request.topic();
        return material.subjectPreset() + " " + topic + " " + material.title();
    }

    private List<QuestionRecord> generateQuestions(String taskId, MaterialRecord material, SubjectPreset subjectPreset,
                                                   List<QuestionType> types, QuestionDifficulty difficulty,
                                                   int count, List<RetrievalHit> hits) {
        List<QuestionRecord> output = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QuestionType type = types.get(i % types.size());
            RetrievalHit hit = hits.get(i % hits.size());
            output.add(buildQuestion(taskId, material, subjectPreset, type, difficulty, hit, i + 1));
        }
        return output;
    }

    private QuestionRecord buildQuestion(String taskId, MaterialRecord material, SubjectPreset subjectPreset, QuestionType type,
                                         QuestionDifficulty difficulty, RetrievalHit hit, int index) {
        MaterialChunk chunk = hit.chunk();
        String snippet = shorten(chunk.text(), 160);
        QuestionRecord aiQuestion = tryBuildAiQuestion(taskId, material, subjectPreset, type, difficulty, hit, index, snippet);
        if (aiQuestion != null) {
            return aiQuestion;
        }
        String prompt;
        List<QuestionOption> options = List.of();
        String answer;
        String analysis;
        switch (type) {
            case TRUE_FALSE -> {
                prompt = "True or false: this material mainly discusses the following content:\n" + shorten(snippet, 90);
                answer = "True";
                analysis = "The statement is grounded in material source " + chunk.sourceLabel() + ".";
            }
            case FILL_BLANK -> {
                String keyword = chunk.keywords().isEmpty() ? "core concept" : chunk.keywords().get(0);
                prompt = "Fill in the blank: one key term in this material chunk is ______.";
                answer = keyword;
                analysis = "The keyword is extracted from the source chunk and should be checked by the teacher.";
            }
            case TRANSLATION -> {
                prompt = "Translate or paraphrase this material content:\n" + snippet;
                answer = "Reference answer should be reviewed according to the teaching target.";
                analysis = "This item works for English learning, bilingual learning, and general paraphrase practice.";
            }
            case DICTATION -> {
                prompt = "Dictation: write down the key sentence after listening to the teacher or TTS audio:\n" + shorten(snippet, 90);
                answer = shorten(snippet, 90);
                analysis = "Current version generates text dictation; TTS can be connected later.";
            }
            case SPEAKING_PROMPT -> {
                prompt = "Speaking prompt: give a 30-second oral response based on this material:\n" + snippet;
                answer = "The response should cover the core information clearly.";
                analysis = "This question type is useful for English speaking and general oral presentation.";
            }
            case WRITING -> {
                prompt = "Writing task: write a short summary or application note based on this material:\n" + snippet;
                answer = "A good answer should include topic, key concept, and application scenario.";
                analysis = "Open writing tasks can be graded manually or by a later AI scorer.";
            }
            case SHORT_ANSWER -> {
                prompt = "Short answer: summarize the core content of this material chunk:\n" + snippet;
                answer = "Summarize the key concepts, scenario, and learning objective from the source material.";
                analysis = "Short-answer questions are suitable for general learning and require source-grounded review.";
            }
            default -> {
                prompt = "Single choice: which option best matches the core content of this material?\nMaterial: " + snippet;
                options = List.of(
                        new QuestionOption("A", "Understand and apply the core concept from the material", true),
                        new QuestionOption("B", "Freely guess without using the material", false),
                        new QuestionOption("C", "Focus only on unrelated details", false),
                        new QuestionOption("D", "Ignore the source evidence", false)
                );
                answer = "A";
                analysis = "Option A is grounded in the retrieved material; the other options violate the source-grounding rule.";
            }
        }
        QuestionSourceRef sourceRef = new QuestionSourceRef(material.materialId(), chunk.chunkId(), chunk.chapterTitle(),
                chunk.pageNo(), snippet, hit.score());
        Instant now = Instant.now();
        return new QuestionRecord(UUID.randomUUID().toString(), taskId, material.materialId(), type, difficulty, subjectPreset,
                "Q" + index + ". " + prompt, options, answer, analysis, List.of(sourceRef), QuestionStatus.PENDING_REVIEW, now, now);
    }

    private QuestionRecord tryBuildAiQuestion(String taskId, MaterialRecord material, SubjectPreset subjectPreset,
                                              QuestionType type, QuestionDifficulty difficulty,
                                              RetrievalHit hit, int index, String snippet) {
        if (!isRemoteAiEnabled()) {
            return null;
        }
        try {
            String systemPrompt = """
                    You are a source-grounded teaching question generation agent.
                    Return only one valid JSON object. Do not return markdown.
                    The question must be answerable from the provided source excerpt.
                    """;
            String userPrompt = """
                    Generate one reviewable learning question.
                    Subject preset: %s
                    Question type: %s
                    Difficulty: %s
                    Topic: %s
                    Source excerpt: %s

                    JSON schema:
                    {
                      "prompt": "question stem",
                      "options": [{"label":"A","text":"option text","correct":true}],
                      "answerText": "reference answer",
                      "analysisText": "short explanation grounded in the source"
                    }
                    For non-choice questions, use an empty options array.
                    """.formatted(subjectPreset, type, difficulty, material.title(), snippet);
            String raw = llmClient.complete(systemPrompt, userPrompt);
            JsonNode root = objectMapper.readTree(extractJsonObject(raw));
            String prompt = root.path("prompt").asText("").trim();
            String answer = root.path("answerText").asText("").trim();
            String analysis = root.path("analysisText").asText("").trim();
            if (prompt.isBlank() || answer.isBlank()) {
                return null;
            }
            List<QuestionOption> options = parseOptions(root.path("options"));
            QuestionSourceRef sourceRef = new QuestionSourceRef(material.materialId(), hit.chunk().chunkId(), hit.chunk().chapterTitle(),
                    hit.chunk().pageNo(), snippet, hit.score());
            Instant now = Instant.now();
            return new QuestionRecord(UUID.randomUUID().toString(), taskId, material.materialId(), type, difficulty, subjectPreset,
                    "Q" + index + ". " + prompt, options, answer,
                    analysis.isBlank() ? "Generated by remote LLM and grounded in the retrieved source." : analysis,
                    List.of(sourceRef), QuestionStatus.PENDING_REVIEW, now, now);
        } catch (Exception ignored) {
            return null;
        }
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
        return options;
    }

    private String extractJsonObject(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private boolean isRemoteAiEnabled() {
        String provider = aiProperties.provider();
        return "openai-compatible".equalsIgnoreCase(provider)
                || "deepseek".equalsIgnoreCase(provider)
                || "deepseek-v4-flash".equalsIgnoreCase(provider);
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
