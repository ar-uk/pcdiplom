package org.example.recommendationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildRequest;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.dto.ChatRequest;
import org.example.recommendationservice.service.FreshBuildRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final FreshBuildRecommendationService buildRecommendationService;

    @PostMapping("/build")
    public ResponseEntity<BuildResponse> build(@Valid @RequestBody BuildRequest request) {
        return ResponseEntity.ok(buildRecommendationService.createBuild(request));
    }

    @PostMapping("/chat/{sessionId}")
    public ResponseEntity<BuildResponse> chat(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatRequest request
    ) {
        return ResponseEntity.ok(buildRecommendationService.applyChat(sessionId, request));
    }
}
