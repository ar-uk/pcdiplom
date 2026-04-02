package org.example.pcbuilder.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank String username
) {
}
