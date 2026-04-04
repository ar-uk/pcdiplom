package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record ManualBuildDraftPartUpdateRequest(
        @NotBlank String userId,
        @NotBlank String category,
        Map<String, Object> part,
        Integer estimatedPower,
        List<String> compatibilityIssues
) {
}
