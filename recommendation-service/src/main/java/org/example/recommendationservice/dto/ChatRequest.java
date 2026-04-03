package org.example.recommendationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String message
) {
}
