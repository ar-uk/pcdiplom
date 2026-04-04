package org.example.pcbuilder.communityservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String authorUserId,
        String title,
        String body,
        Long buildId,
        String buildSnapshotJson,
        Integer score,
        Integer commentCount,
        List<String> tags,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
