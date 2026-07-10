package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.question.GenerationTaskRecord;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.repository.QuestionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryQuestionRepository implements QuestionRepository {
    private final Map<String, GenerationTaskRecord> tasks = new ConcurrentHashMap<>();
    private final Map<String, QuestionRecord> questions = new ConcurrentHashMap<>();

    @Override
    public void saveTask(GenerationTaskRecord task) {
        tasks.put(task.taskId(), task);
    }

    @Override
    public Optional<GenerationTaskRecord> findTask(String taskId) {
        GenerationTaskRecord task = tasks.get(taskId);
        if (task == null) return Optional.empty();
        return Optional.of(task.withResult(task.status(), task.agentRunId(), findQuestionsByTaskId(taskId)));
    }

    @Override
    public List<GenerationTaskRecord> findAllTasks() {
        return tasks.values().stream()
                .map(task -> task.withResult(task.status(), task.agentRunId(), findQuestionsByTaskId(task.taskId())))
                .sorted(Comparator.comparing(GenerationTaskRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public void saveQuestions(List<QuestionRecord> newQuestions) {
        newQuestions.forEach(this::saveQuestion);
    }

    @Override
    public List<QuestionRecord> findAllQuestions() {
        return questions.values().stream()
                .sorted(Comparator.comparing(QuestionRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public List<QuestionRecord> findQuestionsByTaskId(String taskId) {
        return questions.values().stream()
                .filter(question -> taskId.equals(question.taskId()))
                .sorted(Comparator.comparing(QuestionRecord::createdAt))
                .toList();
    }

    @Override
    public Optional<QuestionRecord> findQuestion(String questionId) {
        return Optional.ofNullable(questions.get(questionId));
    }

    @Override
    public void saveQuestion(QuestionRecord question) {
        questions.put(question.questionId(), question);
    }
}
