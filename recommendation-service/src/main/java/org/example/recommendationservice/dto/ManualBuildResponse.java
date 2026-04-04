package org.example.recommendationservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ManualBuildResponse(
        Long id,
        String userId,
        String title,
        String description,
        boolean publicBuild,
        String sourceSessionId,
        Double totalPrice,
        Map<String, Object> selectedParts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}