package org.example.recommendationservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildRequest;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.dto.ChatRequest;
import org.example.recommendationservice.dto.ManualBuildDraftCreateRequest;
import org.example.recommendationservice.dto.ManualBuildDraftFinalizeRequest;
import org.example.recommendationservice.dto.ManualBuildDraftPartUpdateRequest;
import org.example.recommendationservice.dto.ManualBuildDraftResponse;
import org.example.recommendationservice.dto.ManualBuildFinalizeRequest;
import org.example.recommendationservice.dto.ManualBuildResponse;
import org.example.recommendationservice.dto.RecommendationEvaluationRequest;
import org.example.recommendationservice.dto.RecommendationEvaluationResponse;
import org.example.recommendationservice.service.FreshBuildRecommendationService;
import org.example.recommendationservice.service.ManualBuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final FreshBuildRecommendationService buildRecommendationService;
    private final ManualBuildService manualBuildService;

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

    @PostMapping("/evaluate")
    public ResponseEntity<RecommendationEvaluationResponse> evaluate(@Valid @RequestBody RecommendationEvaluationRequest request) {
        return ResponseEntity.ok(buildRecommendationService.evaluate(request));
    }

    @PostMapping("/manual-builds/finalize")
    public ResponseEntity<ManualBuildResponse> finalizeManualBuild(@Valid @RequestBody ManualBuildFinalizeRequest request) {
        return ResponseEntity.ok(manualBuildService.finalizeBuild(request));
    }

    @GetMapping("/manual-builds")
    public ResponseEntity<List<ManualBuildResponse>> listManualBuilds(@RequestParam String userId) {
        return ResponseEntity.ok(manualBuildService.listUserBuilds(userId));
    }

    @PostMapping("/manual-builds/drafts")
    public ResponseEntity<ManualBuildDraftResponse> createManualBuildDraft(@Valid @RequestBody ManualBuildDraftCreateRequest request) {
        return ResponseEntity.ok(manualBuildService.createDraft(request));
    }

    @GetMapping("/manual-builds/drafts/{draftId}")
    public ResponseEntity<ManualBuildDraftResponse> getManualBuildDraft(
            @PathVariable Long draftId,
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(manualBuildService.getDraft(draftId, userId));
    }

    @PatchMapping("/manual-builds/drafts/{draftId}/parts")
    public ResponseEntity<ManualBuildDraftResponse> updateManualBuildDraftPart(
            @PathVariable Long draftId,
            @Valid @RequestBody ManualBuildDraftPartUpdateRequest request
    ) {
        return ResponseEntity.ok(manualBuildService.updateDraftPart(draftId, request));
    }

    @PostMapping("/manual-builds/drafts/{draftId}/finalize")
    public ResponseEntity<ManualBuildResponse> finalizeManualBuildDraft(
            @PathVariable Long draftId,
            @Valid @RequestBody ManualBuildDraftFinalizeRequest request
    ) {
        return ResponseEntity.ok(manualBuildService.finalizeDraft(draftId, request));
    }
}
