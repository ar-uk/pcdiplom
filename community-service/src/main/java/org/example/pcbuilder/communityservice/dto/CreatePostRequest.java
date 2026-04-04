package org.example.pcbuilder.communityservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
        @NotBlank String authorUserId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        Long buildId,
        String buildSnapshotJson,
        List<String> tags,
        List<String> imageUrls
) {
}
