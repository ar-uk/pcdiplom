package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildRequest(
        @NotBlank String prompt,
        String currency,
        String region,
        Boolean strictBudget,
        String userId
) {
}
