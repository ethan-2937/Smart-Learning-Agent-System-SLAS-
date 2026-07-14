package com.hlju.learning.serviceimpl.agent;

import com.hlju.learning.domain.agent.AgentRole;
import com.hlju.learning.domain.agent.AgentWorkflowData.QuestionReview;
import com.hlju.learning.domain.agent.AgentWorkflowData.ReviewDecision;
import com.hlju.learning.domain.agent.AgentWorkflowData.TeachingCompositionInput;
import com.hlju.learning.domain.agent.AgentWorkflowData.TeachingCompositionOutput;
import com.hlju.learning.domain.question.QuestionRecord;
import com.hlju.learning.domain.question.QuestionStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TeachingComposerAgent implements AgentRoleExecutor<TeachingCompositionInput, TeachingCompositionOutput> {
    @Override
    public AgentRole role() {
        return AgentRole.TEACHING_COMPOSER;
    }

    @Override
    public TeachingCompositionOutput execute(TeachingCompositionInput input, AgentExecutionContext context) {
        return context.call("composeTeacherReviewPackage",
                Map.of("candidateCount", input.deduplication().candidates().size(),
                        "reviewCount", input.review().reviews().size()),
                () -> compose(input),
                output -> Map.of("candidateCount", output.candidates().size(),
                        "teacherRecommendations", output.teacherRecommendations()));
    }

    private TeachingCompositionOutput compose(TeachingCompositionInput input) {
        List<QuestionRecord> candidates = input.deduplication().candidates().stream()
                .map(this::ensurePendingReview)
                .toList();
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Review every candidate against its source excerpt before approval.");
        for (QuestionReview review : input.review().reviews()) {
            if (review.decision() != ReviewDecision.PASS && review.decision() != ReviewDecision.REJECT) {
                recommendations.add(review.questionId() + " - " + review.decision() + ": "
                        + String.join(" ", review.reasons()));
            }
        }
        if (!input.deduplication().duplicateQuestionIds().isEmpty()) {
            recommendations.add("Duplicates removed: "
                    + String.join(", ", input.deduplication().duplicateQuestionIds()));
        }
        return new TeachingCompositionOutput(candidates, List.copyOf(recommendations));
    }

    private QuestionRecord ensurePendingReview(QuestionRecord question) {
        if (question.status() == QuestionStatus.PENDING_REVIEW) {
            return question;
        }
        return question.withStatus(QuestionStatus.PENDING_REVIEW);
    }
}
