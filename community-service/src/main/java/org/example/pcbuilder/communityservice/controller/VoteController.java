package org.example.pcbuilder.communityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.pcbuilder.communityservice.dto.CreateVoteRequest;
import org.example.pcbuilder.communityservice.dto.VoteResponse;
import org.example.pcbuilder.communityservice.model.VoteTargetType;
import org.example.pcbuilder.communityservice.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PutMapping
    public ResponseEntity<VoteResponse> castVote(@Valid @RequestBody CreateVoteRequest request) {
        return ResponseEntity.ok(voteService.castVote(request));
    }

    @DeleteMapping
    public ResponseEntity<VoteResponse> removeVote(
            @RequestParam String userId,
            @RequestParam VoteTargetType targetType,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(voteService.removeVote(userId, targetType, targetId));
    }
}
