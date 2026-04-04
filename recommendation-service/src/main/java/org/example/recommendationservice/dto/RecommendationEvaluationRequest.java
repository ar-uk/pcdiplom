package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecommendationEvaluationRequest(
        @NotEmpty List<String> prompts,
        String currency,
        String region,
        Boolean strictBudget,
        String userId
) {
}
