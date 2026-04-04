package org.example.pcbuilder.communityservice.dto;

import org.example.pcbuilder.communityservice.model.VoteTargetType;

public record VoteResponse(
        VoteTargetType targetType,
        Long targetId,
        Integer score,
        Short yourVote
) {
}
