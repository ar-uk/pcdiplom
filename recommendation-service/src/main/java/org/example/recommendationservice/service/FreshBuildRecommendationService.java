package org.example.recommendationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.config.OpenAiProperties;
import org.example.recommendationservice.dto.BuildRequest;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.dto.ChatRequest;
import org.example.recommendationservice.model.AiSavedBuild;
import org.example.recommendationservice.repository.AiSavedBuildRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FreshBuildRecommendationService {

    private static final BigDecimal DEFAULT_BUDGET = new BigDecimal("500000");
    private static final BigDecimal USD_TO_KZT = new BigDecimal("455");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{2,7}(?:\\.\\d+)?)");
    private static final Pattern CPU_QUERY_PATTERN = Pattern.compile("(?i)(ryzen\\s+[3579]\\s+\\d{4}x?|ryzen\\s+[3579]\\s+\\d{4}|core\\s+i[3579]-?\\d{4,5}f?|i[3579]-?\\d{4,5}f?)");
    private static final Pattern GPU_QUERY_PATTERN = Pattern.compile("(?i)(rtx\\s*\\d{3,4}(?:\\s*ti)?|rx\\s*\\d{3,4}(?:\\s*xt)?|arc\\s*\\w+)");
    private static final Pattern STORAGE_QUERY_PATTERN = Pattern.compile("(?i)(\\d+\\s*tb|\\d+\\s*gb).*?(ssd|nvme|hdd|m\\.2)");

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final AiSavedBuildRepository aiSavedBuildRepository;

    @Value("${part-service.url}")
    private String partServiceUrl;

    private final RestClient restClient = RestClient.builder().build();

    public BuildResponse createBuild(BuildRequest request) {
        String sessionId = UUID.randomUUID().toString();
        return buildFromPrompt(sessionId, request.userId(), request.prompt(), request.currency(), request.region(), Boolean.TRUE.equals(request.strictBudget()));
    }

    public BuildResponse applyChat(String sessionId, ChatRequest request) {
        AiSavedBuild existing = aiSavedBuildRepository.findFirstBySessionId(sessionId).orElse(null);
        String userId = existing == null ? null : existing.getUserId();
        return buildFromPrompt(sessionId, userId, request.message(), existing == null ? "USD" : existing.getCurrency(), existing == null ? "US" : existing.getRegion(), existing != null && existing.isStrictBudget());
    }

    private BuildResponse buildFromPrompt(String sessionId, String userId, String prompt, String currency, String region, boolean strictBudget) {
        BuildResponse.IntentDto intent = extractIntent(prompt, currency, region, strictBudget);

        try {
            BigDecimal budget = intent.budgetKzt() == null || intent.budgetKzt().signum() <= 0 ? DEFAULT_BUDGET : intent.budgetKzt();
            Map<String, BuildResponse.PartDto> build = new LinkedHashMap<>();
            Map<String, List<BuildResponse.PartOptionDto>> options = new LinkedHashMap<>();
            List<String> reasoning = new ArrayList<>();

            String cpuQuery = extractCpuQuery(prompt, intent);
            List<BuildResponse.PartOptionDto> cpuOptions = selectOptionsFromContext("/api/parsed/cpu", cpuQuery, budget.multiply(new BigDecimal("0.25")), this::toCpuPart, "cpu", intent, prompt, strictBudget, 3);
            BuildResponse.PartDto cpu = bestPart(cpuOptions, "No part found");
            build.put("cpu", cpu);
            options.put("cpu", cpuOptions);

            String inferredSocket = inferCpuSocket(cpu.name());
            List<BuildResponse.PartOptionDto> motherboardOptions = selectOptionsFromContext(
                    "/api/parsed/motherboard",
                    inferredSocket,
                    budget.multiply(new BigDecimal("0.15")),
                    this::toMotherboardPart,
                    "motherboard",
                    intent,
                    prompt,
                        strictBudget,
                        3
            );
                    BuildResponse.PartDto motherboard = bestPart(motherboardOptions, "No part found");
            build.put("motherboard", motherboard);
                    options.put("motherboard", motherboardOptions);

            String memoryType = inferMemoryType(inferredSocket);
                    List<BuildResponse.PartOptionDto> memoryOptions = selectOptionsFromContext(
                    "/api/parsed/memory",
                    memoryType,
                    budget.multiply(new BigDecimal("0.12")),
                    this::toMemoryPart,
                    "memory",
                    intent,
                    prompt,
                    strictBudget,
                    3
            );
            BuildResponse.PartDto memory = bestPart(memoryOptions, "No part found");
            build.put("memory", memory);
            options.put("memory", memoryOptions);

            String storageQuery = extractStorageQuery(prompt);
            List<BuildResponse.PartOptionDto> storageOptions = selectOptionsFromContext("/api/parsed/internal-hard-drive", storageQuery, budget.multiply(new BigDecimal("0.12")), this::toStoragePart, "storage", intent, prompt, strictBudget, 3);
            BuildResponse.PartDto storage = bestPart(storageOptions, "No part found");
            build.put("storage", storage);
            options.put("storage", storageOptions);

            String gpuQuery = extractGpuQuery(prompt, intent);
            List<BuildResponse.PartOptionDto> gpuOptions = selectOptionsFromContext("/api/parsed/video-card", gpuQuery, budget.multiply(gpuBudgetShare(intent.useCase())), this::toGpuPart, "gpu", intent, prompt, strictBudget, 3);
            BuildResponse.PartDto gpu = bestPart(gpuOptions, "No part found");
            build.put("gpu", gpu);
            options.put("gpu", gpuOptions);

            int estimatedPower = safeInt(cpu.wattage(), 65) + safeInt(gpu.wattage(), 250) + 120;
            List<BuildResponse.PartOptionDto> psuOptions = selectPowerSupplyOptions(estimatedPower + 150, budget.multiply(new BigDecimal("0.11")), intent, prompt, strictBudget, 3);
            BuildResponse.PartDto psu = bestPart(psuOptions, "No PSU found");
            build.put("powerSupply", psu);
            options.put("powerSupply", psuOptions);

            List<BuildResponse.PartOptionDto> caseOptions = selectOptionsFromContext(
                    "/api/parsed/pc-case",
                    intent.constraints() == null ? null : intent.constraints().caseSize(),
                    budget.multiply(new BigDecimal("0.10")),
                    this::toCasePart,
                    "pcCase",
                    intent,
                    prompt,
                        strictBudget,
                        3
            );
                    BuildResponse.PartDto pcCase = bestPart(caseOptions, "No part found");
            build.put("pcCase", pcCase);
                    options.put("pcCase", caseOptions);

            BigDecimal total = build.values().stream()
                    .map(part -> part.priceKzt() == null ? BigDecimal.ZERO : part.priceKzt())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean socketCompatible = inferredSocket == null
                    || motherboard.socket() == null
                    || inferredSocket.equalsIgnoreCase(motherboard.socket());
            boolean budgetOk = !strictBudget || total.compareTo(budget) <= 0;
            boolean powerOk = safeInt(psu.wattage(), 650) >= estimatedPower;
            boolean memoryCompatible = memoryType == null
                    || memory.memoryType() == null
                    || memory.memoryType().equalsIgnoreCase(memoryType);

            reasoning.add("Fresh start: recommendations are now picked from parsed scrape tables, not the old catalog tables.");
            reasoning.add("Parts are ranked by intent context (use-case, priorities, resolution, constraints) and budget fit, not only by cheapest price.");
            reasoning.add("CPU socket inferred as " + (inferredSocket == null ? "unknown" : inferredSocket) + ", then matched against motherboard when possible.");
            reasoning.add("Each component now returns ranked alternatives so users can swap parts while keeping intent and budget context.");

            BigDecimal psuHeadroom = BigDecimal.ZERO;
            if (estimatedPower > 0) {
                psuHeadroom = BigDecimal.valueOf(safeInt(psu.wattage(), 650) - estimatedPower)
                        .multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(estimatedPower), 1, RoundingMode.HALF_UP);
            }

                BuildResponse response = new BuildResponse(
                    sessionId,
                    intent,
                    build,
                    options,
                    new BuildResponse.TotalsDto(total, estimatedPower, psuHeadroom),
                    new BuildResponse.ChecksDto(socketCompatible, memoryCompatible, powerOk, budgetOk),
                    reasoning,
                    List.of(
                            new BuildResponse.AlternativeDto("Lower cost", total.multiply(new BigDecimal("0.92")).setScale(2, RoundingMode.HALF_UP), "Drop to cheaper compatible options."),
                            new BuildResponse.AlternativeDto("Higher FPS", total.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP), "Spend more on the GPU tier.")
                    ),
                    buildMarketInsights(build, budget, intent)
            );
            saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, intent, response);
            return response;
        } catch (Exception exception) {
            BuildResponse response = mockBuildResponse(sessionId, intentFromPrompt(prompt));
            saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, response.intent(), response);
            return response;
        }
    }

    private void saveSnapshot(
            String sessionId,
            String userId,
            String prompt,
            String currency,
            String region,
            boolean strictBudget,
            BuildResponse.IntentDto intent,
            BuildResponse response
    ) {
        try {
                AiSavedBuild snapshot = aiSavedBuildRepository.findFirstBySessionId(sessionId).orElseGet(AiSavedBuild::new);
            snapshot.setSessionId(sessionId);
            snapshot.setUserId(userId);
            snapshot.setPrompt(prompt);
            snapshot.setCurrency(currency);
            snapshot.setRegion(region);
            snapshot.setStrictBudget(strictBudget);
            snapshot.setIntentJson(objectMapper.writeValueAsString(intent));
            snapshot.setBuildJson(objectMapper.writeValueAsString(response.build()));
            snapshot.setTotalsJson(objectMapper.writeValueAsString(response.totals()));
            snapshot.setChecksJson(objectMapper.writeValueAsString(response.checks()));
            snapshot.setReasoningJson(objectMapper.writeValueAsString(response.reasoning()));
            snapshot.setAlternativesJson(objectMapper.writeValueAsString(response.alternatives()));
            aiSavedBuildRepository.save(snapshot);
        } catch (Exception exception) {
            // Persisting the recommendation snapshot should not break the recommendation response.
        }
    }

    private BuildResponse.IntentDto extractIntent(String prompt, String currency, String region, Boolean strictBudget) {
        try {
            if (openAiProperties.apiKey() == null || openAiProperties.apiKey().isBlank()) {
                return intentFromPrompt(prompt);
            }

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", openAiProperties.model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "Extract PC build intent as strict JSON. Output only JSON with keys: budgetKzt, useCase, targetResolution, priorities, constraints. constraints keys: brandCpu, brandGpu, rgb, caseSize, wifiRequired."),
                            Map.of("role", "user", "content", "Prompt: " + prompt + "\nCurrency: " + currency + "\nRegion: " + region + "\nStrictBudget: " + strictBudget)
                    ),
                    "temperature", 0
            ));

            String response = restClient.post()
                    .uri(openAiProperties.url())
                    .header("Authorization", "Bearer " + openAiProperties.apiKey())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("{}");
            JsonNode intentNode = objectMapper.readTree(sanitizeJson(content));

            BigDecimal promptBudget = extractBudgetFromPrompt(prompt);
                BigDecimal aiBudgetKzt = extractAiBudgetKzt(intentNode, promptBudget);

            return new BuildResponse.IntentDto(
                    aiBudgetKzt.max(promptBudget),
                    normalizeUseCase(intentNode.path("useCase").asText("mixed"), prompt),
                    intentNode.path("targetResolution").asText("1080p"),
                    objectMapper.convertValue(intentNode.path("priorities"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)),
                    new BuildResponse.ConstraintsDto(
                            textOrNull(intentNode.path("constraints").path("brandCpu")),
                            textOrNull(intentNode.path("constraints").path("brandGpu")),
                            intentNode.path("constraints").path("rgb").asBoolean(false),
                            textOrNull(intentNode.path("constraints").path("caseSize")),
                            intentNode.path("constraints").path("wifiRequired").asBoolean(false)
                    )
            );
        } catch (Exception exception) {
            return intentFromPrompt(prompt);
        }
    }

    private BuildResponse.IntentDto intentFromPrompt(String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        return new BuildResponse.IntentDto(
                extractBudgetFromPrompt(prompt),
                inferUseCase(normalizedPrompt),
                normalizedPrompt.contains("4k") ? "4k" : normalizedPrompt.contains("1440") ? "1440p" : "1080p",
                buildPrioritiesFromPrompt(normalizedPrompt),
                new BuildResponse.ConstraintsDto(null, null, Boolean.FALSE, null, Boolean.FALSE)
        );
    }

    private List<BuildResponse.PartOptionDto> selectOptionsFromContext(
            String path,
            String query,
            BigDecimal budgetShare,
            PartMapper mapper,
            String component,
            BuildResponse.IntentDto intent,
            String prompt,
            boolean strictBudget,
            int limit
    ) {
        List<BuildResponse.PartDto> parts = fetchParts(path, 500, mapper);
        if (parts.isEmpty()) {
            return List.of();
        }

        String normalizedQuery = query == null ? null : query.trim();
        List<BuildResponse.PartDto> queryMatches = parts.stream()
                .filter(part -> matchesQuery(part.name(), normalizedQuery))
                .toList();

        List<BuildResponse.PartDto> candidatePool = queryMatches.isEmpty() ? parts : queryMatches;
        List<BuildResponse.PartDto> budgetCandidates = strictBudget
            ? candidatePool.stream().filter(part -> withinBudget(part.priceKzt(), budgetShare)).toList()
                : candidatePool;
        List<BuildResponse.PartDto> rankedPool = budgetCandidates.isEmpty() ? candidatePool : budgetCandidates;

        BigDecimal targetPrice = targetPriceForComponent(component, budgetShare, intent);
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);

        return rankedPool.stream()
                .map(part -> {
                    double score = scorePart(part, component, intent, normalizedPrompt, targetPrice);
                    return new BuildResponse.PartOptionDto(
                            part,
                            BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                            buildOptionReason(part, component, targetPrice, intent)
                    );
                })
                .sorted((left, right) -> right.score().compareTo(left.score()))
                .limit(Math.max(1, limit))
                .toList();
    }

    private List<BuildResponse.PartOptionDto> selectPowerSupplyOptions(
            int minWattage,
            BigDecimal budgetShare,
            BuildResponse.IntentDto intent,
            String prompt,
            boolean strictBudget,
            int limit
    ) {
        List<BuildResponse.PartDto> parts = fetchParts("/api/parsed/power-supply", 500, this::toPsuPart);
        if (parts.isEmpty()) {
            return List.of();
        }

        List<BuildResponse.PartDto> wattageCandidates = parts.stream()
                .filter(part -> safeInt(part.wattage(), 0) >= minWattage)
                .toList();
        List<BuildResponse.PartDto> candidatePool = wattageCandidates.isEmpty() ? parts : wattageCandidates;
        List<BuildResponse.PartDto> budgetCandidates = strictBudget
                ? candidatePool.stream().filter(part -> withinBudget(part.priceKzt(), budgetShare)).toList()
                : candidatePool;
        List<BuildResponse.PartDto> rankedPool = budgetCandidates.isEmpty() ? candidatePool : budgetCandidates;

        BigDecimal targetPrice = targetPriceForComponent("powerSupply", budgetShare, intent);
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);

        return rankedPool.stream()
                .map(part -> {
                    double score = scorePart(part, "powerSupply", intent, normalizedPrompt, targetPrice);
                    int headroom = safeInt(part.wattage(), minWattage) - minWattage;
                    score += headroom >= 80 && headroom <= 250 ? 12.0 : 4.0;
                    return new BuildResponse.PartOptionDto(
                            part,
                            BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                            "Meets power target with " + Math.max(0, headroom) + "W headroom."
                    );
                })
                .sorted((left, right) -> right.score().compareTo(left.score()))
                .limit(Math.max(1, limit))
                .toList();
    }

    private BuildResponse.PartDto bestPart(List<BuildResponse.PartOptionDto> options, String fallbackName) {
        if (options == null || options.isEmpty()) {
            return new BuildResponse.PartDto(null, fallbackName, BigDecimal.ZERO, null, null, null, null);
        }
        return options.get(0).part();
    }

    private String buildOptionReason(
            BuildResponse.PartDto part,
            String component,
            BigDecimal targetPrice,
            BuildResponse.IntentDto intent
    ) {
        StringBuilder reason = new StringBuilder();
        if (part.priceKzt() != null && targetPrice != null && targetPrice.signum() > 0) {
            BigDecimal diff = part.priceKzt().subtract(targetPrice);
            if (diff.signum() <= 0) {
                reason.append("Good value for this budget tier. ");
            } else {
                reason.append("Higher tier than budget midpoint. ");
            }
        }

        String normalizedName = normalize(part.name());
        if ("gpu".equals(component) && (normalizedName.contains("4070") || normalizedName.contains("7800") || normalizedName.contains("7900") || normalizedName.contains("4090"))) {
            reason.append("Strong GPU class for high FPS. ");
        }
        if ("cpu".equals(component) && (normalizedName.contains("ryzen 7") || normalizedName.contains("ryzen 9") || normalizedName.contains("core i7") || normalizedName.contains("core i9"))) {
            reason.append("High-performance CPU tier. ");
        }
        if ("memory".equals(component) && normalizedName.contains("32gb")) {
            reason.append("Balanced capacity for modern builds. ");
        }
        if ("storage".equals(component) && (normalizedName.contains("nvme") || normalizedName.contains("ssd"))) {
            reason.append("Fast storage profile. ");
        }
        if (intent != null && intent.constraints() != null && Boolean.TRUE.equals(intent.constraints().wifiRequired()) && "motherboard".equals(component) && normalizedName.contains("wifi")) {
            reason.append("Matches Wi-Fi requirement. ");
        }

        String text = reason.toString().trim();
        return text.isBlank() ? "Balanced option for current intent and budget." : text;
    }

    private List<String> buildMarketInsights(
            Map<String, BuildResponse.PartDto> build,
            BigDecimal total,
            BuildResponse.IntentDto intent
    ) {
        List<String> insights = new ArrayList<>();
        if (total == null || total.signum() <= 0) {
            return List.of("Market insights unavailable: no priced parts found.");
        }

        BigDecimal gpuPrice = priceOf(build.get("gpu"));
        BigDecimal memoryPrice = priceOf(build.get("memory"));
        BigDecimal cpuPrice = priceOf(build.get("cpu"));
        BigDecimal gpuShare = percentOf(gpuPrice, total);
        BigDecimal memoryShare = percentOf(memoryPrice, total);
        BigDecimal cpuShare = percentOf(cpuPrice, total);

        if (memoryShare.compareTo(new BigDecimal("12")) > 0) {
            insights.add("RAM pricing looks elevated right now: memory is taking " + memoryShare.setScale(1, RoundingMode.HALF_UP) + "% of this build.");
        }
        if (gpuShare.compareTo(new BigDecimal("45")) > 0) {
            insights.add("GPU market is dominating this budget tier: graphics card share is " + gpuShare.setScale(1, RoundingMode.HALF_UP) + "%.");
        }
        if (cpuShare.compareTo(new BigDecimal("30")) > 0 && (intent == null || !"creator".equalsIgnoreCase(intent.useCase()))) {
            insights.add("CPU cost is relatively high for this profile; consider dropping one CPU tier for better value.");
        }
        if (insights.isEmpty()) {
            insights.add("Current component pricing looks balanced for this budget segment.");
        }
        return insights;
    }

    private BigDecimal priceOf(BuildResponse.PartDto part) {
        return part == null || part.priceKzt() == null ? BigDecimal.ZERO : part.priceKzt();
    }

    private BigDecimal percentOf(BigDecimal partValue, BigDecimal total) {
        if (total == null || total.signum() <= 0 || partValue == null) {
            return BigDecimal.ZERO;
        }
        return partValue.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private double scorePart(
            BuildResponse.PartDto part,
            String component,
            BuildResponse.IntentDto intent,
            String normalizedPrompt,
            BigDecimal targetPrice
    ) {
        double score = 0.0;

        if (part.priceKzt() != null && part.priceKzt().signum() > 0 && targetPrice != null && targetPrice.signum() > 0) {
            BigDecimal delta = part.priceKzt().subtract(targetPrice).abs();
            BigDecimal ratio = delta.divide(targetPrice, 4, RoundingMode.HALF_UP);
            score += Math.max(0.0, 50.0 - ratio.doubleValue() * 50.0);
        }

        score += tierScore(part.name(), component, intent);

        if ("memory".equals(component)) {
            score += memoryScore(part.name(), part.memoryType(), intent, normalizedPrompt);
        }

        if ("storage".equals(component)) {
            score += storageScore(part.name(), part.wattage(), intent, normalizedPrompt);
        }

        if ("pcCase".equals(component) && intent.constraints() != null && Boolean.TRUE.equals(intent.constraints().rgb())) {
            String normalizedName = normalize(part.name());
            if (normalizedName.contains("rgb") || normalizedName.contains("argb")) {
                score += 8.0;
            }
        }

        if ("motherboard".equals(component) && intent.constraints() != null && Boolean.TRUE.equals(intent.constraints().wifiRequired())) {
            String normalizedName = normalize(part.name());
            if (normalizedName.contains("wifi") || normalizedName.contains("wi fi")) {
                score += 8.0;
            }
        }

        return score;
    }

    private BigDecimal targetPriceForComponent(String component, BigDecimal budgetShare, BuildResponse.IntentDto intent) {
        if (budgetShare == null || budgetShare.signum() <= 0) {
            return new BigDecimal("1");
        }

        BigDecimal multiplier = new BigDecimal("1.00");
        String useCase = intent == null || intent.useCase() == null ? "mixed" : intent.useCase().toLowerCase(Locale.ROOT);

        if ("gaming".equals(useCase) && ("gpu".equals(component) || "cpu".equals(component))) {
            multiplier = multiplier.add(new BigDecimal("0.12"));
        }
        if ("creator".equals(useCase) && ("cpu".equals(component) || "memory".equals(component) || "storage".equals(component))) {
            multiplier = multiplier.add(new BigDecimal("0.10"));
        }
        if ("office".equals(useCase)) {
            multiplier = multiplier.subtract(new BigDecimal("0.12"));
        }

        if (intent != null && intent.priorities() != null) {
            for (String priority : intent.priorities()) {
                String p = priority == null ? "" : priority.toLowerCase(Locale.ROOT);
                if (("fps".equals(p) || "performance".equals(p)) && ("gpu".equals(component) || "cpu".equals(component))) {
                    multiplier = multiplier.add(new BigDecimal("0.10"));
                }
                if ("value".equals(p)) {
                    multiplier = multiplier.subtract(new BigDecimal("0.08"));
                }
                if ("upgradability".equals(p) && ("motherboard".equals(component) || "powerSupply".equals(component))) {
                    multiplier = multiplier.add(new BigDecimal("0.06"));
                }
            }
        }

        if (intent != null && intent.targetResolution() != null && "gpu".equals(component)) {
            String resolution = intent.targetResolution().toLowerCase(Locale.ROOT);
            if (resolution.contains("4k")) {
                multiplier = multiplier.add(new BigDecimal("0.12"));
            } else if (resolution.contains("1440")) {
                multiplier = multiplier.add(new BigDecimal("0.06"));
            }
        }

        BigDecimal bounded = multiplier.max(new BigDecimal("0.65")).min(new BigDecimal("1.35"));
        return budgetShare.multiply(bounded);
    }

    private double tierScore(String name, String component, BuildResponse.IntentDto intent) {
        if (name == null) {
            return 0.0;
        }
        String normalized = normalize(name);

        if ("gpu".equals(component)) {
            int tier = extractFirstInt(normalized, 0);
            if (tier >= 7900 || tier >= 4090) {
                return 18.0;
            }
            if (tier >= 7800 || tier >= 4070) {
                return 14.0;
            }
            if (tier >= 7700 || tier >= 4060) {
                return 10.0;
            }
            if (tier >= 7600 || tier >= 3060) {
                return 7.0;
            }
            return 4.0;
        }

        if ("cpu".equals(component)) {
            int tier = extractFirstInt(normalized, 0);
            if (normalized.contains("ryzen 9") || normalized.contains("core i9")) {
                return 14.0;
            }
            if (normalized.contains("ryzen 7") || normalized.contains("core i7")) {
                return 10.0;
            }
            if (normalized.contains("ryzen 5") || normalized.contains("core i5")) {
                return 7.0;
            }
            if (tier >= 9000 || tier >= 14000) {
                return 9.0;
            }
            return 4.0;
        }

        if ("memory".equals(component) && intent != null && "creator".equalsIgnoreCase(intent.useCase())) {
            if (normalized.contains("64gb")) {
                return 10.0;
            }
            if (normalized.contains("32gb")) {
                return 7.0;
            }
        }

        return 0.0;
    }

    private double memoryScore(String name, String memoryType, BuildResponse.IntentDto intent, String normalizedPrompt) {
        double score = 0.0;
        String normalizedName = normalize(name);

        if (memoryType != null && normalizedPrompt.contains(memoryType.toLowerCase(Locale.ROOT))) {
            score += 10.0;
        }
        if (normalizedName.contains("32gb")) {
            score += intent != null && "creator".equalsIgnoreCase(intent.useCase()) ? 10.0 : 6.0;
        }
        if (normalizedName.contains("16gb")) {
            score += 5.0;
        }
        if (normalizedName.contains("64gb")) {
            score += intent != null && "creator".equalsIgnoreCase(intent.useCase()) ? 12.0 : 4.0;
        }
        return score;
    }

    private double storageScore(String name, Integer capacityMaybe, BuildResponse.IntentDto intent, String normalizedPrompt) {
        double score = 0.0;
        String normalizedName = normalize(name);
        Integer capacity = extractStorageCapacity(name);

        if (normalizedName.contains("nvme") || normalizedName.contains("m 2") || normalizedName.contains("ssd")) {
            score += 8.0;
        }
        if (capacity != null) {
            if (capacity >= 2000) {
                score += 8.0;
            } else if (capacity >= 1000) {
                score += 6.0;
            } else if (capacity >= 500) {
                score += 3.0;
            }
        }
        if (intent != null && "creator".equalsIgnoreCase(intent.useCase()) && capacity != null && capacity >= 1000) {
            score += 4.0;
        }
        if (normalizedPrompt.contains("hdd") && normalizedName.contains("hdd")) {
            score += 6.0;
        }
        return score;
    }

    private List<BuildResponse.PartDto> fetchParts(String path, int size, PartMapper mapper) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(partServiceUrl + path)
                    .queryParam("page", 0)
                    .queryParam("size", size);

            String response = restClient.get()
                    .uri(builder.toUriString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (!content.isArray()) {
                return List.of();
            }

            List<BuildResponse.PartDto> parts = new ArrayList<>();
            for (JsonNode node : content) {
                parts.add(mapper.map(node));
            }
            return parts;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private BuildResponse.PartDto toCpuPart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("CPU");
        String socket = inferCpuSocket(name);
        Integer wattage = extractFirstInt(name, 65);
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, socket, inferMemoryType(socket), wattage, null);
    }

    private BuildResponse.PartDto toGpuPart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("GPU");
        Integer wattage = extractFirstInt(name, 250);
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, null, null, wattage, name);
    }

    private BuildResponse.PartDto toMotherboardPart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("Motherboard");
        String socket = inferSocketFromText(name);
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, socket, inferMemoryType(socket), null, null);
    }

    private BuildResponse.PartDto toMemoryPart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("Memory");
        String memoryType = name.toUpperCase(Locale.ROOT).contains("DDR4") ? "DDR4" : name.toUpperCase(Locale.ROOT).contains("DDR5") ? "DDR5" : null;
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, null, memoryType, null, null);
    }

    private BuildResponse.PartDto toStoragePart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("Storage");
        Integer capacity = extractStorageCapacity(name);
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, null, null, capacity, null);
    }

    private BuildResponse.PartDto toPsuPart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("PSU");
        Integer wattage = extractFirstInt(name, 650);
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, null, null, wattage, null);
    }

    private BuildResponse.PartDto toCasePart(JsonNode node) {
        BigDecimal price = kztFromNode(node.path("priceKzt"));
        String name = node.path("name").asText("Case");
        return new BuildResponse.PartDto(node.path("id").asLong(), name, price, null, null, null, null);
    }

    private boolean matchesQuery(String name, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (name == null) {
            return false;
        }

        String normalizedName = normalize(name);
        String normalizedQuery = normalize(query);

        if (normalizedQuery.contains("5060") && !normalizedQuery.contains("ti") && normalizedName.contains("5060 ti")) {
            return false;
        }

        for (String token : normalizedQuery.split("\\s+")) {
            if (token.length() > 1 && !normalizedName.contains(token)) {
                return false;
            }
        }

        return true;
    }

    private boolean withinBudget(BigDecimal priceKzt, BigDecimal budgetShare) {
        if (priceKzt == null || budgetShare == null) {
            return true;
        }
        return priceKzt.compareTo(budgetShare.max(new BigDecimal("1"))) <= 0;
    }

    private BigDecimal kztFromNode(JsonNode priceNode) {
        if (priceNode == null || priceNode.isMissingNode() || priceNode.isNull()) {
            return BigDecimal.ZERO;
        }

        BigDecimal kzt;
        try {
            if (priceNode.isNumber()) {
                kzt = priceNode.decimalValue();
            } else {
                kzt = new BigDecimal(priceNode.asText("0"));
            }
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
        return kzt.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal extractBudgetFromPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return DEFAULT_BUDGET;
        }

        String normalized = prompt.toLowerCase(Locale.ROOT);
        if (hasUnlimitedBudgetSignal(normalized)) {
            return new BigDecimal("5000000");
        }
        boolean tenge = normalized.contains("tenge") || normalized.contains("kzt") || normalized.contains("₸");
        boolean usd = normalized.contains("usd") || normalized.contains("$") || normalized.contains("dollar");

        Matcher matcher = NUMBER_PATTERN.matcher(prompt.replace(",", ""));
        List<BigDecimal> numbers = new ArrayList<>();
        while (matcher.find()) {
            try {
                numbers.add(new BigDecimal(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        BigDecimal raw = numbers.stream().max(BigDecimal::compareTo).orElse(DEFAULT_BUDGET);
        if (tenge) {
            return raw.setScale(0, RoundingMode.HALF_UP).max(new BigDecimal("150000"));
        }
        if (usd) {
            return raw.multiply(USD_TO_KZT).setScale(0, RoundingMode.HALF_UP).max(new BigDecimal("150000"));
        }

        return raw.compareTo(new BigDecimal("150000")) >= 0 ? raw : DEFAULT_BUDGET;
    }

    private BigDecimal extractAiBudgetKzt(JsonNode intentNode, BigDecimal promptBudgetKzt) {
        if (intentNode.path("budgetKzt").isNumber()) {
            return intentNode.path("budgetKzt").decimalValue();
        }
        if (intentNode.path("budgetUsd").isNumber()) {
            return intentNode.path("budgetUsd").decimalValue().multiply(USD_TO_KZT).setScale(0, RoundingMode.HALF_UP);
        }
        return promptBudgetKzt;
    }

    private boolean hasUnlimitedBudgetSignal(String normalizedPrompt) {
        if (normalizedPrompt == null || normalizedPrompt.isBlank()) {
            return false;
        }

        return normalizedPrompt.contains("money is not the issue")
                || normalizedPrompt.contains("money is not issue")
                || normalizedPrompt.contains("no budget")
                || normalizedPrompt.contains("unlimited budget")
                || normalizedPrompt.contains("best build")
                || normalizedPrompt.contains("best performance")
                || normalizedPrompt.contains("max performance")
                || normalizedPrompt.contains("top tier");
    }

    private String inferUseCase(String normalizedPrompt) {
        if (normalizedPrompt.contains("gaming") || normalizedPrompt.contains("game") || normalizedPrompt.contains("play") || normalizedPrompt.contains("fps")) {
            return "gaming";
        }
        if (normalizedPrompt.contains("creator") || normalizedPrompt.contains("editing") || normalizedPrompt.contains("video") || normalizedPrompt.contains("render")) {
            return "creator";
        }
        if (normalizedPrompt.contains("office") || normalizedPrompt.contains("work") || normalizedPrompt.contains("study")) {
            return "office";
        }
        return "mixed";
    }

    private List<String> buildPrioritiesFromPrompt(String normalizedPrompt) {
        if (normalizedPrompt == null) {
            return List.of("value", "balance");
        }
        List<String> priorities = new ArrayList<>();
        if (normalizedPrompt.contains("fps") || normalizedPrompt.contains("gaming") || normalizedPrompt.contains("performance")) {
            priorities.add("fps");
        }
        if (normalizedPrompt.contains("quiet") || normalizedPrompt.contains("silent")) {
            priorities.add("quiet");
        }
        if (normalizedPrompt.contains("rgb") || normalizedPrompt.contains("aesthetic")) {
            priorities.add("aesthetics");
        }
        if (normalizedPrompt.contains("upgrade") || normalizedPrompt.contains("upgradability")) {
            priorities.add("upgradability");
        }
        if (normalizedPrompt.contains("value") || normalizedPrompt.contains("budget") || normalizedPrompt.contains("cheap")) {
            priorities.add("value");
        }
        if (priorities.isEmpty()) {
            return List.of("value", "balance");
        }
        return List.copyOf(priorities);
    }

    private String normalizeUseCase(String aiUseCase, String prompt) {
        String inferred = inferUseCase(prompt == null ? "" : prompt.toLowerCase(Locale.ROOT));
        if (inferred != null && !"mixed".equals(inferred)) {
            return inferred;
        }
        return aiUseCase == null || aiUseCase.isBlank() ? "mixed" : aiUseCase;
    }

    private String extractCpuQuery(String prompt, BuildResponse.IntentDto intent) {
        String fromPrompt = extractPattern(prompt, CPU_QUERY_PATTERN);
        if (fromPrompt != null) {
            return fromPrompt;
        }
        if (intent.constraints() != null && intent.constraints().brandCpu() != null) {
            return intent.constraints().brandCpu();
        }
        return null;
    }

    private String extractGpuQuery(String prompt, BuildResponse.IntentDto intent) {
        String fromPrompt = extractPattern(prompt, GPU_QUERY_PATTERN);
        if (fromPrompt != null) {
            return fromPrompt;
        }
        if (intent.constraints() != null && intent.constraints().brandGpu() != null) {
            return intent.constraints().brandGpu();
        }
        return null;
    }

    private String extractStorageQuery(String prompt) {
        return extractPattern(prompt, STORAGE_QUERY_PATTERN);
    }

    private String extractPattern(String prompt, Pattern pattern) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String inferCpuSocket(String cpuName) {
        String normalized = cpuName == null ? "" : cpuName.toLowerCase(Locale.ROOT);
        if (normalized.contains("am5") || normalized.matches(".*ryzen\\s+[3579]\\s+[789]\\d{3}.*")) {
            return "AM5";
        }
        if (normalized.contains("am4") || normalized.matches(".*ryzen\\s+[3579]\\s+[1-5]\\d{3}.*")) {
            return "AM4";
        }
        if (normalized.matches(".*intel\\s+core\\s+i[3579]-1[2-4]\\d{3}.*") || normalized.contains("lga1700")) {
            return "LGA1700";
        }
        if (normalized.matches(".*intel\\s+core\\s+i[3579]-1[01]\\d{3}.*") || normalized.contains("lga1200")) {
            return "LGA1200";
        }
        if (normalized.matches(".*intel\\s+core\\s+i[3579]-[6-9]\\d{3}.*") || normalized.contains("lga1151")) {
            return "LGA1151";
        }
        return null;
    }

    private String inferSocketFromText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("am5") || normalized.contains("ryzen 7000") || normalized.contains("ryzen 8000")) {
            return "AM5";
        }
        if (normalized.contains("am4") || normalized.contains("ryzen 3000") || normalized.contains("ryzen 5000")) {
            return "AM4";
        }
        if (normalized.contains("lga1700") || normalized.contains("12th gen") || normalized.contains("13th gen") || normalized.contains("14th gen")) {
            return "LGA1700";
        }
        if (normalized.contains("lga1200") || normalized.contains("10th gen") || normalized.contains("11th gen")) {
            return "LGA1200";
        }
        return null;
    }

    private String inferMemoryType(String socket) {
        if (socket == null) {
            return null;
        }
        if (socket.equalsIgnoreCase("AM5") || socket.equalsIgnoreCase("LGA1700")) {
            return "DDR5";
        }
        if (socket.equalsIgnoreCase("AM4") || socket.equalsIgnoreCase("LGA1200") || socket.equalsIgnoreCase("LGA1151")) {
            return "DDR4";
        }
        return null;
    }

    private Integer extractFirstInt(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        Matcher matcher = Pattern.compile("(\\d{2,4})").matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private Integer extractStorageCapacity(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{3,4})\\s*(TB|GB)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            return matcher.group(2).equalsIgnoreCase("TB") ? value * 1000 : value;
        }
        return null;
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal gpuBudgetShare(String useCase) {
        if (useCase == null) {
            return new BigDecimal("0.30");
        }
        return switch (useCase.toLowerCase(Locale.ROOT)) {
            case "gaming" -> new BigDecimal("0.40");
            case "creator" -> new BigDecimal("0.30");
            case "office" -> new BigDecimal("0.10");
            default -> new BigDecimal("0.30");
        };
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String sanitizeJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        return trimmed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9а-яё\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private BuildResponse mockBuildResponse(String sessionId, BuildResponse.IntentDto intent) {
        Map<String, BuildResponse.PartDto> build = new LinkedHashMap<>();
        build.put("cpu", new BuildResponse.PartDto(1L, "Sample CPU", new BigDecimal("199.99"), "AM5", "DDR5", 65, null));
        build.put("motherboard", new BuildResponse.PartDto(2L, "Sample Motherboard", new BigDecimal("149.99"), "AM5", "DDR5", null, null));
        build.put("memory", new BuildResponse.PartDto(3L, "Sample 32GB DDR5", new BigDecimal("99.99"), null, "DDR5", null, null));
        build.put("storage", new BuildResponse.PartDto(4L, "Sample 1TB NVMe", new BigDecimal("79.99"), null, null, null, null));
        build.put("gpu", new BuildResponse.PartDto(5L, "Sample GPU", new BigDecimal("499.99"), null, null, 220, "Sample chipset"));
        build.put("powerSupply", new BuildResponse.PartDto(6L, "Sample 750W PSU", new BigDecimal("99.99"), null, null, 750, null));
        build.put("pcCase", new BuildResponse.PartDto(7L, "Sample Airflow Case", new BigDecimal("89.99"), null, null, null, null));

        return new BuildResponse(
                sessionId,
                intent,
                build,
            Map.of(),
                new BuildResponse.TotalsDto(new BigDecimal("1219.93"), 385, new BigDecimal("47.3")),
                new BuildResponse.ChecksDto(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                List.of(
                        "This is a starter scaffold build response.",
                        "The recommendation engine should now be switched over to parsed scrape tables."
                ),
                List.of(
                        new BuildResponse.AlternativeDto("Lower cost", new BigDecimal("1099.93"), "Cheaper GPU choice."),
                        new BuildResponse.AlternativeDto("Higher FPS", new BigDecimal("1499.93"), "More budget to GPU.")
            ),
            List.of("Market insights unavailable in mock mode.")
        );
    }

    private interface PartMapper {
        BuildResponse.PartDto map(JsonNode node);
    }
}
