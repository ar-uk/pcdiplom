package org.example.pcbuilder.authservice.dto;

public record TokenResponse(
        String token,
        long expiresAtEpochMillis
) {
}
