package org.example.recommendationservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ManualBuildDraftResponse(
        Long id,
        String userId,
        String title,
        Map<String, Object> selectedParts,
        int estimatedPower,
        List<String> compatibilityIssues,
        boolean finalized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
