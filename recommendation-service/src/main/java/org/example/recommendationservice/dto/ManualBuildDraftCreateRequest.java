package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualBuildDraftCreateRequest(
        @NotBlank String userId,
        String title
) {
}
