package org.example.pcbuilder.communityservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.pcbuilder.communityservice.model.VoteTargetType;

public record CreateVoteRequest(
        @NotBlank String userId,
        @NotNull VoteTargetType targetType,
        @NotNull Long targetId,
        @NotNull Short value
) {
}
