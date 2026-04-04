package org.example.pcbuilder.communityservice.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String authorUserId,
        String body,
        Integer score,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
