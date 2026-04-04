package org.example.pcbuilder.authservice.dto;

public record AuthResponse(
        String username,
        String email,
        String role,
        boolean verified,
        String token,
        long expiresAtEpochMillis
) {
}
