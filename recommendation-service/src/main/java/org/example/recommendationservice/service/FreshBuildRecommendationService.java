package org.example.recommendationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.config.OpenAiProperties;
import org.example.recommendationservice.dto.BuildRequest;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.dto.ChatRequest;
import org.example.recommendationservice.dto.RecommendationEvaluationRequest;
import org.example.recommendationservice.dto.RecommendationEvaluationResponse;
import org.example.recommendationservice.model.AiSavedBuild;
import org.example.recommendationservice.model.PartPerformanceMapping;
import org.example.recommendationservice.repository.AiSavedBuildRepository;
import org.example.recommendationservice.repository.PartPerformanceMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FreshBuildRecommendationService {

    private static final BigDecimal DEFAULT_BUDGET = new BigDecimal("500000");
    private static final BigDecimal USD_TO_KZT = new BigDecimal("455");
    private static final String SCORE_VERSION = "v1.0-internal-kz";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{2,7}(?:\\.\\d+)?)");
    private static final Pattern CPU_QUERY_PATTERN = Pattern.compile("(?i)(ryzen\\s+[3579]\\s+\\d{4}x?|ryzen\\s+[3579]\\s+\\d{4}|core\\s+i[3579]-?\\d{4,5}f?|i[3579]-?\\d{4,5}f?)");
    private static final Pattern GPU_QUERY_PATTERN = Pattern.compile("(?i)(rtx\\s*\\d{3,4}(?:\\s*ti)?|rx\\s*\\d{3,4}(?:\\s*xt)?|arc\\s*\\w+)");
        private static final Pattern EXPLICIT_KZT_PATTERN = Pattern.compile("(?i)(\\d{2,3}(?:[\\s,]\\d{3})+|\\d{5,7})\\s*(kzt|тенге|₸)");
        private static final Pattern EXPLICIT_USD_PATTERN = Pattern.compile("(?i)(\\d{2,5}(?:[\\s,]\\d{3})*|\\d{2,5})\\s*(usd|dollar|\\$)");

        private static final Map<String, Integer> CPU_WATTAGE_LOOKUP = Map.ofEntries(
            Map.entry("ryzen 5 7600", 88),
            Map.entry("ryzen 7 7700", 88),
            Map.entry("ryzen 7 7800x3d", 120),
            Map.entry("ryzen 9 7900", 170),
            Map.entry("core i5 13400", 148),
            Map.entry("core i5 13600", 181),
            Map.entry("core i7 13700", 253),
            Map.entry("core i7 14700", 253),
            Map.entry("core i9 14900", 253)
        );

        private static final Map<String, Integer> GPU_WATTAGE_LOOKUP = Map.ofEntries(
            Map.entry("rtx 4060", 115),
            Map.entry("rtx 4060 ti", 160),
            Map.entry("rtx 4070", 200),
            Map.entry("rtx 4070 super", 220),
            Map.entry("rtx 4080", 320),
            Map.entry("rtx 4090", 450),
            Map.entry("rx 7600", 165),
            Map.entry("rx 7700 xt", 245),
            Map.entry("rx 7800 xt", 263),
            Map.entry("rx 7900 xt", 315),
            Map.entry("rx 7900 xtx", 355)
        );

    private static final Map<String, VariantProfile> VARIANTS = Map.of(
            "best_value", new VariantProfile("best_value", new BigDecimal("0.35"), new BigDecimal("0.22"), 0.55, 0.25, 0.20),
            "best_performance", new VariantProfile("best_performance", new BigDecimal("0.45"), new BigDecimal("0.24"), 0.20, 0.60, 0.20),
            "balanced", new VariantProfile("balanced", new BigDecimal("0.40"), new BigDecimal("0.23"), 0.35, 0.40, 0.25)
    );

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final AiSavedBuildRepository aiSavedBuildRepository;
        private final HardwareFallbackResolver hardwareFallbackResolver;
    private final PartPerformanceMappingRepository partPerformanceMappingRepository;

    @Value("${part-service.url}")
    private String partServiceUrl;

    private final RestClient restClient = RestClient.builder().build();

    public BuildResponse createBuild(BuildRequest request) {
        String sessionId = UUID.randomUUID().toString();
        FallbackUsageTracker fallbackUsageTracker = new FallbackUsageTracker();
        return buildFromPrompt(sessionId, request.userId(), request.prompt(), request.currency(), request.region(), Boolean.TRUE.equals(request.strictBudget()), fallbackUsageTracker);
    }

    public BuildResponse applyChat(String sessionId, ChatRequest request) {
        AiSavedBuild existing = aiSavedBuildRepository.findFirstBySessionId(sessionId).orElse(null);
        String userId = existing == null ? null : existing.getUserId();
        String currency = existing == null ? "KZT" : existing.getCurrency();
        String region = existing == null ? "KZ" : existing.getRegion();
        boolean strictBudget = existing != null && existing.isStrictBudget();
        return buildFromPrompt(sessionId, userId, request.message(), currency, region, strictBudget, new FallbackUsageTracker());
    }

    public RecommendationEvaluationResponse evaluate(RecommendationEvaluationRequest request) {
        List<String> prompts = request.prompts() == null ? List.of() : request.prompts().stream()
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .toList();

        if (prompts.isEmpty()) {
            return new RecommendationEvaluationResponse(0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0L, 0, BigDecimal.ZERO, List.of());
        }

        List<RecommendationEvaluationResponse.CaseResultDto> cases = new ArrayList<>();
        int compatibilityPassed = 0;
        int budgetPassed = 0;
        int fallbackCases = 0;
        long totalLatency = 0L;

        for (String prompt : prompts) {
            BuildResponse result = createBuild(new BuildRequest(
                    prompt,
                    request.currency(),
                    request.region(),
                    request.strictBudget(),
                    request.userId()
            ));

            boolean compatibility = Boolean.TRUE.equals(result.checks().compatibilityPassed());
            boolean budget = Boolean.TRUE.equals(result.checks().budgetOk());
            long latency = result.metrics() == null || result.metrics().latencyMs() == null ? 0L : result.metrics().latencyMs();
            boolean fallback = result.warnings() != null
                    && result.warnings().stream().anyMatch(warning -> warning != null && warning.toLowerCase(Locale.ROOT).contains("fallback"));

            if (compatibility) {
                compatibilityPassed++;
            }
            if (budget) {
                budgetPassed++;
            }
            if (fallback) {
                fallbackCases++;
            }
            totalLatency += latency;

            String topVariant = result.top3Builds() == null || result.top3Builds().isEmpty()
                    ? null
                    : result.top3Builds().get(0).label();

            cases.add(new RecommendationEvaluationResponse.CaseResultDto(
                    prompt,
                    compatibility,
                    budget,
                    latency,
                    fallback,
                    result.warnings() == null ? 0 : result.warnings().size(),
                    result.budgetBand(),
                    topVariant
            ));
        }

        int totalCases = cases.size();
        long avgLatency = totalCases == 0 ? 0L : totalLatency / totalCases;

        return new RecommendationEvaluationResponse(
                totalCases,
                compatibilityPassed,
                percent(compatibilityPassed, totalCases),
                budgetPassed,
                percent(budgetPassed, totalCases),
                avgLatency,
                fallbackCases,
                percent(fallbackCases, totalCases),
                List.copyOf(cases)
        );
    }

    private BuildResponse buildFromPrompt(
            String sessionId,
            String userId,
            String prompt,
            String currency,
            String region,
            boolean strictBudget,
            FallbackUsageTracker fallbackUsageTracker
    ) {
        long startedAt = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        BuildResponse.RequirementsDto requirements = extractRequirements(prompt, currency, region, strictBudget);
        BigDecimal budget = requirements.budgetKzt() == null || requirements.budgetKzt().signum() <= 0
                ? DEFAULT_BUDGET
                : requirements.budgetKzt();
        String budgetBand = resolveBudgetBand(budget);

        try {
            ComponentPools pools = loadComponentPools(prompt, requirements, warnings, fallbackUsageTracker);
            List<VariantCandidate> builtVariants = new ArrayList<>();
            int candidateBuildsEvaluated = 0;

            for (String label : List.of("best_value", "best_performance", "balanced")) {
                VariantProfile profile = VARIANTS.get(label);
                VariantCandidate candidate = buildVariant(profile, pools, requirements, budget, strictBudget);
                if (candidate != null) {
                    builtVariants.add(candidate);
                    candidateBuildsEvaluated += candidate.evaluatedCount;
                }
            }

            if (builtVariants.isEmpty()) {
                warnings.add(diagnoseNoCandidateReason(pools, requirements, budget, strictBudget));
                BuildResponse fallback = mockBuildResponse(sessionId, requirements, budgetBand, warnings, startedAt);
                saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, requirements, fallback);
                return fallback;
            }

            List<BuildResponse.BuildVariantDto> top3 = builtVariants.stream()
                    .map(VariantCandidate::toVariantDto)
                    .toList();

            List<String> explanations = builtVariants.stream()
                    .map(this::buildExplanation)
                    .toList();

            int compatibleBuilds = (int) builtVariants.stream().filter(candidate -> Boolean.TRUE.equals(candidate.checks.compatibilityPassed())).count();
            BuildResponse.ChecksDto aggregateChecks = aggregateChecks(builtVariants, pools.stockValidationTrusted);
            BigDecimal mappingCoverage = performanceMappingCoverage(top3);
            int effectiveFallbackCount = effectiveFallbackCount(top3);

            if (!Boolean.TRUE.equals(aggregateChecks.memoryCompatible())) {
                warnings.add("Motherboard memory type is missing or incompatible; compatibility confidence is reduced.");
            }
            if (!Boolean.TRUE.equals(aggregateChecks.socketCompatible())) {
                warnings.add("CPU and motherboard socket compatibility could not be fully validated from normalized data.");
            }
            if (!Boolean.TRUE.equals(aggregateChecks.caseFitValidated())) {
                warnings.add("Case fit validation is not enforced yet: normalized dimensions are missing for strict fit checks.");
            }
            if (!Boolean.TRUE.equals(aggregateChecks.stockValidationEnforced())) {
                warnings.add("Stock validation is not enforceable: parsed stock metadata is missing.");
            }
            if ("low".equalsIgnoreCase(aggregateChecks.powerEstimateConfidence())) {
                warnings.add("Power estimate confidence is low for one or more parts: normalized TDP values are missing.");
            }

            String normalizedDataConfidence = normalizedDataConfidence(mappingCoverage, effectiveFallbackCount);
            if ("low".equals(normalizedDataConfidence)) {
                warnings.add("Normalized data confidence is low: fallback inference was required for multiple parts.");
            }

            BuildResponse response = new BuildResponse(
                    sessionId,
                    requirements,
                    budgetBand,
                    top3,
                    explanations,
                    aggregateChecks,
                    new BuildResponse.MetricsDto(
                            System.currentTimeMillis() - startedAt,
                            candidateBuildsEvaluated,
                            compatibleBuilds,
                            SCORE_VERSION,
                            mappingCoverage,
                                effectiveFallbackCount,
                            normalizedDataConfidence
                    ),
                    List.copyOf(warnings)
            );

            saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, requirements, response);
            return response;
        } catch (Exception exception) {
            warnings.add("Fell back to mock recommendation due to unexpected error: " + exception.getClass().getSimpleName());
            BuildResponse fallback = mockBuildResponse(sessionId, requirements, budgetBand, warnings, startedAt);
            saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, requirements, fallback);
            return fallback;
        }
    }

    private ComponentPools loadComponentPools(String prompt, BuildResponse.RequirementsDto requirements, List<String> warnings, FallbackUsageTracker fallbackUsageTracker) {
        List<BuildResponse.PartDto> cpuParts = mergePartPools(
            fetchParts("/api/parsed/cpu", 500, node -> toCpuPart(node, fallbackUsageTracker)),
            fetchParts("/api/cpu", 500, node -> toCpuDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> gpuParts = mergePartPools(
            fetchParts("/api/parsed/video-card", 500, node -> toGpuPart(node, fallbackUsageTracker)),
            fetchParts("/api/gpu", 500, node -> toGpuDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> motherboardParts = mergePartPools(
            fetchParts("/api/parsed/motherboard", 500, node -> toMotherboardPart(node, fallbackUsageTracker)),
            fetchParts("/api/motherboard", 500, node -> toMotherboardDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> memoryParts = mergePartPools(
            fetchParts("/api/parsed/memory", 500, node -> toMemoryPart(node, fallbackUsageTracker)),
            fetchParts("/api/memory", 500, node -> toMemoryDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> storageParts = mergePartPools(
            fetchParts("/api/parsed/internal-hard-drive", 500, node -> toStoragePart(node, fallbackUsageTracker)),
            fetchParts("/api/storage", 500, node -> toStorageDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> psuParts = mergePartPools(
            fetchParts("/api/parsed/power-supply", 500, node -> toPsuPart(node, fallbackUsageTracker)),
            fetchParts("/api/psu", 500, node -> toPsuDbPart(node, fallbackUsageTracker))
        );
        List<BuildResponse.PartDto> pcCaseParts = mergePartPools(
            fetchParts("/api/parsed/pc-case", 500, node -> toCasePart(node, fallbackUsageTracker)),
            fetchParts("/api/case", 500, node -> toCaseDbPart(node, fallbackUsageTracker))
        );

        StockFilterResult cpu = applyStockPolicy(cpuParts, warnings, "cpu", requirements.strictStockOnly());
        StockFilterResult gpu = applyStockPolicy(gpuParts, warnings, "gpu", requirements.strictStockOnly());
        StockFilterResult motherboard = applyStockPolicy(motherboardParts, warnings, "motherboard", requirements.strictStockOnly());
        StockFilterResult memory = applyStockPolicy(memoryParts, warnings, "memory", requirements.strictStockOnly());
        StockFilterResult storage = applyStockPolicy(storageParts, warnings, "storage", requirements.strictStockOnly());
        StockFilterResult psu = applyStockPolicy(psuParts, warnings, "powerSupply", requirements.strictStockOnly());
        StockFilterResult pcCase = applyStockPolicy(pcCaseParts, warnings, "pcCase", requirements.strictStockOnly());

        String cpuQuery = hardwareFallbackResolver.extractCpuQuery(prompt);
        String gpuQuery = hardwareFallbackResolver.extractGpuQuery(prompt);

        List<BuildResponse.PartDto> filteredCpus = applyQueryAndBrandFilter(cpu.parts, cpuQuery, requirements.constraints() == null ? null : requirements.constraints().brandCpu());
        List<BuildResponse.PartDto> filteredGpus = applyQueryAndBrandFilter(gpu.parts, gpuQuery, requirements.constraints() == null ? null : requirements.constraints().brandGpu());

        boolean stockTrusted = cpu.trusted && gpu.trusted && motherboard.trusted && memory.trusted && storage.trusted && psu.trusted && pcCase.trusted;
        if (Boolean.TRUE.equals(requirements.strictStockOnly()) && !stockTrusted) {
            warnings.add("Stock metadata is unavailable in the parsed catalog, so strict in-stock filtering could not be enforced.");
        }

        return new ComponentPools(
                filteredCpus.isEmpty() ? cpu.parts : filteredCpus,
                filteredGpus.isEmpty() ? gpu.parts : filteredGpus,
                motherboard.parts,
                memory.parts,
                storage.parts,
                psu.parts,
                pcCase.parts,
            stockTrusted,
            fallbackUsageTracker.count()
        );
    }

    private VariantCandidate buildVariant(
            VariantProfile profile,
            ComponentPools pools,
            BuildResponse.RequirementsDto requirements,
            BigDecimal budget,
            boolean strictBudget
    ) {
        List<BuildResponse.PartDto> rankedCpu = rankParts(pools.cpus, "cpu", requirements, budget.multiply(profile.cpuShare), profile);
        List<BuildResponse.PartDto> rankedGpu = rankParts(pools.gpus, "gpu", requirements, budget.multiply(profile.gpuShare), profile);

        int evaluated = 0;
        VariantCandidate best = null;

        for (BuildResponse.PartDto cpu : rankedCpu.stream().limit(8).toList()) {
            String cpuSocket = cpu.socket();
            Tier cpuTier = tierOf(cpu);

            List<BuildResponse.PartDto> motherboardCandidates = pools.motherboards.stream()
                    .filter(motherboard -> compatibleSocket(cpuSocket, motherboard.socket()))
                    .sorted(Comparator.comparing((BuildResponse.PartDto part) -> partScore(part, "motherboard", requirements, budget.multiply(new BigDecimal("0.12")), profile)).reversed())
                    .limit(6)
                    .toList();

            if (motherboardCandidates.isEmpty()) {
                continue;
            }

            for (BuildResponse.PartDto gpu : rankedGpu.stream().limit(10).toList()) {
                Tier gpuTier = tierOf(gpu);
                if (!cpuGpuPairingAllowed(cpuTier, gpuTier, requirements.useCase())) {
                    continue;
                }

                for (BuildResponse.PartDto motherboard : motherboardCandidates.stream().limit(3).toList()) {
                    String expectedMemoryType = normalizeDdrLabel(motherboard.memoryType());
                    List<BuildResponse.PartDto> memoryCandidates = pools.memories.stream()
                            .filter(memory -> memory != null && memory.memoryType() != null && !memory.memoryType().isBlank())
                            .filter(memory -> {
                                if (expectedMemoryType == null || expectedMemoryType.isBlank()) {
                                    return true;
                                }
                                String actualMemoryType = normalizeDdrLabel(memory.memoryType());
                                return actualMemoryType != null && expectedMemoryType.equalsIgnoreCase(actualMemoryType);
                            })
                            .sorted(Comparator.comparing((BuildResponse.PartDto part) -> partScore(part, "memory", requirements, budget.multiply(new BigDecimal("0.12")), profile)).reversed())
                            .limit(6)
                            .toList();

                    if (memoryCandidates.isEmpty()) {
                        continue;
                    }

                    BuildResponse.PartDto memory = memoryCandidates.get(0);
                    BuildResponse.PartDto storage = rankParts(pools.storages, "storage", requirements, budget.multiply(new BigDecimal("0.10")), profile)
                            .stream()
                            .findFirst()
                            .orElse(null);
                    BuildResponse.PartDto pcCase = rankParts(pools.cases, "pcCase", requirements, budget.multiply(new BigDecimal("0.07")), profile)
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (storage == null || pcCase == null) {
                        continue;
                    }

                    int estimatedPower = estimatePower(cpu, gpu);
                    int psuMinW = psuMinimumWattage(estimatedPower);
                    BuildResponse.PartDto psu = pools.psus.stream()
                            .filter(item -> safeInt(item.wattage(), 0) >= psuMinW)
                            .sorted(Comparator.comparing((BuildResponse.PartDto part) -> partScore(part, "powerSupply", requirements, budget.multiply(new BigDecimal("0.09")), profile)).reversed())
                            .findFirst()
                            .orElse(null);

                    if (psu == null) {
                        continue;
                    }

                    Map<String, BuildResponse.PartDto> parts = new LinkedHashMap<>();
                    parts.put("cpu", cpu);
                    parts.put("gpu", gpu);
                    parts.put("motherboard", motherboard);
                    parts.put("memory", memory);
                    parts.put("storage", storage);
                    parts.put("powerSupply", psu);
                    parts.put("pcCase", pcCase);

                    BigDecimal total = totalPrice(parts);
                    if (strictBudget && total.compareTo(budget) > 0) {
                        continue;
                    }

                    BuildResponse.ChecksDto checks = evaluateChecks(parts, budget, requirements, strictBudget, true);
                    if (!Boolean.TRUE.equals(checks.compatibilityPassed())) {
                        continue;
                    }

                    BigDecimal score = buildCompositeScore(parts, total, budget, profile);
                    List<String> tradeoffs = tradeoffs(profile.label);
                    BuildResponse.TotalsDto totals = buildTotals(parts);

                    VariantCandidate candidate = new VariantCandidate(profile.label, parts, totals, score, tradeoffs, checks, 1);
                    evaluated += 1;

                    if (best == null || candidate.score.compareTo(best.score) > 0) {
                        best = candidate;
                    }
                }
            }
        }

        if (best == null) {
            return null;
        }

        return new VariantCandidate(best.label, best.parts, best.totals, best.score, best.tradeoffs, best.checks, evaluated);
    }

    private BuildResponse.ChecksDto evaluateChecks(
            Map<String, BuildResponse.PartDto> parts,
            BigDecimal budget,
            BuildResponse.RequirementsDto requirements,
            boolean strictBudget,
            boolean stockTrusted
    ) {
        BuildResponse.PartDto cpu = parts.get("cpu");
        BuildResponse.PartDto gpu = parts.get("gpu");
        BuildResponse.PartDto motherboard = parts.get("motherboard");
        BuildResponse.PartDto memory = parts.get("memory");
        BuildResponse.PartDto psu = parts.get("powerSupply");

        boolean socketCompatible = compatibleSocket(cpu == null ? null : cpu.socket(), motherboard == null ? null : motherboard.socket());
        String expectedMemoryType = motherboard == null ? null : normalizeDdrLabel(motherboard.memoryType());
        String actualMemoryType = memory == null ? null : normalizeDdrLabel(memory.memoryType());
        boolean memoryCompatible = memory != null
            && (expectedMemoryType == null
                || (actualMemoryType != null && expectedMemoryType.equalsIgnoreCase(actualMemoryType)));

        int estimatedPower = estimatePower(cpu, gpu);
        int psuWatts = safeInt(psu == null ? null : psu.wattage(), 0);
        boolean psuMinHeadroomOk = psuWatts >= psuMinimumWattage(estimatedPower);
        boolean psuPreferredHeadroomOk = psuWatts >= psuPreferredWattage(estimatedPower);

        BigDecimal total = totalPrice(parts);
        boolean budgetOk = !strictBudget || total.compareTo(budget) <= 0;
        boolean cpuGpuBalanceOk = cpuGpuPairingAllowed(tierOf(cpu), tierOf(gpu), requirements.useCase());
        boolean caseFitValidated = false;
        boolean stockValidationEnforced = stockTrusted;
        String powerEstimateConfidence = powerEstimateConfidence(cpu, gpu);

        boolean compatibilityPassed = socketCompatible
                && memoryCompatible
                && psuMinHeadroomOk
                && budgetOk
                && cpuGpuBalanceOk;

        return new BuildResponse.ChecksDto(
                compatibilityPassed,
                socketCompatible,
                memoryCompatible,
                psuMinHeadroomOk,
                psuPreferredHeadroomOk,
                budgetOk,
                cpuGpuBalanceOk,
                stockTrusted,
                stockValidationEnforced,
                caseFitValidated,
                powerEstimateConfidence
        );
    }

    private BuildResponse.ChecksDto aggregateChecks(List<VariantCandidate> builtVariants, boolean stockTrusted) {
        boolean compatibilityPassed = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.compatibilityPassed()));
        boolean socketCompatible = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.socketCompatible()));
        boolean memoryCompatible = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.memoryCompatible()));
        boolean psuMinimum = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.psuMinimumHeadroomPassed()));
        boolean psuPreferred = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.psuPreferredHeadroomPassed()));
        boolean budgetOk = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.budgetOk()));
        boolean cpuGpuBalanceOk = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.cpuGpuBalanceOk()));
        boolean stockValidationEnforced = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.stockValidationEnforced()));
        boolean caseFitValidated = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.caseFitValidated()));
        String powerEstimateConfidence = builtVariants.stream().allMatch(candidate -> "medium".equalsIgnoreCase(candidate.checks.powerEstimateConfidence()))
            ? "medium"
            : "low";

        return new BuildResponse.ChecksDto(
                compatibilityPassed,
                socketCompatible,
                memoryCompatible,
                psuMinimum,
                psuPreferred,
                budgetOk,
                cpuGpuBalanceOk,
                stockTrusted,
                stockValidationEnforced,
                caseFitValidated,
                powerEstimateConfidence
        );
    }

    private String diagnoseNoCandidateReason(ComponentPools pools, BuildResponse.RequirementsDto requirements, BigDecimal budget, boolean strictBudget) {
        if (pools.cpus == null || pools.cpus.isEmpty()) {
            return "No CPU candidates remained after prompt and catalog filtering.";
        }
        if (pools.gpus == null || pools.gpus.isEmpty()) {
            return "No GPU candidates remained after prompt and catalog filtering.";
        }

        boolean hasSocketMatch = pools.cpus.stream()
                .filter(cpu -> cpu != null && cpu.socket() != null)
                .anyMatch(cpu -> pools.motherboards.stream().anyMatch(motherboard -> compatibleSocket(cpu.socket(), motherboard.socket())));
        if (!hasSocketMatch) {
            return "No motherboard matched the available CPU socket values.";
        }

        boolean hasBoardWithKnownMemoryType = pools.motherboards.stream()
            .map(BuildResponse.PartDto::memoryType)
            .map(this::normalizeDdrLabel)
            .anyMatch(memoryType -> memoryType != null && !memoryType.isBlank());
        if (hasBoardWithKnownMemoryType) {
            boolean hasMemoryMatch = pools.motherboards.stream()
                .map(BuildResponse.PartDto::memoryType)
                .map(this::normalizeDdrLabel)
                .filter(memoryType -> memoryType != null && !memoryType.isBlank())
                .anyMatch(memoryType -> pools.memories.stream()
                    .map(BuildResponse.PartDto::memoryType)
                    .map(this::normalizeDdrLabel)
                    .anyMatch(memoryType::equalsIgnoreCase));
            if (!hasMemoryMatch) {
            return "No memory kit matched the motherboard memoryType values.";
            }
        }

        boolean hasPsuHeadroom = pools.cpus.stream().anyMatch(cpu -> pools.gpus.stream().anyMatch(gpu -> {
            int estimatedPower = estimatePower(cpu, gpu);
            int psuMinW = psuMinimumWattage(estimatedPower);
            return pools.psus.stream().anyMatch(psu -> safeInt(psu.wattage(), 0) >= psuMinW);
        }));
        if (!hasPsuHeadroom) {
            return "No PSU in the catalog met the minimum power headroom requirement.";
        }

        if (strictBudget) {
            BigDecimal cheapestCandidateFloor = pools.cpus.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO)
                    .add(pools.gpus.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO))
                    .add(pools.motherboards.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO))
                    .add(pools.memories.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO))
                    .add(pools.storages.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO))
                    .add(pools.psus.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO))
                    .add(pools.cases.stream().findFirst().map(BuildResponse.PartDto::priceKzt).orElse(BigDecimal.ZERO));
            if (cheapestCandidateFloor.compareTo(budget) > 0) {
                return "Strict budget eliminated all candidates before scoring.";
            }
        }

        return "All candidate branches were filtered out by compatibility or budget checks before scoring.";
    }

    private String normalizedDataConfidence(BigDecimal mappingCoverage, int fallbackInferenceCount) {
        if (mappingCoverage == null) {
            return fallbackInferenceCount <= 0 ? "medium" : "low";
        }
        if (fallbackInferenceCount <= 0 && mappingCoverage.compareTo(new BigDecimal("80")) >= 0) {
            return "high";
        }
        if (fallbackInferenceCount <= 3 && mappingCoverage.compareTo(new BigDecimal("50")) >= 0) {
            return "medium";
        }
        return "low";
    }

    private String buildExplanation(VariantCandidate candidate) {
        BuildResponse.PartDto cpu = candidate.parts.get("cpu");
        BuildResponse.PartDto gpu = candidate.parts.get("gpu");
        BigDecimal total = candidate.totals.partsTotalKzt();

        return candidate.label + ": "
                + "CPU/GPU pairing "
                + safeName(cpu)
                + " + "
                + safeName(gpu)
                + " passed compatibility and tier balance checks; "
                + "total "
                + total.setScale(0, RoundingMode.HALF_UP)
                + " KZT with PSU headroom "
                + candidate.totals.psuHeadroomPercent()
                + "%.";
    }

    private List<BuildResponse.PartDto> rankParts(
            List<BuildResponse.PartDto> parts,
            String component,
            BuildResponse.RequirementsDto requirements,
            BigDecimal targetBudget,
            VariantProfile profile
    ) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }

        List<BuildResponse.PartDto> priced = parts.stream()
            .filter(part -> part != null && part.priceKzt() != null && part.priceKzt().signum() > 0)
            .toList();

        List<BuildResponse.PartDto> source = priced.isEmpty() ? parts : priced;

        return source.stream()
                .sorted(Comparator.comparing((BuildResponse.PartDto part) -> partScore(part, component, requirements, targetBudget, profile)).reversed())
                .toList();
    }

    private double partScore(
            BuildResponse.PartDto part,
            String component,
            BuildResponse.RequirementsDto requirements,
            BigDecimal targetBudget,
            VariantProfile profile
    ) {
        if (part == null) {
            return 0.0;
        }

        double value = priceFitScore(part.priceKzt(), targetBudget);
        double performance = part.performanceScore() == null ? 0 : part.performanceScore();
        double upgrade = upgradePathScore(part, component, requirements);

        return (profile.valueWeight * value)
                + (profile.performanceWeight * performance)
                + (profile.upgradeWeight * upgrade);
    }

    private double priceFitScore(BigDecimal price, BigDecimal target) {
        if (price == null || target == null || target.signum() <= 0) {
            return 50.0;
        }
        BigDecimal delta = price.subtract(target).abs();
        BigDecimal ratio = delta.divide(target, 4, RoundingMode.HALF_UP);
        double penalty = ratio.doubleValue() * 80.0;
        return Math.max(0.0, 100.0 - penalty);
    }

    private double upgradePathScore(BuildResponse.PartDto part, String component, BuildResponse.RequirementsDto requirements) {
        if (part == null) {
            return 0.0;
        }

        double score = 40.0;
        if ("cpu".equals(component) || "motherboard".equals(component)) {
            String socket = part.socket();
            if ("AM5".equalsIgnoreCase(socket)) {
                score += 40.0;
            } else if ("AM4".equalsIgnoreCase(socket)) {
                score += 15.0;
            }
        }

        if ("powerSupply".equals(component) && safeInt(part.wattage(), 0) >= 750) {
            score += 20.0;
        }

        if (requirements != null && requirements.priorities() != null
                && requirements.priorities().stream().map(String::toLowerCase).anyMatch(priority -> priority.contains("upgrade"))) {
            score += 15.0;
        }

        return Math.min(score, 100.0);
    }

    private boolean compatibleSocket(String cpuSocket, String motherboardSocket) {
        if (cpuSocket == null || motherboardSocket == null) {
            return false;
        }
        return cpuSocket.equalsIgnoreCase(motherboardSocket);
    }

    private boolean cpuGpuPairingAllowed(Tier cpuTier, Tier gpuTier, String useCase) {
        if (cpuTier == null || gpuTier == null) {
            return false;
        }

        if ("gaming".equalsIgnoreCase(useCase)) {
            Map<Tier, Set<Tier>> allowedCpuByGpu = new EnumMap<>(Tier.class);
            allowedCpuByGpu.put(Tier.ENTRY, Set.of(Tier.ENTRY, Tier.LOW_MID, Tier.MID));
            allowedCpuByGpu.put(Tier.LOW_MID, Set.of(Tier.ENTRY, Tier.LOW_MID, Tier.MID));
            allowedCpuByGpu.put(Tier.MID, Set.of(Tier.LOW_MID, Tier.MID, Tier.MID_HIGH));
            allowedCpuByGpu.put(Tier.MID_HIGH, Set.of(Tier.MID, Tier.MID_HIGH, Tier.HIGH));
            allowedCpuByGpu.put(Tier.HIGH, Set.of(Tier.MID_HIGH, Tier.HIGH, Tier.ENTHUSIAST));
            allowedCpuByGpu.put(Tier.ENTHUSIAST, Set.of(Tier.HIGH, Tier.ENTHUSIAST));
            return allowedCpuByGpu.getOrDefault(gpuTier, Set.of()).contains(cpuTier);
        }

        int delta = Math.abs(cpuTier.ordinal() - gpuTier.ordinal());
        return delta <= 2;
    }

    private int effectiveFallbackCount(List<BuildResponse.BuildVariantDto> top3) {
        if (top3 == null || top3.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (BuildResponse.BuildVariantDto variant : top3) {
            if (variant == null || variant.parts() == null) {
                continue;
            }
            count += countVariantFallbackSignals(variant.parts());
        }
        return count;
    }

    private int countVariantFallbackSignals(Map<String, BuildResponse.PartDto> parts) {
        int count = 0;

        BuildResponse.PartDto cpu = parts.get("cpu");
        if (cpu != null) {
            if (cpu.socket() == null || cpu.socket().isBlank()) count++;
            if (cpu.wattage() == null || cpu.wattage() <= 0) count++;
        }

        BuildResponse.PartDto gpu = parts.get("gpu");
        if (gpu != null && (gpu.wattage() == null || gpu.wattage() <= 0)) {
            count++;
        }

        BuildResponse.PartDto motherboard = parts.get("motherboard");
        if (motherboard != null) {
            if (motherboard.socket() == null || motherboard.socket().isBlank()) count++;
            if (normalizeDdrLabel(motherboard.memoryType()) == null) count++;
        }

        BuildResponse.PartDto memory = parts.get("memory");
        if (memory != null && normalizeDdrLabel(memory.memoryType()) == null) {
            count++;
        }

        BuildResponse.PartDto psu = parts.get("powerSupply");
        if (psu != null && (psu.wattage() == null || psu.wattage() <= 0)) {
            count++;
        }

        return count;
    }

    private int estimatePower(BuildResponse.PartDto cpu, BuildResponse.PartDto gpu) {
        int cpuPower = safeInt(cpu == null ? null : cpu.wattage(), 95);
        int gpuPower = safeInt(gpu == null ? null : gpu.wattage(), 220);
        return cpuPower + gpuPower + 120;
    }

    private int psuMinimumWattage(int estimatedPower) {
        return BigDecimal.valueOf(estimatedPower)
                .multiply(new BigDecimal("1.20"))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private int psuPreferredWattage(int estimatedPower) {
        return BigDecimal.valueOf(estimatedPower)
                .multiply(new BigDecimal("1.30"))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private BuildResponse.TotalsDto buildTotals(Map<String, BuildResponse.PartDto> parts) {
        int estimatedPower = estimatePower(parts.get("cpu"), parts.get("gpu"));
        int psuWattage = safeInt(parts.get("powerSupply") == null ? null : parts.get("powerSupply").wattage(), 0);

        BigDecimal headroom = BigDecimal.ZERO;
        if (estimatedPower > 0) {
            headroom = BigDecimal.valueOf(psuWattage - estimatedPower)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(estimatedPower), 1, RoundingMode.HALF_UP);
        }

        return new BuildResponse.TotalsDto(totalPrice(parts), estimatedPower, headroom);
    }

    private BigDecimal buildCompositeScore(
            Map<String, BuildResponse.PartDto> parts,
            BigDecimal total,
            BigDecimal budget,
            VariantProfile profile
    ) {
        BuildResponse.PartDto cpu = parts.get("cpu");
        BuildResponse.PartDto gpu = parts.get("gpu");

        double value = priceFitScore(total, budget);
        double performance = ((safePerformance(cpu) + safePerformance(gpu)) / 2.0);
        double balance = 100.0 - (Math.abs(tierOf(cpu).ordinal() - tierOf(gpu).ordinal()) * 20.0);

        double score = (profile.valueWeight * value)
                + (profile.performanceWeight * performance)
                + (profile.upgradeWeight * Math.max(0.0, balance));

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> tradeoffs(String label) {
        if ("best_value".equals(label)) {
            return List.of("Prioritizes price efficiency", "May use lower GPU tier than performance build");
        }
        if ("best_performance".equals(label)) {
            return List.of("Prioritizes higher FPS", "Can approach budget ceiling");
        }
        return List.of("Balanced spend across CPU/GPU", "Good baseline for upgrades");
    }

    private BuildResponse.RequirementsDto extractRequirements(String prompt, String currency, String region, Boolean strictBudget) {
        try {
            if (openAiProperties.apiKey() == null || openAiProperties.apiKey().isBlank()) {
                return requirementsFromPrompt(prompt, strictBudget);
            }

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", openAiProperties.model(),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "Extract PC build requirements as strict JSON. Output JSON only with keys: budgetKzt,useCase,targetResolution,priorities,constraints. constraints keys: brandCpu,brandGpu,rgb,caseSize,wifiRequired."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", "Prompt: " + prompt + "\\nCurrency: " + currency + "\\nRegion: " + region + "\\nStrictBudget: " + strictBudget
                            )
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
            BigDecimal aiBudget = extractAiBudgetKzt(intentNode, promptBudget);

            return new BuildResponse.RequirementsDto(
                    aiBudget.max(promptBudget),
                    normalizeUseCase(intentNode.path("useCase").asText("mixed"), prompt),
                    normalizeResolution(intentNode.path("targetResolution").asText("1080p"), prompt),
                    objectMapper.convertValue(intentNode.path("priorities"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)),
                    new BuildResponse.ConstraintsDto(
                            textOrNull(intentNode.path("constraints").path("brandCpu")),
                            textOrNull(intentNode.path("constraints").path("brandGpu")),
                            intentNode.path("constraints").path("rgb").asBoolean(false),
                            textOrNull(intentNode.path("constraints").path("caseSize")),
                            intentNode.path("constraints").path("wifiRequired").asBoolean(false)
                    ),
                    region == null || region.isBlank() ? "KZ" : region,
                    Boolean.TRUE.equals(strictBudget),
                    true
            );
        } catch (Exception exception) {
            return requirementsFromPrompt(prompt, strictBudget);
        }
    }

    private BuildResponse.RequirementsDto requirementsFromPrompt(String prompt, Boolean strictBudget) {
        String normalized = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        return new BuildResponse.RequirementsDto(
                extractBudgetFromPrompt(prompt),
                inferUseCase(normalized),
                normalizeResolution(null, prompt),
                buildPrioritiesFromPrompt(normalized),
                new BuildResponse.ConstraintsDto(null, null, false, null, false),
                "KZ",
                Boolean.TRUE.equals(strictBudget),
                true
        );
    }

    private void saveSnapshot(
            String sessionId,
            String userId,
            String prompt,
            String currency,
            String region,
            boolean strictBudget,
            BuildResponse.RequirementsDto requirements,
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
            snapshot.setIntentJson(objectMapper.writeValueAsString(requirements));
            snapshot.setBuildJson(objectMapper.writeValueAsString(response.top3Builds()));
            snapshot.setTotalsJson(objectMapper.writeValueAsString(response.metrics()));
            snapshot.setChecksJson(objectMapper.writeValueAsString(response.checks()));
            snapshot.setReasoningJson(objectMapper.writeValueAsString(response.explanations()));
            snapshot.setAlternativesJson(objectMapper.writeValueAsString(response.warnings()));
            aiSavedBuildRepository.save(snapshot);
        } catch (Exception ignored) {
            // Snapshot persistence failure should not fail recommendation response.
        }
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
            return deduplicateByName(parts);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<BuildResponse.PartDto> mergePartPools(List<BuildResponse.PartDto> primary, List<BuildResponse.PartDto> secondary) {
        Map<String, BuildResponse.PartDto> byName = new LinkedHashMap<>();
        for (BuildResponse.PartDto part : concatenate(primary, secondary)) {
            if (part == null) {
                continue;
            }
            String key = normalize(part.name());
            BuildResponse.PartDto existing = byName.get(key);
            if (existing == null || isBetterCandidate(part, existing)) {
                byName.put(key, part);
            }
        }
        return List.copyOf(byName.values());
    }

    private List<BuildResponse.PartDto> concatenate(List<BuildResponse.PartDto> first, List<BuildResponse.PartDto> second) {
        List<BuildResponse.PartDto> all = new ArrayList<>();
        if (first != null) {
            all.addAll(first);
        }
        if (second != null) {
            all.addAll(second);
        }
        return all;
    }

    private boolean isBetterCandidate(BuildResponse.PartDto candidate, BuildResponse.PartDto existing) {
        int candidateScore = partCompletenessScore(candidate);
        int existingScore = partCompletenessScore(existing);
        if (candidateScore != existingScore) {
            return candidateScore > existingScore;
        }
        return safePrice(candidate).compareTo(safePrice(existing)) < 0;
    }

    private int partCompletenessScore(BuildResponse.PartDto part) {
        if (part == null) {
            return 0;
        }
        int score = 0;
        if (part.id() != null) score++;
        if (part.name() != null && !part.name().isBlank()) score++;
        if (part.priceKzt() != null && part.priceKzt().signum() > 0) score++;
        if (part.socket() != null && !part.socket().isBlank()) score++;
        if (part.memoryType() != null && !part.memoryType().isBlank()) score++;
        if (part.wattage() != null && part.wattage() > 0) score++;
        if (part.chipset() != null && !part.chipset().isBlank()) score++;
        if (part.performanceScore() != null && part.performanceScore() > 0) score++;
        if (part.tierLabel() != null && !part.tierLabel().isBlank()) score++;
        return score;
    }

    private List<BuildResponse.PartDto> deduplicateByName(List<BuildResponse.PartDto> parts) {
        Map<String, BuildResponse.PartDto> byName = new LinkedHashMap<>();
        for (BuildResponse.PartDto part : parts) {
            String key = normalize(part.name());
            BuildResponse.PartDto existing = byName.get(key);
            if (existing == null || safePrice(part).compareTo(safePrice(existing)) < 0) {
                byName.put(key, part);
            }
        }
        return List.copyOf(byName.values());
    }

    private List<BuildResponse.PartDto> applyQueryAndBrandFilter(List<BuildResponse.PartDto> parts, String query, String brand) {
        return parts.stream()
                .filter(part -> matchesQuery(part.name(), query))
                .filter(part -> brand == null || brand.isBlank() || normalize(part.name()).contains(normalize(brand)))
                .toList();
    }

    private StockFilterResult applyStockPolicy(
            List<BuildResponse.PartDto> parts,
            List<String> warnings,
            String component,
            Boolean strictStockOnly
    ) {
        if (parts.isEmpty() || !Boolean.TRUE.equals(strictStockOnly)) {
            return new StockFilterResult(parts, true);
        }

        boolean hasExplicitStock = parts.stream()
                .anyMatch(part -> part.stockStatus() != null && !"unknown".equalsIgnoreCase(part.stockStatus()));

        if (!hasExplicitStock) {
            return new StockFilterResult(parts, false);
        }

        List<BuildResponse.PartDto> inStock = parts.stream()
                .filter(part -> "in_stock".equalsIgnoreCase(part.stockStatus()))
                .toList();
        return new StockFilterResult(inStock, true);
    }

    private BuildResponse.PartDto toCpuPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("CPU");
        HardwareFallbackResolver.ResolvedValue<String> socketResolution = hardwareFallbackResolver.resolveCpuSocket(readText(node, "socket", "cpuSocket"), name);
        if (socketResolution.fallbackUsed()) {
            tracker.increment();
        }
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveCpuTierLabel(name);
        if (tierResolution.fallbackUsed()) {
            tracker.increment();
        }
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "cpu", inferredTier, tracker);
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = hardwareFallbackResolver.resolveCpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) {
            tracker.increment();
        }
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                socketResolution.value(),
                null,
                wattageResolution.value(),
                null,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toGpuPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("GPU");
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveGpuTierLabel(name);
        if (tierResolution.fallbackUsed()) {
            tracker.increment();
        }
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "gpu", inferredTier, tracker);
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = hardwareFallbackResolver.resolveGpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) {
            tracker.increment();
        }
        String chipset = readText(node, "chipset", "gpuChipset");
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
                wattageResolution.value(),
                chipset,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toMotherboardPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Motherboard");
        String socket = readText(node, "socket", "mbSocket", "cpuSocket");
        String memoryType = resolveMotherboardMemoryType(node, name, tracker);
        if (socket == null || socket.isBlank() || memoryType == null || memoryType.isBlank()) {
            tracker.increment();
        }
        String chipset = readText(node, "chipset", "mbChipset");
        Long partId = node.path("id").asLong();
        Tier inferredTier = Tier.MID;
        ScoreTier scoreTier = resolveScoreTier(partId, "motherboard", inferredTier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                socket,
                memoryType,
                null,
                chipset,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toMemoryPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Memory");
        String memoryType = readText(node, "ddrType", "memoryType", "ramType");
        if (memoryType == null || memoryType.isBlank()) {
            HardwareFallbackResolver.ResolvedValue<String> memoryResolution = hardwareFallbackResolver.resolveMemoryTypeFromName(name);
            memoryType = memoryResolution.value();
            if (memoryResolution.fallbackUsed()) {
                tracker.increment();
            }
        }
        Tier tier = inferMemoryTier(name);
        Long partId = node.path("id").asLong();
        ScoreTier scoreTier = resolveScoreTier(partId, "memory", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                memoryType,
                null,
                null,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toStoragePart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Storage");
        Tier tier = inferStorageTier(name);
        Long partId = node.path("id").asLong();
        ScoreTier scoreTier = resolveScoreTier(partId, "storage", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
                extractStorageCapacity(name),
                null,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toPsuPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("PSU");
        Integer normalizedWattage = readInteger(node, "wattageW", "wattage", "powerW");
        int wattage = normalizedWattage == null ? 650 : normalizedWattage;
        if (normalizedWattage == null) {
            tracker.increment();
        }
        Tier tier = wattage >= 850 ? Tier.HIGH : wattage >= 750 ? Tier.MID_HIGH : wattage >= 650 ? Tier.MID : Tier.LOW_MID;
        Long partId = node.path("id").asLong();
        ScoreTier scoreTier = resolveScoreTier(partId, "power_supply", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
                wattage,
                null,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toCasePart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Case");
        Tier tier = normalize(name).contains("mesh") ? Tier.MID_HIGH : Tier.MID;
        Long partId = node.path("id").asLong();
        ScoreTier scoreTier = resolveScoreTier(partId, "pc_case", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
                null,
                null,
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toCpuDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("CPU");
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> socketResolution = hardwareFallbackResolver.resolveCpuSocket(readText(node, "socket"), name);
        if (socketResolution.fallbackUsed()) {
            tracker.increment();
        }
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveCpuTierLabel(name);
        if (tierResolution.fallbackUsed()) {
            tracker.increment();
        }
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = node.path("tdp").isNumber()
                ? new HardwareFallbackResolver.ResolvedValue<>(node.path("tdp").intValue(), false)
                : hardwareFallbackResolver.resolveCpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) {
            tracker.increment();
        }
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "cpu", inferredTier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                socketResolution.value(),
                null,
                wattageResolution.value(),
                textOrNull(node.path("graphics")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toGpuDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("GPU");
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveGpuTierLabel(name);
        if (tierResolution.fallbackUsed()) {
            tracker.increment();
        }
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = hardwareFallbackResolver.resolveGpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) {
            tracker.increment();
        }
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "gpu", inferredTier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                null,
                wattageResolution.value(),
                textOrNull(node.path("chipset")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toMotherboardDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Motherboard");
        Long partId = node.path("id").asLong();
        String socket = readText(node, "socket", "mbSocket", "cpuSocket");
        if (socket == null || socket.isBlank()) {
            tracker.increment();
        }
        String memoryType = resolveMotherboardMemoryType(node, name, tracker);
        if (memoryType == null) {
            tracker.increment();
        }
        ScoreTier scoreTier = resolveScoreTier(partId, "motherboard", Tier.MID, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                socket,
                memoryType,
                null,
                textOrNull(node.path("formFactor")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toMemoryDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Memory");
        Long partId = node.path("id").asLong();
        String memoryType = node.path("ddr").isNumber() ? "DDR" + node.path("ddr").asInt() : null;
        if (memoryType == null) {
            HardwareFallbackResolver.ResolvedValue<String> memoryResolution = hardwareFallbackResolver.resolveMemoryTypeFromName(name);
            memoryType = memoryResolution.value();
            if (memoryResolution.fallbackUsed()) {
                tracker.increment();
            }
        }
        Tier tier = inferMemoryTier(name);
        ScoreTier scoreTier = resolveScoreTier(partId, "memory", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                memoryType,
                null,
                textOrNull(node.path("color")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toStorageDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Storage");
        Long partId = node.path("id").asLong();
        Tier tier = inferStorageTier(name);
        ScoreTier scoreTier = resolveScoreTier(partId, "storage", tier, tracker);
        Integer capacityGb = node.path("capacityGb").isNumber() ? node.path("capacityGb").intValue() : extractStorageCapacity(name);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                null,
                capacityGb,
                textOrNull(node.path("driveInterface")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toPsuDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("PSU");
        Long partId = node.path("id").asLong();
        Integer wattage = node.path("wattage").isNumber() ? node.path("wattage").intValue() : null;
        if (wattage == null) {
            tracker.increment();
            wattage = 650;
        }
        Tier tier = wattage >= 850 ? Tier.HIGH : wattage >= 750 ? Tier.MID_HIGH : wattage >= 650 ? Tier.MID : Tier.LOW_MID;
        ScoreTier scoreTier = resolveScoreTier(partId, "power_supply", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                null,
                wattage,
                textOrNull(node.path("psuType")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toCaseDbPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Case");
        Long partId = node.path("id").asLong();
        Tier tier = normalize(name).contains("mesh") ? Tier.MID_HIGH : Tier.MID;
        ScoreTier scoreTier = resolveScoreTier(partId, "pc_case", tier, tracker);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                null,
                null,
                textOrNull(node.path("caseType")),
                scoreTier.performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private ScoreTier resolveScoreTier(Long partId, String partType, Tier inferredTier, FallbackUsageTracker tracker) {
        if (partId == null || partId <= 0) {
            tracker.increment();
            return new ScoreTier(scoreFromTier(inferredTier), inferredTier.label);
        }

        for (String candidatePartType : scoreTypeAliases(partType)) {
            PartPerformanceMapping mapping = partPerformanceMappingRepository
                    .findFirstByPartTypeAndPartIdAndScoreVersion(candidatePartType, partId, SCORE_VERSION)
                    .orElse(null);
            if (mapping != null) {
                return toScoreTier(mapping);
            }
        }

        return new ScoreTier(scoreFromTier(inferredTier), inferredTier.label);
    }

    private String resolveMotherboardMemoryType(JsonNode node, String name, FallbackUsageTracker tracker) {
        String direct = normalizeDdrLabel(readText(node, "ddrType", "memoryType", "ramType", "memoryStandard", "memory"));
        if (direct != null) {
            return direct;
        }

        Integer ddrNumber = readInteger(node, "ddr", "memoryDdr", "memoryGeneration");
        if (ddrNumber != null && ddrNumber >= 3 && ddrNumber <= 6) {
            tracker.increment();
            return "DDR" + ddrNumber;
        }

        HardwareFallbackResolver.ResolvedValue<String> fallback = hardwareFallbackResolver.resolveMemoryTypeFromName(name);
        if (fallback.fallbackUsed()) {
            tracker.increment();
        }
        return normalizeDdrLabel(fallback.value());
    }

    private String normalizeDdrLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalized.contains("DDR5") || "5".equals(normalized)) {
            return "DDR5";
        }
        if (normalized.contains("DDR4") || "4".equals(normalized)) {
            return "DDR4";
        }
        if (normalized.contains("DDR3") || "3".equals(normalized)) {
            return "DDR3";
        }
        return value.trim();
    }

    private List<String> scoreTypeAliases(String partType) {
        if (partType == null || partType.isBlank()) {
            return List.of();
        }
        return switch (partType) {
            case "power_supply" -> List.of("power_supply", "powerSupply", "psu");
            case "powerSupply" -> List.of("powerSupply", "power_supply", "psu");
            case "pc_case" -> List.of("pc_case", "pcCase", "case");
            case "pcCase" -> List.of("pcCase", "pc_case", "case");
            default -> List.of(partType);
        };
    }

        private ScoreTier toScoreTier(PartPerformanceMapping mapping) {
        String tierLabel = mapping.getTierLabel() == null || mapping.getTierLabel().isBlank()
            ? Tier.MID.label
            : mapping.getTierLabel().trim().toLowerCase(Locale.ROOT);
        Integer performance = mapping.getPerformanceScore() == null ? scoreFromTier(Tier.MID) : mapping.getPerformanceScore();
        return new ScoreTier(performance, tierLabel);
        }

        private BigDecimal percent(int value, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

    private String parseStock(JsonNode node) {
        String stock = readText(node, "stockStatus", "stock_status");
        if (stock == null || stock.isBlank()) {
            return "unknown";
        }
        String normalized = normalize(stock);
        if ("in stock".equals(normalized) || "in_stock".equals(normalized)) {
            return "in_stock";
        }
        if ("out of stock".equals(normalized) || "out_of_stock".equals(normalized)) {
            return "out_of_stock";
        }
        return "unknown";
    }

    private BigDecimal totalPrice(Map<String, BuildResponse.PartDto> parts) {
        return parts.values().stream()
                .map(this::safePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safePrice(BuildResponse.PartDto part) {
        return part == null || part.priceKzt() == null ? BigDecimal.ZERO : part.priceKzt();
    }

    private int safePerformance(BuildResponse.PartDto part) {
        return part == null || part.performanceScore() == null ? 0 : part.performanceScore();
    }

    private BigDecimal kztFromNode(JsonNode priceNode) {
        if (priceNode == null || priceNode.isNull() || priceNode.isMissingNode()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = priceNode.isNumber() ? priceNode.decimalValue() : new BigDecimal(priceNode.asText("0"));
            return value.setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal priceUsdToKzt(JsonNode priceNode) {
        if (priceNode == null || priceNode.isNull() || priceNode.isMissingNode()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = priceNode.isNumber() ? priceNode.decimalValue() : new BigDecimal(priceNode.asText("0"));
            return value.multiply(USD_TO_KZT).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private boolean matchesQuery(String name, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedName = normalize(name);
        String normalizedQuery = normalize(query);
        for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank() && token.length() > 1 && !normalizedName.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal extractBudgetFromPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return DEFAULT_BUDGET;
        }

        BigDecimal explicitKzt = extractExplicitCurrencyAmount(prompt, EXPLICIT_KZT_PATTERN, false);
        if (explicitKzt != null) {
            return explicitKzt.max(new BigDecimal("150000"));
        }

        BigDecimal explicitUsd = extractExplicitCurrencyAmount(prompt, EXPLICIT_USD_PATTERN, true);
        if (explicitUsd != null) {
            return explicitUsd.max(new BigDecimal("150000"));
        }

        String normalized = prompt.toLowerCase(Locale.ROOT);
        boolean tenge = normalized.contains("tenge") || normalized.contains("kzt") || normalized.contains("₸");
        boolean usd = normalized.contains("usd") || normalized.contains("$") || normalized.contains("dollar");

        Matcher matcher = NUMBER_PATTERN.matcher(prompt.replace(",", ""));
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            try {
                values.add(new BigDecimal(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        BigDecimal raw = values.stream().max(BigDecimal::compareTo).orElse(DEFAULT_BUDGET);
        if (tenge) {
            return raw.max(new BigDecimal("150000"));
        }
        if (usd) {
            return raw.multiply(USD_TO_KZT).setScale(0, RoundingMode.HALF_UP).max(new BigDecimal("150000"));
        }
        return raw.compareTo(new BigDecimal("150000")) >= 0 ? raw : DEFAULT_BUDGET;
    }

    private BigDecimal extractExplicitCurrencyAmount(String prompt, Pattern pattern, boolean convertUsd) {
        Matcher matcher = pattern.matcher(prompt.replace(",", " "));
        BigDecimal best = null;
        while (matcher.find()) {
            try {
                BigDecimal amount = new BigDecimal(matcher.group(1).replace(" ", ""));
                if (convertUsd) {
                    amount = amount.multiply(USD_TO_KZT).setScale(0, RoundingMode.HALF_UP);
                }
                if (best == null || amount.compareTo(best) > 0) {
                    best = amount;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private String readText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText(""
                ).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                if (valueNode.isInt() || valueNode.isLong()) {
                    return valueNode.intValue();
                }
                if (valueNode.isNumber()) {
                    return valueNode.numberValue().intValue();
                }
                String raw = valueNode.asText("").replaceAll("[^0-9]", "").trim();
                if (!raw.isBlank()) {
                    try {
                        return Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private String parseMemoryTypeFromName(String text) {
        if (text == null) {
            return null;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("DDR5")) {
            return "DDR5";
        }
        if (upper.contains("DDR4")) {
            return "DDR4";
        }
        return null;
    }

    private String powerEstimateConfidence(BuildResponse.PartDto cpu, BuildResponse.PartDto gpu) {
        boolean cpuFromLookup = isLookupMappedWattage(cpu == null ? null : cpu.name(), CPU_WATTAGE_LOOKUP);
        boolean gpuFromLookup = isLookupMappedWattage(gpu == null ? null : gpu.name(), GPU_WATTAGE_LOOKUP);
        if (cpuFromLookup && gpuFromLookup) {
            return "medium";
        }
        return "low";
    }

    private boolean isLookupMappedWattage(String name, Map<String, Integer> lookup) {
        String normalizedName = normalize(name);
        for (String key : lookup.keySet()) {
            if (normalizedName.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal performanceMappingCoverage(List<BuildResponse.BuildVariantDto> variants) {
        int totalParts = 0;
        int mappedParts = 0;

        for (BuildResponse.BuildVariantDto variant : variants) {
            if (variant == null || variant.parts() == null) {
                continue;
            }
            for (Map.Entry<String, BuildResponse.PartDto> entry : variant.parts().entrySet()) {
                BuildResponse.PartDto part = entry.getValue();
                if (part == null || part.id() == null || part.id() <= 0) {
                    continue;
                }
                totalParts++;
                if (hasMapping(entry.getKey(), part.id())) {
                    mappedParts++;
                }
            }
        }

        if (totalParts == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(mappedParts)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalParts), 2, RoundingMode.HALF_UP);
    }

    private boolean hasMapping(String component, Long partId) {
        String partType = switch (component) {
            case "powerSupply" -> "power_supply";
            case "pcCase" -> "pc_case";
            default -> component;
        };
        return partPerformanceMappingRepository
                .findFirstByPartTypeAndPartIdAndScoreVersion(partType, partId, SCORE_VERSION)
                .isPresent();
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

    private String inferUseCase(String normalizedPrompt) {
        if (normalizedPrompt.contains("gaming") || normalizedPrompt.contains("fps") || normalizedPrompt.contains("esports")) {
            return "gaming";
        }
        if (normalizedPrompt.contains("creator") || normalizedPrompt.contains("editing") || normalizedPrompt.contains("render")) {
            return "creator";
        }
        if (normalizedPrompt.contains("work") || normalizedPrompt.contains("office")) {
            return "work";
        }
        return "mixed";
    }

    private String normalizeUseCase(String aiUseCase, String prompt) {
        String inferred = inferUseCase(prompt == null ? "" : prompt.toLowerCase(Locale.ROOT));
        if (!"mixed".equals(inferred)) {
            return inferred;
        }
        if (aiUseCase == null || aiUseCase.isBlank()) {
            return "mixed";
        }
        return aiUseCase.toLowerCase(Locale.ROOT);
    }

    private String normalizeResolution(String aiResolution, String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        if (normalizedPrompt.contains("4k")) {
            return "4k";
        }
        if (normalizedPrompt.contains("1440")) {
            return "1440p";
        }
        if (aiResolution == null || aiResolution.isBlank()) {
            return "1080p";
        }
        String normalized = aiResolution.toLowerCase(Locale.ROOT);
        if (normalized.contains("1440")) {
            return "1440p";
        }
        if (normalized.contains("4k")) {
            return "4k";
        }
        return "1080p";
    }

    private List<String> buildPrioritiesFromPrompt(String normalizedPrompt) {
        List<String> priorities = new ArrayList<>();
        if (normalizedPrompt.contains("value") || normalizedPrompt.contains("budget") || normalizedPrompt.contains("cheap")) {
            priorities.add("value");
        }
        if (normalizedPrompt.contains("fps") || normalizedPrompt.contains("performance") || normalizedPrompt.contains("gaming")) {
            priorities.add("performance");
        }
        if (normalizedPrompt.contains("upgrade")) {
            priorities.add("upgradability");
        }
        if (priorities.isEmpty()) {
            priorities.add("value");
            priorities.add("performance");
        }
        return List.copyOf(priorities);
    }

    private String resolveBudgetBand(BigDecimal budgetKzt) {
        if (budgetKzt == null) {
            return "mid";
        }
        if (budgetKzt.compareTo(new BigDecimal("350000")) < 0) {
            return "entry";
        }
        if (budgetKzt.compareTo(new BigDecimal("550000")) < 0) {
            return "mid";
        }
        if (budgetKzt.compareTo(new BigDecimal("850000")) < 0) {
            return "upper_mid";
        }
        return "high_end";
    }

    private Integer extractStorageCapacity(String text) {
        if (text == null) {
            return null;
        }
        Matcher tb = Pattern.compile("(\\d+)\\s*tb", Pattern.CASE_INSENSITIVE).matcher(text);
        if (tb.find()) {
            return Integer.parseInt(tb.group(1)) * 1000;
        }
        Matcher gb = Pattern.compile("(\\d{3,4})\\s*gb", Pattern.CASE_INSENSITIVE).matcher(text);
        if (gb.find()) {
            return Integer.parseInt(gb.group(1));
        }
        return null;
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
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

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Tier inferMemoryTier(String name) {
        String n = normalize(name);
        if (n.contains("64gb")) {
            return Tier.HIGH;
        }
        if (n.contains("32gb")) {
            return Tier.MID_HIGH;
        }
        if (n.contains("16gb")) {
            return Tier.MID;
        }
        return Tier.LOW_MID;
    }

    private Tier inferStorageTier(String name) {
        String n = normalize(name);
        Integer capacity = extractStorageCapacity(name);
        if (n.contains("nvme") && capacity != null && capacity >= 2000) {
            return Tier.HIGH;
        }
        if (n.contains("nvme") && capacity != null && capacity >= 1000) {
            return Tier.MID_HIGH;
        }
        if (capacity != null && capacity >= 1000) {
            return Tier.MID;
        }
        return Tier.LOW_MID;
    }

    private int scoreFromTier(Tier tier) {
        return switch (tier) {
            case ENTRY -> 40;
            case LOW_MID -> 55;
            case MID -> 70;
            case MID_HIGH -> 82;
            case HIGH -> 92;
            case ENTHUSIAST -> 98;
        };
    }

    private Tier tierOf(BuildResponse.PartDto part) {
        if (part == null || part.tierLabel() == null) {
            return Tier.MID;
        }
        return Tier.fromLabel(part.tierLabel());
    }

    private String safeName(BuildResponse.PartDto part) {
        return part == null || part.name() == null ? "unknown" : part.name();
    }

    private BuildResponse mockBuildResponse(
            String sessionId,
            BuildResponse.RequirementsDto requirements,
            String budgetBand,
            List<String> warnings,
            long startedAt
    ) {
        BuildResponse.PartDto cpu = new BuildResponse.PartDto(1L, "Fallback Ryzen 5 7600", new BigDecimal("90000"), "AM5", "DDR5", 95, null, 82, "mid_high", "unknown");
        BuildResponse.PartDto gpu = new BuildResponse.PartDto(2L, "Fallback RTX 4060", new BigDecimal("165000"), null, null, 190, null, 70, "mid", "unknown");
        BuildResponse.PartDto motherboard = new BuildResponse.PartDto(3L, "Fallback B650", new BigDecimal("80000"), "AM5", "DDR5", null, null, 82, "mid_high", "unknown");
        BuildResponse.PartDto memory = new BuildResponse.PartDto(4L, "Fallback 32GB DDR5", new BigDecimal("60000"), null, "DDR5", null, null, 82, "mid_high", "unknown");
        BuildResponse.PartDto storage = new BuildResponse.PartDto(5L, "Fallback 1TB NVMe", new BigDecimal("35000"), null, null, 1000, null, 82, "mid_high", "unknown");
        BuildResponse.PartDto psu = new BuildResponse.PartDto(6L, "Fallback 750W PSU", new BigDecimal("40000"), null, null, 750, null, 82, "mid_high", "unknown");
        BuildResponse.PartDto pcCase = new BuildResponse.PartDto(7L, "Fallback Airflow Case", new BigDecimal("30000"), null, null, null, null, 70, "mid", "unknown");

        Map<String, BuildResponse.PartDto> parts = new LinkedHashMap<>();
        parts.put("cpu", cpu);
        parts.put("gpu", gpu);
        parts.put("motherboard", motherboard);
        parts.put("memory", memory);
        parts.put("storage", storage);
        parts.put("powerSupply", psu);
        parts.put("pcCase", pcCase);

        BuildResponse.TotalsDto totals = buildTotals(parts);
        BuildResponse.ChecksDto checks = new BuildResponse.ChecksDto(false, true, true, true, true, true, true, false, false, false, "low");

        warnings.add("Returned fallback deterministic build because no fully valid candidate set was found.");

        return new BuildResponse(
                sessionId,
                requirements,
                budgetBand,
                List.of(
                        new BuildResponse.BuildVariantDto("balanced", parts, totals, new BigDecimal("50.00"), List.of("Fallback selection"), checks)
                ),
                List.of("Fallback explanation: deterministic candidate generation did not find a full top-3 set."),
                checks,
                new BuildResponse.MetricsDto(System.currentTimeMillis() - startedAt, 0, 0, SCORE_VERSION, BigDecimal.ZERO, 0, "low"),
                List.copyOf(warnings)
        );
    }

    private record VariantProfile(
            String label,
            BigDecimal gpuShare,
            BigDecimal cpuShare,
            double valueWeight,
            double performanceWeight,
            double upgradeWeight
    ) {
    }

    private record ComponentPools(
            List<BuildResponse.PartDto> cpus,
            List<BuildResponse.PartDto> gpus,
            List<BuildResponse.PartDto> motherboards,
            List<BuildResponse.PartDto> memories,
            List<BuildResponse.PartDto> storages,
            List<BuildResponse.PartDto> psus,
            List<BuildResponse.PartDto> cases,
            boolean stockValidationTrusted,
            int fallbackInferenceCount
    ) {
    }

    private record VariantCandidate(
            String label,
            Map<String, BuildResponse.PartDto> parts,
            BuildResponse.TotalsDto totals,
            BigDecimal score,
            List<String> tradeoffs,
            BuildResponse.ChecksDto checks,
            int evaluatedCount
    ) {
        private BuildResponse.BuildVariantDto toVariantDto() {
            return new BuildResponse.BuildVariantDto(label, parts, totals, score, tradeoffs, checks);
        }
    }

    private record StockFilterResult(List<BuildResponse.PartDto> parts, boolean trusted) {
    }

    private static final class FallbackUsageTracker {
        private int count;

        private void increment() {
            this.count++;
        }

        private int count() {
            return count;
        }
    }

    private record ScoreTier(int performanceScore, String tierLabel) {
    }

    private enum Tier {
        ENTRY("entry"),
        LOW_MID("low_mid"),
        MID("mid"),
        MID_HIGH("mid_high"),
        HIGH("high"),
        ENTHUSIAST("enthusiast");

        private final String label;

        Tier(String label) {
            this.label = label;
        }

        private static Tier fromLabel(String label) {
            for (Tier tier : values()) {
                if (tier.label.equalsIgnoreCase(label)) {
                    return tier;
                }
            }
            return MID;
        }
    }

    private interface PartMapper {
        BuildResponse.PartDto map(JsonNode node);
    }
}
