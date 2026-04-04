package org.example.pcbuilder.communityservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdatePostRequest(
        @NotBlank String editorUserId,
        @Size(max = 200) String title,
        String body,
        List<String> tags,
        List<String> imageUrls
) {
}
