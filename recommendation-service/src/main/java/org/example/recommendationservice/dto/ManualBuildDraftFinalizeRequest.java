package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualBuildDraftFinalizeRequest(
        @NotBlank String userId,
        Long targetBuildId,
        String title,
        String description,
        Boolean publicBuild
) {
}
