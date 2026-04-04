package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record ManualBuildFinalizeRequest(
        @NotBlank String userId,
        Long targetBuildId,
        String title,
        String description,
        Boolean publicBuild,
        String sourceSessionId,
        @NotNull Map<String, Object> selectedParts,
        Integer estimatedPower,
        List<String> compatibilityIssues
) {
}