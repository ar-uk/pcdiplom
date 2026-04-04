package org.example.pcbuilder.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MailTestRequest(
        @Email @NotBlank String email
) {
}