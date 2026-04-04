package org.example.pcbuilder.communityservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String authorUserId,
        Long parentCommentId,
        @NotBlank String body
) {
}
