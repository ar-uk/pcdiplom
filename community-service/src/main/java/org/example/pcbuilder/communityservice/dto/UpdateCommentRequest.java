package org.example.pcbuilder.communityservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank String editorUserId,
        @NotBlank String body
) {
}
