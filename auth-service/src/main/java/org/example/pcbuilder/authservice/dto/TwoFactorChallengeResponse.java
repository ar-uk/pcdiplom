package org.example.pcbuilder.authservice.dto;

public record TwoFactorChallengeResponse(
        String challengeId,
        String email,
        long expiresAtEpochMillis,
        String message
) {
}