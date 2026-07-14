package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.agent.AgentRunStatus;
import com.hlju.learning.domain.agent.AgentWorkflowData.WorkflowRequest;
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
import com.hlju.learning.domain.question.UpdateQuestionRequest;
import com.hlju.learning.repository.QuestionRepository;
import com.hlju.learning.service.AgentRunService;
import com.hlju.learning.service.MaterialService;
import com.hlju.learning.service.QuestionGenerationService;
import com.hlju.learning.serviceimpl.agent.AgentExecutionRuntime;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionGenerationServiceImpl implements QuestionGenerationService {
    private final MaterialService materialService;
    private final AgentRunService agentRunService;
    private final QuestionRepository questionRepository;
    private final AgentExecutionRuntime runtime;

    public QuestionGenerationServiceImpl(MaterialService materialService, AgentRunService agentRunService,
                                         QuestionRepository questionRepository, AgentExecutionRuntime runtime) {
        this.materialService = materialService;
        this.agentRunService = agentRunService;
        this.questionRepository = questionRepository;
        this.runtime = runtime;
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
        String taskId = runtime.newId();
        Instant now = runtime.now();
        GenerationTaskRecord task = new GenerationTaskRecord(taskId, material.materialId(), subjectPreset, request.topic(),
                types, difficulty, count, GenerationTaskStatus.RUNNING, null, List.of(), now, now);
        questionRepository.saveTask(task);

        WorkflowRequest workflowRequest = new WorkflowRequest(taskId, material, subjectPreset, request.topic(),
                types, difficulty, count, request.resolvedWorkflowMode());
        var execution = agentRunService.runQuestionWorkflow(workflowRequest);
        if (execution.run().status() == AgentRunStatus.FAILED) {
            GenerationTaskRecord failed = task.withResult(GenerationTaskStatus.FAILED,
                    execution.run().runId(), List.of(), runtime.now());
            questionRepository.saveTask(failed);
            return failed;
        }
        questionRepository.saveQuestions(execution.questions());
        GenerationTaskRecord finished = task.withResult(GenerationTaskStatus.FINISHED,
                execution.run().runId(), execution.questions(), runtime.now());
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

    @Override
    public List<QuestionRecord> batchUpdateQuestionStatus(List<String> questionIds, boolean approved) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Question ids cannot be empty");
        }
        List<QuestionRecord> updated = new ArrayList<>();
        for (String questionId : questionIds) {
            if (questionId != null && !questionId.isBlank()) {
                updated.add(updateQuestionStatus(questionId, approved));
            }
        }
        return updated;
    }

    @Override
    public QuestionRecord updateQuestion(String questionId, UpdateQuestionRequest request) {
        QuestionRecord record = questionRepository.findQuestion(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        QuestionRecord updated = record.withContent(request.prompt(), request.options(), request.answerText(),
                request.analysisText(), request.difficulty());
        questionRepository.saveQuestion(updated);
        return updated;
    }

    @Override
    public byte[] exportQuestionsExcel(String materialId, QuestionStatus status) {
        List<QuestionRecord> records = questionRepository.findAllQuestions().stream()
                .filter(question -> materialId == null || materialId.isBlank() || materialId.equals(question.materialId()))
                .filter(question -> status == null || status == question.status())
                .toList();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"题目ID", "教材ID", "题型", "难度", "学科", "状态", "题干", "选项", "答案", "解析", "来源证据", "更新时间"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (QuestionRecord question : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(question.questionId());
                row.createCell(1).setCellValue(question.materialId());
                row.createCell(2).setCellValue(question.type().name());
                row.createCell(3).setCellValue(question.difficulty().name());
                row.createCell(4).setCellValue(question.subjectPreset().name());
                row.createCell(5).setCellValue(question.status().name());
                row.createCell(6).setCellValue(question.prompt());
                row.createCell(7).setCellValue(formatOptions(question.options()));
                row.createCell(8).setCellValue(question.answerText());
                row.createCell(9).setCellValue(question.analysisText() == null ? "" : question.analysisText());
                row.createCell(10).setCellValue(formatSources(question.sourceRefs()));
                row.createCell(11).setCellValue(question.updatedAt() == null ? "" : question.updatedAt().toString());
            }
            int[] widths = {18, 18, 14, 12, 18, 14, 48, 42, 32, 42, 48, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Export questions failed: " + ex.getMessage(), ex);
        }
    }

    private String formatOptions(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        return options.stream()
                .map(option -> option.label() + ". " + option.text() + (option.correct() ? " [正确]" : ""))
                .toList()
                .toString();
    }

    private String formatSources(List<QuestionSourceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return "";
        }
        return refs.stream()
                .map(ref -> ref.chapterTitle() + " / score=" + String.format("%.3f", ref.score()) + " / " + shorten(ref.snippet(), 180))
                .toList()
                .toString();
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
