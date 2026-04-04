package org.example.recommendationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.ManualBuildDraftCreateRequest;
import org.example.recommendationservice.dto.ManualBuildDraftFinalizeRequest;
import org.example.recommendationservice.dto.ManualBuildDraftPartUpdateRequest;
import org.example.recommendationservice.dto.ManualBuildDraftResponse;
import org.example.recommendationservice.dto.ManualBuildFinalizeRequest;
import org.example.recommendationservice.dto.ManualBuildResponse;
import org.example.recommendationservice.model.ManualBuildDraft;
import org.example.recommendationservice.model.UserBuild;
import org.example.recommendationservice.repository.ManualBuildDraftRepository;
import org.example.recommendationservice.repository.UserBuildRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManualBuildService {

    private final UserBuildRepository userBuildRepository;
    private final ManualBuildDraftRepository manualBuildDraftRepository;
    private final ObjectMapper objectMapper;

    public ManualBuildDraftResponse createDraft(ManualBuildDraftCreateRequest request) {
        ManualBuildDraft draft = new ManualBuildDraft();
        draft.setUserId(normalizeUserId(request.userId()));
        draft.setTitle(resolveTitle(request.title()));
        draft.setBuildJson("{}");
        draft.setEstimatedPower(0);
        draft.setCompatibilityIssuesJson("[]");
        draft.setFinalized(false);

        return toDraftResponse(manualBuildDraftRepository.save(draft));
    }

    public ManualBuildDraftResponse getDraft(Long draftId, String userId) {
        ManualBuildDraft draft = findDraft(draftId, userId);
        return toDraftResponse(draft);
    }

    @Transactional
    public ManualBuildDraftResponse updateDraftPart(Long draftId, ManualBuildDraftPartUpdateRequest request) {
        ManualBuildDraft draft = findDraft(draftId, request.userId());
        if (draft.isFinalized()) {
            throw new IllegalStateException("Draft already finalized");
        }

        Map<String, Object> selectedParts = readSelectedPartsMap(draft.getBuildJson());
        if (request.part() == null) {
            selectedParts.remove(request.category());
        } else {
            selectedParts.put(request.category(), request.part());
        }

        draft.setBuildJson(writeJson(selectedParts));
        draft.setEstimatedPower(request.estimatedPower() == null ? 0 : request.estimatedPower());
        draft.setCompatibilityIssuesJson(writeJson(request.compatibilityIssues() == null ? List.of() : request.compatibilityIssues()));

        return toDraftResponse(manualBuildDraftRepository.save(draft));
    }

    @Transactional
    public ManualBuildResponse finalizeDraft(Long draftId, ManualBuildDraftFinalizeRequest request) {
        ManualBuildDraft draft = findDraft(draftId, request.userId());
        if (draft.isFinalized()) {
            throw new IllegalStateException("Draft already finalized");
        }

        String normalizedUserId = normalizeUserId(request.userId());
        UserBuild userBuild = resolveEditableBuild(request.targetBuildId(), normalizedUserId);
        userBuild.setUserId(normalizedUserId);
        userBuild.setTitle(resolveTitle(request.title() == null ? draft.getTitle() : request.title()));
        userBuild.setDescription(request.description());
        userBuild.setPublicBuild(Boolean.TRUE.equals(request.publicBuild()));
        userBuild.setSourceSessionId("draft:" + draftId);
        userBuild.setBuildJson(writeBuildJson(draft));

        UserBuild saved = userBuildRepository.save(userBuild);
        draft.setFinalized(true);
        manualBuildDraftRepository.save(draft);

        return toResponse(saved);
    }

    public ManualBuildResponse finalizeBuild(ManualBuildFinalizeRequest request) {
        String normalizedUserId = normalizeUserId(request.userId());
        UserBuild userBuild = resolveEditableBuild(request.targetBuildId(), normalizedUserId);
        userBuild.setUserId(normalizedUserId);
        userBuild.setTitle(resolveTitle(request.title()));
        userBuild.setDescription(request.description());
        userBuild.setPublicBuild(Boolean.TRUE.equals(request.publicBuild()));
        userBuild.setSourceSessionId(request.sourceSessionId());
        userBuild.setBuildJson(writeBuildJson(request));

        UserBuild saved = userBuildRepository.save(userBuild);
        return toResponse(saved);
    }

    public List<ManualBuildResponse> listUserBuilds(String userId) {
        return userBuildRepository.findAllByUserIdOrderByUpdatedAtDesc(normalizeUserId(userId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String writeBuildJson(ManualBuildDraft draft) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("selectedParts", readSelectedPartsMap(draft.getBuildJson()));
            payload.put("estimatedPower", draft.getEstimatedPower() == null ? 0 : draft.getEstimatedPower());
            payload.put("compatibilityIssues", readCompatibilityIssues(draft.getCompatibilityIssuesJson()));
            payload.put("finalized", true);
            payload.put("draftId", draft.getId());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize draft payload", exception);
        }
    }

    private String writeBuildJson(ManualBuildFinalizeRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("selectedParts", request.selectedParts());
            payload.put("estimatedPower", request.estimatedPower() == null ? 0 : request.estimatedPower());
            payload.put("compatibilityIssues", request.compatibilityIssues() == null ? List.of() : request.compatibilityIssues());
            payload.put("finalized", true);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize build payload", exception);
        }
    }

    private ManualBuildDraft findDraft(Long draftId, String userId) {
        return manualBuildDraftRepository.findByIdAndUserId(draftId, normalizeUserId(userId))
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSelectedPartsMap(String rawJson) {
        try {
            if (rawJson == null || rawJson.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(rawJson, Map.class);
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readCompatibilityIssues(String rawJson) {
        try {
            if (rawJson == null || rawJson.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(rawJson, List.class);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize JSON payload", exception);
        }
    }

    private String normalizeUserId(String userId) {
        return userId.trim().toLowerCase();
    }

    private String resolveTitle(String title) {
        String cleaned = title == null ? "" : title.trim();
        return cleaned.isEmpty() ? "Custom Build" : cleaned;
    }

    private UserBuild resolveEditableBuild(Long targetBuildId, String requestUserId) {
        if (targetBuildId == null) {
            return new UserBuild();
        }

        return userBuildRepository.findById(targetBuildId)
                .filter(existing -> normalizeUserId(existing.getUserId()).equals(requestUserId))
                .orElseGet(UserBuild::new);
    }

    private ManualBuildResponse toResponse(UserBuild userBuild) {
        return new ManualBuildResponse(
                userBuild.getId(),
                userBuild.getUserId(),
                userBuild.getTitle(),
                userBuild.getDescription(),
                userBuild.isPublicBuild(),
                userBuild.getSourceSessionId(),
                extractTotalPrice(userBuild.getBuildJson()),
                extractSelectedParts(userBuild.getBuildJson()),
                userBuild.getCreatedAt(),
                userBuild.getUpdatedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSelectedParts(String buildJson) {
        if (buildJson == null || buildJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(buildJson, Map.class);
            Object selectedPartsRaw = payload.get("selectedParts");
            if (selectedPartsRaw instanceof Map<?, ?> selectedParts) {
                return new LinkedHashMap<>((Map<String, Object>) selectedParts);
            }
            return new LinkedHashMap<>();
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Double extractTotalPrice(String buildJson) {
        if (buildJson == null || buildJson.isBlank()) {
            return 0.0;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(buildJson, Map.class);
            Object selectedPartsRaw = payload.get("selectedParts");
            if (!(selectedPartsRaw instanceof Map<?, ?> selectedParts)) {
                return 0.0;
            }

            double total = 0.0;
            for (Object partRaw : selectedParts.values()) {
                if (!(partRaw instanceof Map<?, ?> partMap)) {
                    continue;
                }

                Object priceRaw = partMap.get("price");
                if (priceRaw instanceof Number number) {
                    total += number.doubleValue();
                    continue;
                }

                if (priceRaw instanceof String priceText && !priceText.isBlank()) {
                    try {
                        total += Double.parseDouble(priceText.trim());
                    } catch (NumberFormatException ignored) {
                        // Skip malformed price values and continue summing known numeric prices.
                    }
                }
            }

            return total;
        } catch (Exception exception) {
            return 0.0;
        }
    }

    private ManualBuildDraftResponse toDraftResponse(ManualBuildDraft draft) {
        return new ManualBuildDraftResponse(
                draft.getId(),
                draft.getUserId(),
                draft.getTitle(),
                readSelectedPartsMap(draft.getBuildJson()),
                draft.getEstimatedPower() == null ? 0 : draft.getEstimatedPower(),
                new ArrayList<>(readCompatibilityIssues(draft.getCompatibilityIssuesJson())),
                draft.isFinalized(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}