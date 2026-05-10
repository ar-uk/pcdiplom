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
import org.example.recommendationservice.dto.ResolutionTarget;
import org.example.recommendationservice.dto.WorkloadType;
import org.example.recommendationservice.model.AiSavedBuild;
import org.example.recommendationservice.model.CpuBenchmark;
import org.example.recommendationservice.model.GpuBenchmark;
import org.example.recommendationservice.model.PartPerformanceMapping;
import org.example.recommendationservice.repository.AiSavedBuildRepository;
import org.example.recommendationservice.repository.PartPerformanceMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FreshBuildRecommendationService {

    private static final BigDecimal DEFAULT_BUDGET = new BigDecimal("500000");
    private static final BigDecimal USD_TO_KZT = new BigDecimal("455");
    private static final String SCORE_VERSION = "v1.0-internal-kz";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{2,7}(?:\\.\\d+)?)");
    private static final Pattern EXPLICIT_KZT_PATTERN = Pattern.compile("(?i)(\\d{2,3}(?:[\\s,]\\d{3})+|\\d{5,7})\\s*(kzt|тенге|₸)");
    private static final Pattern EXPLICIT_KZT_SCALED_PATTERN = Pattern.compile("(?i)(\\d{2,4}(?:\\.\\d+)?)\\s*(k|m|тыс|тысяч|млн)\\s*(kzt|тенге|₸|tenge)");
    private static final Pattern EXPLICIT_USD_PATTERN = Pattern.compile("(?i)(\\d{2,5}(?:[\\s,]\\d{3})*|\\d{2,5})\\s*(usd|dollar|\\$)");

    private static final Map<String, VariantProfile> VARIANTS = Map.of(
            "best_value", new VariantProfile("best_value", new BigDecimal("0.35"), new BigDecimal("0.22"), 0.55, 0.25, 0.20),
            "best_performance", new VariantProfile("best_performance", new BigDecimal("0.45"), new BigDecimal("0.24"), 0.20, 0.60, 0.20),
            "balanced", new VariantProfile("balanced", new BigDecimal("0.40"), new BigDecimal("0.23"), 0.35, 0.40, 0.25)
    );

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final AiSavedBuildRepository aiSavedBuildRepository;
    private final HardwareFallbackResolver hardwareFallbackResolver;
    private final CpuBenchmarkService cpuBenchmarkService;
    private final GpuBenchmarkService gpuBenchmarkService;
    private final PartPerformanceMappingRepository partPerformanceMappingRepository;

    @Value("${part-service.url}")
    private String partServiceUrl;

    private static final Logger log = LoggerFactory.getLogger(FreshBuildRecommendationService.class);

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    private final RestClient restClient = RestClient.builder().build();

    public BuildResponse createBuild(BuildRequest request) {
        String sessionId = UUID.randomUUID().toString();
        FallbackUsageTracker fallbackUsageTracker = new FallbackUsageTracker();
        return buildFromPrompt(sessionId, request.userId(), request.prompt(), request.currency(), request.region(), Boolean.TRUE.equals(request.strictBudget()), fallbackUsageTracker);
    }

    public BuildResponse applyChat(String sessionId, ChatRequest request) {
        AiSavedBuild existing = null;
        try {
            existing = aiSavedBuildRepository.findFirstBySessionId(sessionId).orElse(null);
        } catch (Exception ignored) {
            // Session snapshot lookup should never break chat edits.
        }
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
        IntentScoringProfile intentProfile = buildIntentScoringProfile(requirements, prompt);
        BigDecimal budget = requirements.budgetKzt() == null || requirements.budgetKzt().signum() <= 0
                ? DEFAULT_BUDGET
                : requirements.budgetKzt();
        String budgetBand = resolveBudgetBand(budget);

        try {
            ComponentPools pools = loadComponentPools(prompt, requirements, warnings, fallbackUsageTracker);
            log.info(
                    "Session {}: component pools cpus={} gpus={} motherboards={} memories={} storages={} psus={} cases={} fieldInferenceFallbacks={}",
                    sessionId,
                    pools.cpus().size(),
                    pools.gpus().size(),
                    pools.motherboards().size(),
                    pools.memories().size(),
                    pools.storages().size(),
                    pools.psus().size(),
                    pools.cases().size(),
                    pools.fallbackInferenceCount);
            List<VariantCandidate> builtVariants = new ArrayList<>();
            int candidateBuildsEvaluated = 0;

            for (String label : List.of("best_value", "best_performance", "balanced")) {
                VariantProfile profile = VARIANTS.get(label);
                VariantCandidate candidate = buildVariant(profile, pools, requirements, budget, strictBudget, intentProfile);
                if (candidate != null) {
                    builtVariants.add(candidate);
                    candidateBuildsEvaluated += candidate.evaluatedCount;
                }
            }

            if (builtVariants.isEmpty() && strictBudget) {
                log.warn("Session {}: strict budget filtered all candidate combinations; retrying without strict budget", sessionId);
                warnings.add("Strict budget eliminated all technically compatible combinations; returning nearest compatible options above budget.");
                for (String label : List.of("best_value", "best_performance", "balanced")) {
                    VariantProfile profile = VARIANTS.get(label);
                    VariantCandidate candidate = buildVariant(profile, pools, requirements, budget, false, intentProfile);
                    if (candidate != null) {
                        builtVariants.add(candidate);
                        candidateBuildsEvaluated += candidate.evaluatedCount;
                    }
                }
            }

            if (builtVariants.isEmpty()) {
                String reason = diagnoseNoCandidateReason(pools, requirements, budget, strictBudget);
                warnings.add(reason);
                log.error("Session {}: falling back to deterministic build. reason='{}' budget={} strictBudget={} requirements={} cpuPool={} gpuPool={} mbPool={} memPool={} psuPool={} casePool={} warnings={}",
                        sessionId,
                        reason,
                        budget,
                        strictBudget,
                        requirements,
                        pools.cpus().size(),
                        pools.gpus().size(),
                        pools.motherboards().size(),
                        pools.memories().size(),
                        pools.psus().size(),
                        pools.cases().size(),
                        warnings);

                BuildResponse fallback = mockBuildResponse(sessionId, requirements, budgetBand, warnings, startedAt);
                saveSnapshot(sessionId, userId, prompt, currency, region, strictBudget, requirements, fallback);
                return fallback;
            }

            builtVariants.sort(Comparator.comparing(VariantCandidate::score).reversed());

            if (!builtVariants.isEmpty()) {
                VariantCandidate top = builtVariants.get(0);
                log.info("Session {}: built {} variants, top candidate='{}' score={}", sessionId, builtVariants.size(), top.label, top.score);
            }

            List<BuildResponse.BuildVariantDto> top3 = builtVariants.stream()
                    .map(VariantCandidate::toVariantDto)
                    .toList();

            List<String> explanations = builtVariants.stream()
                    .map(this::buildExplanation)
                    .toList();

            int compatibleBuilds = (int) builtVariants.stream().filter(candidate -> Boolean.TRUE.equals(candidate.checks.compatibilityPassed())).count();
            BuildResponse.ChecksDto aggregateChecks = aggregateChecks(builtVariants);
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
            // Stock validation removed: no stock-based warnings
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
            fetchParts("/api/parsed/cpu", 500, node -> toCpuPart(node, fallbackUsageTracker, requirements.resolutionTarget())),
            fetchParts("/api/reference/matched-cpu", 500, node -> toCpuMatchPart(node, fallbackUsageTracker, requirements.resolutionTarget()))
        );
        if (log.isDebugEnabled()) {
            log.debug("raw cpuParts count={} sampleCpuNames={}", cpuParts.size(), cpuParts.stream().limit(8).map(BuildResponse.PartDto::name).toList());
        }
        List<BuildResponse.PartDto> gpuParts = mergePartPools(
            fetchParts("/api/parsed/video-card", 500, node -> toGpuPart(node, fallbackUsageTracker, requirements.resolutionTarget())),
            fetchParts("/api/reference/matched-gpu", 500, node -> toGpuMatchPart(node, fallbackUsageTracker, requirements.resolutionTarget()))
        );
        List<BuildResponse.PartDto> motherboardParts = mergePartPools(
            fetchParts("/api/parsed/motherboard", 500, node -> toMotherboardPart(node, fallbackUsageTracker)),
            fetchParts("/api/reference/matched-motherboard", 500, node -> toMotherboardMatchPart(node, fallbackUsageTracker))
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

        if (!gpu.parts.isEmpty()) {
            warnings.add("GPU ranking uses inferred metadata from listing names because parsed GPU records are listing-based and not fully normalized.");
        }

        if (filteredCpus.isEmpty() && !cpu.parts.isEmpty()) {
            String brand = requirements.constraints() == null ? null : requirements.constraints().brandCpu();
            log.debug("CPU filtering removed all candidates. cpuQuery='{}' brandFilter='{}' rawCount={} sampleRawNames={} ",
                cpuQuery,
                brand,
                cpu.parts.size(),
                cpu.parts.stream().limit(20).map(BuildResponse.PartDto::name).toList());
            long matched = cpu.parts.stream().filter(p -> matchesQuery(p.name(), cpuQuery) && (brand == null || brand.isBlank() || normalize(p.name()).contains(normalize(brand)))).count();
            log.debug("CPU matched count after query+brand filter = {}", matched);
        }

        // Stock checking removed: do not enforce in-stock filtering

        return new ComponentPools(
                filteredCpus.isEmpty() ? cpu.parts : filteredCpus,
                filteredGpus.isEmpty() ? gpu.parts : filteredGpus,
                motherboard.parts,
                memory.parts,
                storage.parts,
                psu.parts,
                pcCase.parts,
            fallbackUsageTracker.count()
        );
    }

    private Map<String, List<BuildResponse.PartDto>> indexMotherboardsBySocketToken(List<BuildResponse.PartDto> motherboards) {
        Map<String, List<BuildResponse.PartDto>> map = new HashMap<>();
        for (BuildResponse.PartDto mb : motherboards) {
            Set<String> tokens = socketTokens(mb.socket());
            if (tokens.isEmpty()) {
                map.computeIfAbsent("", k -> new ArrayList<>()).add(mb);
            } else {
                for (String t : tokens) {
                    map.computeIfAbsent(t, k -> new ArrayList<>()).add(mb);
                }
            }
        }
        return map;
    }

    private List<BuildResponse.PartDto> resolveMotherboardCandidates(
            String cpuSocket,
            Map<String, List<BuildResponse.PartDto>> motherboardsBySocketToken,
            List<BuildResponse.PartDto> allMotherboards,
            BuildResponse.RequirementsDto requirements,
            BigDecimal budget,
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        Set<String> cpuToks = socketTokens(cpuSocket);
        Map<String, BuildResponse.PartDto> unique = new LinkedHashMap<>();
        if (cpuToks.isEmpty()) {
            for (BuildResponse.PartDto mb : allMotherboards) {
                if (compatibleSocket(cpuSocket, mb.socket())) {
                    unique.putIfAbsent(normalize(mb.name()), mb);
                }
            }
        } else {
            for (String t : cpuToks) {
                for (BuildResponse.PartDto mb : motherboardsBySocketToken.getOrDefault(t, List.of())) {
                    if (mb != null && mb.name() != null) {
                        unique.putIfAbsent(normalize(mb.name()), mb);
                    }
                }
            }
            for (BuildResponse.PartDto mb : motherboardsBySocketToken.getOrDefault("", List.of())) {
                if (compatibleSocket(cpuSocket, mb.socket())) {
                    unique.putIfAbsent(normalize(mb.name()), mb);
                }
            }
        }
        return unique.values().stream()
                .sorted(Comparator
                        .comparingInt((BuildResponse.PartDto m) -> {
                            int socketOk = m.socket() != null && !m.socket().isBlank() ? 0 : 1;
                            int ddrOk = normalizeDdrLabel(m.memoryType()) != null ? 0 : 1;
                            return socketOk * 2 + ddrOk;
                        })
                        .thenComparing((BuildResponse.PartDto part) -> partScore(part, "motherboard", requirements, targetBudgetForComponent(budget, "motherboard", intentProfile), profile, intentProfile), Comparator.reverseOrder()))
                .limit(6)
                .toList();
    }

    private List<BuildResponse.PartDto> filterGpusByVramFloor(List<BuildResponse.PartDto> gpus, int minVramWhenDetected) {
        if (gpus == null || gpus.isEmpty() || minVramWhenDetected <= 0) {
            return gpus;
        }
        List<BuildResponse.PartDto> kept = new ArrayList<>();
        for (BuildResponse.PartDto gpu : gpus) {
            int v = extractGpuVramGb(gpu.name());
            if (v == 0 || v >= minVramWhenDetected) {
                kept.add(gpu);
            }
        }
        return kept;
    }

    private VariantCandidate buildVariant(
            VariantProfile profile,
            ComponentPools pools,
            BuildResponse.RequirementsDto requirements,
            BigDecimal budget,
            boolean strictBudget,
            IntentScoringProfile intentProfile
    ) {
        List<BuildResponse.PartDto> rankedCpu = rankParts(pools.cpus, "cpu", requirements, targetBudgetForComponent(budget, "cpu", intentProfile), profile, intentProfile);
        List<BuildResponse.PartDto> rankedGpu = rankParts(pools.gpus, "gpu", requirements, targetBudgetForComponent(budget, "gpu", intentProfile), profile, intentProfile);

        GamingHardwareFloors gamingFloors = gamingHardwareFloors(requirements);
        if (requirements.useCase() != null && "gaming".equalsIgnoreCase(requirements.useCase())) {
            List<BuildResponse.PartDto> vramFiltered = filterGpusByVramFloor(rankedGpu, gamingFloors.minGpuVramGb());
            if (!vramFiltered.isEmpty()) {
                rankedGpu = vramFiltered;
            }
        }

        List<BuildResponse.PartDto> storageCandidates = rankParts(pools.storages, "storage", requirements, targetBudgetForComponent(budget, "storage", intentProfile), profile, intentProfile)
                .stream()
                .limit(3)
                .toList();
        List<BuildResponse.PartDto> pcCaseCandidates = rankParts(pools.cases, "pcCase", requirements, targetBudgetForComponent(budget, "pcCase", intentProfile), profile, intentProfile)
                .stream()
                .limit(3)
                .toList();
        if (storageCandidates.isEmpty() || pcCaseCandidates.isEmpty()) {
            return null;
        }

        Map<String, List<BuildResponse.PartDto>> motherboardsBySocketToken = indexMotherboardsBySocketToken(pools.motherboards);

        List<BuildResponse.PartDto> cpus = rankedCpu.stream().limit(8).toList();
        List<BuildResponse.PartDto> gpus = rankedGpu.stream().limit(10).toList();

        int evaluated = 0;
        VariantCandidate best = null;

        for (BuildResponse.PartDto cpu : cpus) {
            String cpuSocket = cpu.socket();
            Tier cpuTier = tierOf(cpu);

            List<BuildResponse.PartDto> motherboardCandidates = resolveMotherboardCandidates(
                    cpuSocket,
                    motherboardsBySocketToken,
                    pools.motherboards,
                    requirements,
                    budget,
                    profile,
                    intentProfile);

            if (motherboardCandidates.isEmpty()) {
                continue;
            }

            List<BuildResponse.PartDto> boardsTop = motherboardCandidates.stream().limit(3).toList();

            for (BuildResponse.PartDto gpu : gpus) {
                Tier gpuTier = tierOf(gpu);
                boolean pairingAllowed = cpuGpuPairingAllowed(
                        cpuTier,
                        gpuTier,
                        requirements.useCase(),
                        resolutionLabel(requirements.resolutionTarget()),
                        budget);
                if (!pairingAllowed) {
                    boolean performancePriority = requirements != null
                            && requirements.priorities() != null
                            && requirements.priorities().stream()
                            .anyMatch(priority -> priority != null && priority.toLowerCase(Locale.ROOT).contains("performance"));
                    if (!performancePriority) {
                        continue;
                    }
                }

                int estimatedPower = estimatePower(cpu, gpu);
                int psuMinW = psuMinimumWattage(estimatedPower, budget);
                List<BuildResponse.PartDto> psuCandidates = selectPsuCandidates(
                        pools.psus,
                        psuMinW,
                        estimatedPower,
                        requirements,
                        budget,
                        profile,
                        intentProfile);

                if (psuCandidates.isEmpty()) {
                    continue;
                }

                for (BuildResponse.PartDto motherboard : boardsTop) {
                    String expectedMemoryType = normalizeDdrLabel(motherboard.memoryType());
                    List<BuildResponse.PartDto> memoryPool = pools.memories.stream()
                            .filter(memory -> memory != null && !isSodimm(memory))
                            .filter(memory -> {
                                if (expectedMemoryType == null || expectedMemoryType.isBlank()) {
                                    return true;
                                }
                                String actualMemoryType = effectiveMemoryDdrType(memory);
                                return actualMemoryType != null && expectedMemoryType.equalsIgnoreCase(actualMemoryType);
                            })
                            .toList();
                    List<BuildResponse.PartDto> memoryCandidates = memoryPool.stream()
                            .sorted(Comparator.comparingInt((BuildResponse.PartDto m) -> hasPositivePrice(m) ? 0 : 1)
                                    .thenComparing(memoryCandidateComparator(requirements, budget, profile, intentProfile)))
                            .limit(3)
                            .toList();

                    if (memoryCandidates.isEmpty()) {
                        continue;
                    }

                    for (BuildResponse.PartDto memory : memoryCandidates) {
                        for (BuildResponse.PartDto storage : storageCandidates) {
                            for (BuildResponse.PartDto pcCase : pcCaseCandidates) {
                                for (BuildResponse.PartDto psu : psuCandidates) {
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

                                    BuildResponse.ChecksDto checks = evaluateChecks(parts, budget, requirements, strictBudget);
                                    if (!Boolean.TRUE.equals(checks.compatibilityPassed())) {
                                        continue;
                                    }

                                    BigDecimal score = buildCompositeScore(parts, total, budget, requirements, profile, intentProfile);
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
            boolean strictBudget
        ) {
        BuildResponse.PartDto cpu = parts.get("cpu");
        BuildResponse.PartDto gpu = parts.get("gpu");
        BuildResponse.PartDto motherboard = parts.get("motherboard");
        BuildResponse.PartDto memory = parts.get("memory");
        BuildResponse.PartDto storage = parts.get("storage");
        BuildResponse.PartDto psu = parts.get("powerSupply");

        boolean socketCompatible = compatibleSocket(cpu == null ? null : cpu.socket(), motherboard == null ? null : motherboard.socket());
        String expectedMemoryType = motherboard == null ? null : normalizeDdrLabel(motherboard.memoryType());
        String actualMemoryType = memory == null ? null : effectiveMemoryDdrType(memory);
        boolean memoryCompatible = memory != null
            && !isSodimm(memory)
            && (expectedMemoryType == null
                || (actualMemoryType != null && expectedMemoryType.equalsIgnoreCase(actualMemoryType)));

        int estimatedPower = estimatePower(cpu, gpu);
        int psuWatts = safeInt(psu == null ? null : psu.wattage(), 0);
        boolean psuMinHeadroomOk = psuWatts >= psuMinimumWattage(estimatedPower, budget);
        boolean psuPreferredHeadroomOk = psuWatts >= psuPreferredWattage(estimatedPower);

        BigDecimal total = totalPrice(parts);
        boolean budgetOk = total.compareTo(budget) <= 0;
        boolean cpuGpuBalanceOk = cpuGpuPairingAllowed(
                tierOf(cpu),
                tierOf(gpu),
                requirements.useCase(),
                resolutionLabel(requirements.resolutionTarget()),
                budget);
        boolean gamingMinimumsOk = meetsGamingMinimums(requirements, gpu, memory, storage);
        boolean caseFitValidated = false;
        boolean stockValidationEnforced = false;
        String powerEstimateConfidence = powerEstimateConfidence(cpu, gpu);

        boolean compatibilityPassed = socketCompatible
                && memoryCompatible
                && psuMinHeadroomOk
            && cpuGpuBalanceOk
            && gamingMinimumsOk;

        return new BuildResponse.ChecksDto(
                compatibilityPassed,
                socketCompatible,
                memoryCompatible,
                psuMinHeadroomOk,
                psuPreferredHeadroomOk,
                budgetOk,
                cpuGpuBalanceOk,
                false,
                false,
                caseFitValidated,
                powerEstimateConfidence
        );
    }
    private BuildResponse.ChecksDto aggregateChecks(List<VariantCandidate> builtVariants) {
        boolean compatibilityPassed = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.compatibilityPassed()));
        boolean socketCompatible = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.socketCompatible()));
        boolean memoryCompatible = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.memoryCompatible()));
        boolean psuMinimum = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.psuMinimumHeadroomPassed()));
        boolean psuPreferred = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.psuPreferredHeadroomPassed()));
        boolean budgetOk = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.budgetOk()));
        boolean cpuGpuBalanceOk = builtVariants.stream().allMatch(candidate -> Boolean.TRUE.equals(candidate.checks.cpuGpuBalanceOk()));
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
            false,
            false,
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
                    .map(this::effectiveMemoryDdrType)
                    .anyMatch(memoryType::equalsIgnoreCase));
            if (!hasMemoryMatch) {
            return "No memory kit matched the motherboard memoryType values.";
            }
        }

        boolean hasPsuHeadroom = pools.cpus.stream().anyMatch(cpu -> pools.gpus.stream().anyMatch(gpu -> {
            int estimatedPower = estimatePower(cpu, gpu);
            int psuMinW = psuMinimumWattage(estimatedPower, budget);
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
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }

        List<BuildResponse.PartDto> priced = parts.stream()
            .filter(part -> part != null && part.priceKzt() != null && part.priceKzt().signum() > 0)
            .toList();

        List<BuildResponse.PartDto> source = priced.isEmpty() ? parts : priced;

        List<BuildResponse.PartDto> sorted = source.stream()
                .sorted(Comparator.comparing((BuildResponse.PartDto part) -> partScore(part, component, requirements, targetBudget, profile, intentProfile)).reversed())
                .toList();

        if ("cpu".equals(component)) {
            List<BuildResponse.PartDto> socketFirst = new ArrayList<>();
            List<BuildResponse.PartDto> socketUnknown = new ArrayList<>();
            for (BuildResponse.PartDto part : sorted) {
                if (part != null && part.socket() != null && !part.socket().isBlank()) {
                    socketFirst.add(part);
                } else {
                    socketUnknown.add(part);
                }
            }
            socketFirst.addAll(socketUnknown);
            return socketFirst;
        }

        return sorted;
    }

    private double partScore(
            BuildResponse.PartDto part,
            String component,
            BuildResponse.RequirementsDto requirements,
            BigDecimal targetBudget,
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        if (part == null) {
            return 0.0;
        }

        double value = priceFitScore(part.priceKzt(), targetBudget);
        double performance = part.performanceScore() == null ? 0 : part.performanceScore();
        double upgrade = upgradePathScore(part, component, requirements);
        double efficiency = efficiencyScore(part, component);
        double aesthetic = aestheticScore(part, component, requirements);
        double noise = noiseScore(part, component);

        if ("storage".equals(component) && requirements != null && "gaming".equalsIgnoreCase(requirements.useCase())) {
            if (isSsd(part)) {
                performance += 30;
            }
            if (isHdd(part)) {
                performance -= 40;
            }
        }

        if ("gpu".equals(component) && requirements != null) {
            String resolution = resolutionLabel(requirements.resolutionTarget());
            boolean perfHeavy = requirements.priorities() != null
                    && requirements.priorities().stream()
                    .anyMatch(priority -> priority != null && priority.toLowerCase(Locale.ROOT).contains("performance"));

            if ("1440p".equalsIgnoreCase(resolution)
                    || "4k".equalsIgnoreCase(resolution)
                    || perfHeavy) {
                performance *= 1.20;
            }
        }

        if ("memory".equals(component) && requirements != null && isEntryBudget(requirements.budgetKzt())) {
            int gb = extractMemoryCapacityGb(part.name());
            if (gb >= 32) {
                performance -= 28.0;
            } else if (gb >= 24) {
                performance -= 12.0;
            }
        }

        if ("powerSupply".equals(component) && requirements != null && isEntryBudget(requirements.budgetKzt())) {
            int w = safeInt(part.wattage(), 0);
            if (w > 700) {
                performance -= Math.min(35.0, (w - 700) / 8.0);
            }
        }

        double performanceWeight = Math.min(0.92, Math.max(0.15, componentWeight(component, intentProfile) + 0.12));
        double valueWeight = Math.max(0.08, 1.0 - performanceWeight);

        double objectiveScore = (valueWeight * value)
            + (performanceWeight * performance)
            + (Math.max(0.05, intentProfile.upgradeWeight()) * upgrade)
            + (intentProfile.efficiencyWeight() * efficiency)
            + (intentProfile.aestheticWeight() * aesthetic)
            + (intentProfile.noiseWeight() * noise);

        return objectiveScore;
    }

    private boolean isHdd(BuildResponse.PartDto part) {
        String name = part == null || part.name() == null ? "" : part.name().toLowerCase(Locale.ROOT);
        return name.contains("hdd") || name.contains("3.5");
    }

    private boolean isSsd(BuildResponse.PartDto part) {
        String name = part == null || part.name() == null ? "" : part.name().toLowerCase(Locale.ROOT);
        return name.contains("ssd") || name.contains("nvme") || name.contains("m.2");
    }

    private boolean isSodimm(BuildResponse.PartDto memory) {
        String name = memory == null || memory.name() == null ? "" : memory.name().toLowerCase(Locale.ROOT);
        return name.contains("so-dimm") || name.contains("sodimm");
    }

    private boolean isEntryBudget(BigDecimal budgetKzt) {
        return budgetKzt != null && budgetKzt.compareTo(new BigDecimal("350000")) < 0;
    }

    private boolean hasPositivePrice(BuildResponse.PartDto part) {
        return part != null && part.priceKzt() != null && part.priceKzt().signum() > 0;
    }

    private Comparator<BuildResponse.PartDto> memoryCandidateComparator(
            BuildResponse.RequirementsDto requirements,
            BigDecimal budget,
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        Comparator<BuildResponse.PartDto> byScore = Comparator.comparing(
                (BuildResponse.PartDto part) -> partScore(part, "memory", requirements, targetBudgetForComponent(budget, "memory", intentProfile), profile, intentProfile))
                .reversed();
        if (!isEntryBudget(budget)) {
            return byScore;
        }
        return Comparator
                .comparingInt((BuildResponse.PartDto m) -> {
                    int gb = extractMemoryCapacityGb(m.name());
                    return gb <= 0 ? 999 : gb;
                })
                .thenComparing(
                        (BuildResponse.PartDto part) -> partScore(part, "memory", requirements, targetBudgetForComponent(budget, "memory", intentProfile), profile, intentProfile),
                        Comparator.reverseOrder());
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
            if (requirements != null && isEntryBudget(requirements.budgetKzt())) {
                score += 4.0;
            } else {
                score += 20.0;
            }
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
        Set<String> cpuTokens = socketTokens(cpuSocket);
        Set<String> motherboardTokens = socketTokens(motherboardSocket);
        if (cpuTokens.isEmpty() || motherboardTokens.isEmpty()) {
            return cpuSocket.equalsIgnoreCase(motherboardSocket);
        }
        for (String token : cpuTokens) {
            if (motherboardTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> socketTokens(String socket) {
        if (socket == null || socket.isBlank()) {
            return Set.of();
        }

        String normalized = normalize(socket);
        Set<String> tokens = new LinkedHashSet<>();

        if (normalized.contains("am4")) tokens.add("am4");
        if (normalized.contains("am5")) tokens.add("am5");
        if (normalized.contains("am3")) tokens.add("am3");
        if (normalized.contains("am2")) tokens.add("am2");
        if (normalized.contains("lga1700")) tokens.add("lga1700");
        if (normalized.contains("lga1851")) tokens.add("lga1851");
        if (normalized.contains("lga1200")) tokens.add("lga1200");
        if (normalized.contains("lga1151")) tokens.add("lga1151");
        if (normalized.contains("lga1150")) tokens.add("lga1150");
        if (normalized.contains("lga2066")) tokens.add("lga2066");
        if (normalized.contains("sTRx4".toLowerCase(Locale.ROOT))) tokens.add("strx4");
        if (normalized.contains("tr4")) tokens.add("tr4");

        Matcher lgaMatcher = Pattern.compile("lga\\s*(\\d{4})").matcher(normalized);
        while (lgaMatcher.find()) {
            tokens.add("lga" + lgaMatcher.group(1));
        }

        Matcher amMatcher = Pattern.compile("am\\s*(\\d)").matcher(normalized);
        while (amMatcher.find()) {
            tokens.add("am" + amMatcher.group(1));
        }

        Matcher numberMatcher = Pattern.compile("\\b(1150|1151|1200|1700|1851|2066)\\b").matcher(normalized);
        while (numberMatcher.find()) {
            tokens.add("lga" + numberMatcher.group(1));
        }

        return tokens;
    }

    private boolean cpuGpuPairingAllowed(
            Tier cpuTier,
            Tier gpuTier,
            String useCase,
            String resolution,
            BigDecimal budgetKzt
    ) {
        if (cpuTier == null || gpuTier == null) {
            return false;
        }
        int delta = Math.abs(cpuTier.ordinal() - gpuTier.ordinal());
        String normalizedUseCase = useCase == null ? "mixed" : useCase.toLowerCase(Locale.ROOT);
        String normalizedResolution = resolution == null ? "1080p" : resolution.toLowerCase(Locale.ROOT);
        boolean entryBudget = budgetKzt != null && budgetKzt.compareTo(new BigDecimal("350000")) < 0;

        if ("gaming".equals(normalizedUseCase)) {
            if (normalizedResolution.contains("4k")) {
                return delta <= 4;
            }
            if (normalizedResolution.contains("1440")) {
                return delta <= 3;
            }
            if (entryBudget) {
                return delta <= 4;
            }
            return delta <= 2;
        }

        if ("work".equals(normalizedUseCase)) {
            return delta <= 4;
        }

        return delta <= 3;
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
        return inferredCpuPowerForEstimate(cpu) + inferredGpuPowerForEstimate(gpu) + 120;
    }

    private int inferredCpuPowerForEstimate(BuildResponse.PartDto cpu) {
        if (cpu == null) {
            return 65;
        }
        int w = safeInt(cpu.wattage(), 0);
        if (w > 0) {
            return w;
        }
        return switch (tierOf(cpu)) {
            case ENTRY -> 65;
            case LOW_MID -> 75;
            case MID -> 95;
            case MID_HIGH -> 105;
            case HIGH -> 125;
            case ENTHUSIAST -> 165;
        };
    }

    private int inferredGpuPowerForEstimate(BuildResponse.PartDto gpu) {
        if (gpu == null) {
            return 0;
        }
        int w = safeInt(gpu.wattage(), 0);
        if (w > 0) {
            return w;
        }
        return switch (tierOf(gpu)) {
            case ENTRY -> 65;
            case LOW_MID -> 85;
            case MID -> 115;
            case MID_HIGH -> 150;
            case HIGH -> 185;
            case ENTHUSIAST -> 240;
        };
    }

    private int psuMinimumWattage(int estimatedPower, BigDecimal budgetKzt) {
        BigDecimal mult = new BigDecimal("1.20");
        if (budgetKzt != null && budgetKzt.compareTo(new BigDecimal("350000")) < 0) {
            mult = new BigDecimal("1.12");
        }
        return BigDecimal.valueOf(estimatedPower)
                .multiply(mult)
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private List<BuildResponse.PartDto> selectPsuCandidates(
            List<BuildResponse.PartDto> psus,
            int psuMinW,
            int estimatedPower,
            BuildResponse.RequirementsDto requirements,
            BigDecimal budget,
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        int preferredW = psuPreferredWattage(estimatedPower);
        Comparator<BuildResponse.PartDto> byScore = Comparator.comparing(
                (BuildResponse.PartDto part) -> partScore(part, "powerSupply", requirements, targetBudgetForComponent(budget, "powerSupply", intentProfile), profile, intentProfile))
                .reversed();
        Comparator<BuildResponse.PartDto> psuOrder = isEntryBudget(budget)
                ? Comparator
                .<BuildResponse.PartDto>comparingInt(p -> Math.abs(safeInt(p.wattage(), 0) - preferredW))
                .thenComparingInt(p -> safeInt(p.wattage(), 0))
                .thenComparing((BuildResponse.PartDto part) -> partScore(part, "powerSupply", requirements, targetBudgetForComponent(budget, "powerSupply", intentProfile), profile, intentProfile), Comparator.reverseOrder())
                : byScore;
        List<BuildResponse.PartDto> primary = psus.stream()
                .filter(item -> safeInt(item.wattage(), 0) >= psuMinW)
                .sorted(psuOrder)
                .limit(3)
                .toList();
        if (!primary.isEmpty()) {
            return primary;
        }
        int relaxed = Math.max(350, (int) Math.floor(psuMinW * 0.92));
        List<BuildResponse.PartDto> secondary = psus.stream()
                .filter(item -> safeInt(item.wattage(), 0) >= relaxed)
                .sorted(psuOrder)
                .limit(3)
                .toList();
        if (!secondary.isEmpty()) {
            return secondary;
        }
        return psus.stream()
                .filter(item -> safeInt(item.wattage(), 0) > 0)
                .sorted(Comparator.comparingInt((BuildResponse.PartDto p) -> safeInt(p.wattage(), 0)))
                .limit(3)
                .toList();
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
            BuildResponse.RequirementsDto requirements,
            VariantProfile profile,
            IntentScoringProfile intentProfile
    ) {
        BuildResponse.PartDto cpu = parts.get("cpu");
        BuildResponse.PartDto gpu = parts.get("gpu");
        BuildResponse.PartDto memory = parts.get("memory");
        BuildResponse.PartDto storage = parts.get("storage");
        BuildResponse.PartDto motherboard = parts.get("motherboard");
        BuildResponse.PartDto psu = parts.get("powerSupply");
        BuildResponse.PartDto pcCase = parts.get("pcCase");

        double intentPerformance = weightedPartPerformance(parts, intentProfile);
        double value = marketValueScore(parts, budget, intentProfile);
        double balance = 100.0 - (Math.abs(tierOf(cpu).ordinal() - tierOf(gpu).ordinal()) * 20.0);
        double upgrade = (
            upgradePathScore(cpu, "cpu", requirements)
                + upgradePathScore(motherboard, "motherboard", requirements)
                + upgradePathScore(psu, "powerSupply", requirements)
        ) / 3.0;
        double preferenceMatch = (
            aestheticScore(pcCase, "pcCase", requirements)
                + noiseScore(psu, "powerSupply")
                + efficiencyScore(psu, "powerSupply")
        ) / 3.0;

        double budgetPenalty = budgetAllocationPenalty(parts, budget, intentProfile);
        double bottleneckPenalty = bottleneckPenalty(cpu, gpu, requirements, intentProfile);

        double score = (intentProfile.rawPerformanceWeight() * intentPerformance)
            + (intentProfile.valueWeight() * value)
            + (0.10 * Math.max(0.0, balance))
            + (intentProfile.upgradeWeight() * upgrade)
            + (0.05 * preferenceMatch)
            - (intentProfile.budgetEfficiencyWeight() * budgetPenalty)
            - (intentProfile.bottleneckPenaltyWeight() * bottleneckPenalty);

        if (memory != null && isSodimm(memory)) {
            score -= 15.0;
        }
        if (storage != null && isHdd(storage) && requirements != null && "gaming".equalsIgnoreCase(requirements.useCase())) {
            score -= 12.0;
        }

        return BigDecimal.valueOf(Math.max(0.0, score)).setScale(2, RoundingMode.HALF_UP);
    }

    private IntentScoringProfile buildIntentScoringProfile(BuildResponse.RequirementsDto requirements, String prompt) {
        WorkloadType workload = requirements == null || requirements.workloadType() == null
                ? inferWorkload(prompt)
                : requirements.workloadType();
        ResolutionTarget resolution = requirements == null || requirements.resolutionTarget() == null
                ? inferResolutionHint(prompt == null ? "" : prompt.toLowerCase(Locale.ROOT))
                : requirements.resolutionTarget();
        int refreshRateHz = requirements == null || requirements.refreshRateHz() == null || requirements.refreshRateHz() <= 0
                ? inferRefreshRate(prompt)
                : requirements.refreshRateHz();
        BigDecimal budget = requirements == null || requirements.budgetKzt() == null ? DEFAULT_BUDGET : requirements.budgetKzt();

        double budgetPressure = budgetPressure(budget);
        double refreshPressure = refreshPressure(refreshRateHz);
        double resolutionPressure = switch (resolution) {
            case P4K -> 1.0;
            case P1440 -> 0.65;
            default -> 0.30;
        };

        double gpuWeight;
        double cpuWeight;
        double ramWeight;
        double storageWeight;
        double motherboardWeight;
        double psuWeight;
        double caseWeight;
        double valueWeight;
        double performanceWeight;
        double upgradeWeight;

        switch (workload) {
            case ESPORTS -> {
                gpuWeight = 0.30 + (resolutionPressure * 0.05) + (refreshPressure * 0.05);
                cpuWeight = 0.34 + (refreshPressure * 0.10);
                ramWeight = 0.13;
                storageWeight = 0.07;
                motherboardWeight = 0.08;
                psuWeight = 0.04;
                caseWeight = 0.04;
                performanceWeight = 0.42 + (refreshPressure * 0.10);
                valueWeight = 0.26 + (budgetPressure * 0.10);
                upgradeWeight = 0.14;
            }
            case GAMING, AAA -> {
                gpuWeight = 0.40 + (resolutionPressure * 0.14) + (refreshPressure * 0.04);
                cpuWeight = 0.24 - (resolutionPressure * 0.04) + (refreshPressure * 0.03);
                ramWeight = 0.12;
                storageWeight = 0.08;
                motherboardWeight = 0.07;
                psuWeight = 0.05;
                caseWeight = 0.04;
                performanceWeight = 0.48 + (resolutionPressure * 0.08);
                valueWeight = 0.22 + (budgetPressure * 0.10);
                upgradeWeight = 0.13;
            }
            case WORK, CREATION -> {
                gpuWeight = 0.18 + (resolutionPressure * 0.03);
                cpuWeight = 0.34 + (refreshPressure * 0.04);
                ramWeight = 0.18;
                storageWeight = 0.12;
                motherboardWeight = 0.10;
                psuWeight = 0.04;
                caseWeight = 0.04;
                performanceWeight = 0.34 + (refreshPressure * 0.04);
                valueWeight = 0.30 + (budgetPressure * 0.08);
                upgradeWeight = 0.18;
            }
            default -> {
                gpuWeight = 0.30 + (resolutionPressure * 0.06);
                cpuWeight = 0.28 + (refreshPressure * 0.05);
                ramWeight = 0.15;
                storageWeight = 0.10;
                motherboardWeight = 0.09;
                psuWeight = 0.04;
                caseWeight = 0.04;
                performanceWeight = 0.38 + (resolutionPressure * 0.05);
                valueWeight = 0.28 + (budgetPressure * 0.08);
                upgradeWeight = 0.16;
            }
        }

        double weightSum = gpuWeight + cpuWeight + ramWeight + storageWeight + motherboardWeight + psuWeight + caseWeight;
        if (weightSum <= 0.0) {
            weightSum = 1.0;
        }

        return new IntentScoringProfile(
                normalizeWeight(gpuWeight / weightSum),
                normalizeWeight(cpuWeight / weightSum),
                normalizeWeight(ramWeight / weightSum),
                normalizeWeight(storageWeight / weightSum),
                normalizeWeight(motherboardWeight / weightSum),
                normalizeWeight(psuWeight / weightSum),
                normalizeWeight(caseWeight / weightSum),
                normalizeWeight(valueWeight),
                normalizeWeight(performanceWeight),
                normalizeWeight(upgradeWeight),
                normalizeWeight(0.08 + (workload == WorkloadType.WORK || workload == WorkloadType.CREATION ? 0.05 : 0.0)),
                normalizeWeight(workload == WorkloadType.WORK || workload == WorkloadType.CREATION ? 0.06 : 0.03),
                normalizeWeight(workload == WorkloadType.ESPORTS ? 0.05 : 0.03),
                normalizeWeight(0.12),
                normalizeWeight(0.14 + budgetPressure * 0.04),
                normalizeWeight(Math.max(0.18, gpuWeight - 0.10)),
                normalizeWeight(Math.min(0.72, gpuWeight + 0.18)),
                normalizeWeight(Math.max(0.14, cpuWeight - 0.06)),
                normalizeWeight(Math.min(0.50, cpuWeight + 0.18)),
                normalizeWeight(Math.max(0.08, ramWeight - 0.04)),
                normalizeWeight(Math.min(0.30, ramWeight + 0.10)),
                normalizeWeight(0.14 + budgetPressure * 0.06)
        );
    }

    private double componentWeight(String component, IntentScoringProfile profile) {
        return switch (component) {
            case "gpu" -> profile.gpuWeight();
            case "cpu" -> profile.cpuWeight();
            case "memory" -> profile.ramWeight();
            case "storage" -> profile.storageWeight();
            case "motherboard" -> profile.motherboardWeight();
            case "powerSupply" -> profile.psuWeight();
            case "pcCase" -> profile.caseWeight();
            default -> 0.05;
        };
    }

    private BigDecimal targetBudgetForComponent(BigDecimal budget, String component, IntentScoringProfile profile) {
        return budget.multiply(BigDecimal.valueOf(Math.max(0.04, componentWeight(component, profile))));
    }

    private double weightedPartPerformance(Map<String, BuildResponse.PartDto> parts, IntentScoringProfile profile) {
        if (parts == null || parts.isEmpty()) {
            return 0.0;
        }

        double weighted = 0.0;
        weighted += componentWeight("cpu", profile) * safePerformance(parts.get("cpu"));
        weighted += componentWeight("gpu", profile) * safePerformance(parts.get("gpu"));
        weighted += componentWeight("memory", profile) * safePerformance(parts.get("memory"));
        weighted += componentWeight("storage", profile) * safePerformance(parts.get("storage"));
        weighted += componentWeight("motherboard", profile) * safePerformance(parts.get("motherboard"));
        weighted += componentWeight("powerSupply", profile) * safePerformance(parts.get("powerSupply"));
        weighted += componentWeight("pcCase", profile) * safePerformance(parts.get("pcCase"));
        return weighted;
    }

    private double marketValueScore(Map<String, BuildResponse.PartDto> parts, BigDecimal budget, IntentScoringProfile intentProfile) {
        if (parts == null || parts.isEmpty()) {
            return 0.0;
        }

        double perf = weightedPartPerformance(parts, intentProfile);
        BigDecimal total = totalPrice(parts);
        if (total.signum() <= 0) {
            return 0.0;
        }

        double perfPerKzt = perf / total.doubleValue();
        double budgetFit = priceFitScore(total, budget) / 100.0;
        return Math.min(100.0, (perfPerKzt * 400000.0 * 0.7) + (budgetFit * 30.0));
    }

    private double budgetAllocationPenalty(
            Map<String, BuildResponse.PartDto> parts,
            BigDecimal budget,
            IntentScoringProfile profile
    ) {
        if (parts == null || budget == null || budget.signum() <= 0) {
            return 0.0;
        }

        BigDecimal total = totalPrice(parts);
        if (total.signum() <= 0) {
            return 0.0;
        }

        double gpuShareTotal = share(parts.get("gpu"), total);
        double cpuShareTotal = share(parts.get("cpu"), total);
        double memoryShareTotal = share(parts.get("memory"), total);
        double motherboardShareTotal = share(parts.get("motherboard"), total);

        double gpuShareBudget = shareAgainstBudget(parts.get("gpu"), budget);
        double cpuShareBudget = shareAgainstBudget(parts.get("cpu"), budget);
        double memoryShareBudget = shareAgainstBudget(parts.get("memory"), budget);
        double motherboardShareBudget = shareAgainstBudget(parts.get("motherboard"), budget);

        double penalty = 0.0;
        if (gpuShareTotal < profile.gpuMinShare()) {
            penalty += (profile.gpuMinShare() - gpuShareTotal) * 140.0;
        }
        if (gpuShareBudget < profile.gpuMinShare()) {
            penalty += (profile.gpuMinShare() - gpuShareBudget) * 80.0;
        }
        if (gpuShareTotal > profile.gpuMaxShare()) {
            penalty += (gpuShareTotal - profile.gpuMaxShare()) * 120.0;
        }
        if (cpuShareTotal < profile.cpuMinShare()) {
            penalty += (profile.cpuMinShare() - cpuShareTotal) * 110.0;
        }
        if (cpuShareBudget < profile.cpuMinShare()) {
            penalty += (profile.cpuMinShare() - cpuShareBudget) * 70.0;
        }
        if (cpuShareTotal > profile.cpuMaxShare()) {
            penalty += (cpuShareTotal - profile.cpuMaxShare()) * 90.0;
        }
        if (memoryShareTotal < profile.ramMinShare()) {
            penalty += (profile.ramMinShare() - memoryShareTotal) * 75.0;
        }
        if (memoryShareBudget < profile.ramMinShare()) {
            penalty += (profile.ramMinShare() - memoryShareBudget) * 40.0;
        }
        if (memoryShareTotal > profile.ramMaxShare()) {
            penalty += (memoryShareTotal - profile.ramMaxShare()) * 60.0;
        }
        if (motherboardShareTotal > profile.motherboardMaxShare()) {
            penalty += (motherboardShareTotal - profile.motherboardMaxShare()) * 130.0;
        }
        if (motherboardShareBudget > profile.motherboardMaxShare()) {
            penalty += (motherboardShareBudget - profile.motherboardMaxShare()) * 50.0;
        }

        if (budget.signum() > 0 && total.compareTo(budget) < 0 && profile.rawPerformanceWeight() > profile.valueWeight()) {
            double budgetUsage = total.divide(budget, 6, RoundingMode.HALF_UP).doubleValue();
            penalty += (1.0 - budgetUsage) * 120.0;
        }

        return Math.min(100.0, penalty);
    }

    private double bottleneckPenalty(
            BuildResponse.PartDto cpu,
            BuildResponse.PartDto gpu,
            BuildResponse.RequirementsDto requirements,
            IntentScoringProfile profile
    ) {
        if (cpu == null || gpu == null) {
            return 50.0;
        }

        int cpuTier = tierOf(cpu).ordinal();
        int gpuTier = tierOf(gpu).ordinal();
        int delta = gpuTier - cpuTier;

        String useCase = requirements == null || requirements.useCase() == null ? "mixed" : requirements.useCase().toLowerCase(Locale.ROOT);
        String resolution = resolutionLabel(requirements == null ? null : requirements.resolutionTarget());

        double penalty;
        if ("gaming".equals(useCase) && resolution.contains("1080")) {
            penalty = Math.max(0, Math.abs(delta) - 1) * 16.0;
        } else if ("gaming".equals(useCase) && resolution.contains("1440")) {
            penalty = Math.max(0, Math.abs(delta) - 2) * 13.0;
        } else if ("gaming".equals(useCase) && resolution.contains("4k")) {
            penalty = Math.max(0, (cpuTier - gpuTier) - 1) * 10.0;
        } else {
            penalty = Math.max(0, Math.abs(delta) - 2) * 10.0;
        }

        return Math.min(100.0, penalty * (1.0 + profile.bottleneckPenaltyWeight()));
    }

    private double efficiencyScore(BuildResponse.PartDto part, String component) {
        if (part == null) {
            return 40.0;
        }

        if ("powerSupply".equals(component)) {
            int wattage = safeInt(part.wattage(), 0);
            if (wattage >= 750 && wattage <= 850) {
                return 90.0;
            }
            if (wattage > 0) {
                return 70.0;
            }
        }

        int perf = safePerformance(part);
        int watts = safeInt(part.wattage(), 0);
        if (perf <= 0 || watts <= 0) {
            return 55.0;
        }

        return Math.min(100.0, (perf * 2.0) / Math.max(1.0, watts / 50.0));
    }

    private double aestheticScore(BuildResponse.PartDto part, String component, BuildResponse.RequirementsDto requirements) {
        if (part == null) {
            return 50.0;
        }
        String name = normalize(part.name());
        boolean wantsRgb = requirements != null
                && requirements.constraints() != null
                && Boolean.TRUE.equals(requirements.constraints().rgb());
        boolean wantsWhite = requirements != null
                && requirements.priorities() != null
                && requirements.priorities().stream().anyMatch(priority -> normalize(priority).contains("white"));

        double score = 60.0;
        if (wantsRgb) {
            score += name.contains("rgb") ? 25.0 : -15.0;
        }
        if (wantsWhite) {
            score += name.contains("white") ? 20.0 : -10.0;
        }
        if ("pcCase".equals(component) && (name.contains("mesh") || name.contains("airflow"))) {
            score += 10.0;
        }

        return Math.max(0.0, Math.min(100.0, score));
    }

    private double noiseScore(BuildResponse.PartDto part, String component) {
        if (part == null) {
            return 50.0;
        }
        String name = normalize(part.name());
        if ("pcCase".equals(component) || "powerSupply".equals(component)) {
            if (name.contains("silent") || name.contains("quiet")) {
                return 90.0;
            }
            if (name.contains("airflow") || name.contains("mesh")) {
                return 72.0;
            }
        }
        return 60.0;
    }

    private double blend(double variantWeight, double intentWeight) {
        return (variantWeight * 0.45) + (intentWeight * 0.55);
    }

    private double share(BuildResponse.PartDto part, BigDecimal total) {
        if (part == null || total == null || total.signum() <= 0 || part.priceKzt() == null) {
            return 0.0;
        }
        return part.priceKzt().divide(total, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private double shareAgainstBudget(BuildResponse.PartDto part, BigDecimal budget) {
        if (part == null || budget == null || budget.signum() <= 0 || part.priceKzt() == null) {
            return 0.0;
        }
        return part.priceKzt().divide(budget, 6, RoundingMode.HALF_UP).doubleValue();
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
            String preprocessedPrompt = preprocessPrompt(prompt);
            if (!openAiEnabled || openAiProperties.apiKey() == null || openAiProperties.apiKey().isBlank()) {
                return requirementsFromPrompt(preprocessedPrompt, strictBudget);
            }

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", openAiProperties.model(),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                        "content", "Extract PC build requirements as strict JSON. Output JSON only with keys: budgetKzt,useCase,workloadType,resolutionTarget,refreshRateHz,priorities,constraints. constraints keys: brandCpu,brandGpu,rgb,caseSize,wifiRequired."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", "Prompt: " + preprocessedPrompt + "\\nCurrency: " + currency + "\\nRegion: " + region + "\\nStrictBudget: " + strictBudget
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

                BigDecimal promptBudget = extractBudgetFromPrompt(preprocessedPrompt);
            BigDecimal aiBudget = extractAiBudgetKzt(intentNode, promptBudget);

            return new BuildResponse.RequirementsDto(
                    aiBudget.max(promptBudget),
                    normalizeUseCase(intentNode.path("useCase").asText("mixed"), preprocessedPrompt),
                    normalizeWorkload(intentNode.path("workloadType").asText("mixed"), preprocessedPrompt),
                    normalizeResolution(intentNode.path("resolutionTarget").asText("1080p"), preprocessedPrompt),
                    normalizeRefreshRate(intentNode.path("refreshRateHz"), preprocessedPrompt),
                    normalizePriorities(intentNode.path("priorities"), preprocessedPrompt),
                    new BuildResponse.ConstraintsDto(
                            textOrNull(intentNode.path("constraints").path("brandCpu")),
                            textOrNull(intentNode.path("constraints").path("brandGpu")),
                            intentNode.path("constraints").path("rgb").asBoolean(false),
                            textOrNull(intentNode.path("constraints").path("caseSize")),
                            intentNode.path("constraints").path("wifiRequired").asBoolean(false)
                    ),
                    region == null || region.isBlank() ? "KZ" : region,
                    Boolean.TRUE.equals(strictBudget),
                    false
            );
        } catch (Exception exception) {
            return requirementsFromPrompt(prompt, strictBudget);
        }
    }

    private BuildResponse.RequirementsDto requirementsFromPrompt(String prompt, Boolean strictBudget) {
        String normalized = preprocessPrompt(prompt);

        BigDecimal budget = extractBudgetFromPrompt(prompt);
        if (budget == null || budget.signum() <= 0 || DEFAULT_BUDGET.compareTo(budget) == 0) {
            if (normalized.contains("low-end") || normalized.contains("budget") || normalized.contains("cheap")) {
                budget = new BigDecimal("400000");
            } else if (normalized.contains("high-end")
                    || normalized.contains("ultra")
                    || normalized.contains("max settings")
                    || normalized.contains("path tracing")
                    || normalized.contains("pathtracing")
                    || normalized.contains("ray tracing")) {
                budget = new BigDecimal("900000");
            } else {
                budget = DEFAULT_BUDGET;
            }
        }

        return new BuildResponse.RequirementsDto(
                budget,
                inferUseCase(normalized),
                inferWorkload(normalized),
                inferResolutionHint(normalized),
                inferRefreshRate(normalized),
                buildPrioritiesFromPrompt(normalized),
                new BuildResponse.ConstraintsDto(null, null, false, null, false),
                "KZ",
                Boolean.TRUE.equals(strictBudget),
            false
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
        String requestUri = UriComponentsBuilder.fromHttpUrl(partServiceUrl + path)
                .queryParam("page", 0)
                .queryParam("size", size)
                .toUriString();
        try {
            String response = restClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            // Spring Data Page -> { "content": [ ... ] }. Some controllers return a bare array [ ... ].
            JsonNode content = root.isArray() ? root : root.path("content");
            if (!content.isArray()) {
                List<String> keys = new ArrayList<>();
                root.fieldNames().forEachRemaining(keys::add);
                log.warn(
                        "part-service JSON is not a paged object or array: path={} uri={} rootIsArray={} rootKeys={}",
                        path,
                        requestUri,
                        root.isArray(),
                        keys);
                return List.of();
            }

            List<BuildResponse.PartDto> parts = new ArrayList<>();
            for (JsonNode node : content) {
                parts.add(mapper.map(node));
            }
            List<BuildResponse.PartDto> deduped = deduplicateByName(parts);
            if (log.isDebugEnabled()) {
                log.debug("part-service fetch ok: path={} rawRows={} afterDedupe={}", path, content.size(), deduped.size());
            }
            return deduped;
        } catch (Exception exception) {
            log.warn("part-service fetch failed: path={} uri={} — {}", path, requestUri, exception.toString());
            if (log.isDebugEnabled()) {
                log.debug("part-service fetch stack trace", exception);
            }
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
        int candidateSocket = candidate.socket() != null && !candidate.socket().isBlank() ? 1 : 0;
        int existingSocket = existing.socket() != null && !existing.socket().isBlank() ? 1 : 0;
        if (candidateSocket != existingSocket) {
            return candidateSocket > existingSocket;
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
        // Stock checking removed: return original candidate list without trust flag
        return new StockFilterResult(parts, false);
    }

    private BuildResponse.PartDto toCpuPart(JsonNode node, FallbackUsageTracker tracker, ResolutionTarget resolutionTarget) {
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
        int performanceScore = cpuPerformanceScore(name, resolutionTarget, scoreTier);
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                socketResolution.value(),
                null,
                wattageResolution.value(),
                null,
            performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private BuildResponse.PartDto toGpuPart(JsonNode node, FallbackUsageTracker tracker, ResolutionTarget resolutionTarget) {
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
        int performanceScore = gpuPerformanceScore(name, resolutionTarget, scoreTier);
        String chipset = readText(node, "chipset", "gpuChipset", "family", "model");
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
                wattageResolution.value(),
                chipset,
                performanceScore,
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
        String memoryType = normalizeDdrLabel(readText(node, "ddrType", "memoryType", "ramType"));
        if (memoryType == null || memoryType.isBlank()) {
            HardwareFallbackResolver.ResolvedValue<String> memoryResolution = hardwareFallbackResolver.resolveMemoryTypeFromName(name);
            memoryType = normalizeDdrLabel(memoryResolution.value());
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
        Integer normalizedCapacity = readInteger(node, "capacityGb", "storageCapacityGb");
        return new BuildResponse.PartDto(
                partId,
                name,
                kztFromNode(node.path("priceKzt")),
                null,
                null,
            normalizedCapacity == null ? extractStorageCapacity(name) : normalizedCapacity,
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

    private BuildResponse.PartDto toCpuDbPart(JsonNode node, FallbackUsageTracker tracker, ResolutionTarget resolutionTarget) {
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
        int performanceScore = cpuPerformanceScore(name, resolutionTarget, scoreTier);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                socketResolution.value(),
                null,
                wattageResolution.value(),
                textOrNull(node.path("graphics")),
            performanceScore,
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
        int performanceScore = gpuPerformanceScore(name, ResolutionTarget.P1080, scoreTier);
        return new BuildResponse.PartDto(
                partId,
                name,
                priceUsdToKzt(node.path("priceUsd")),
                null,
                null,
                wattageResolution.value(),
                textOrNull(node.path("chipset")),
                performanceScore,
                scoreTier.tierLabel,
                parseStock(node)
        );
    }

    private int cpuPerformanceScore(String cpuName, ResolutionTarget resolutionTarget, ScoreTier fallbackScore) {
        CpuBenchmark benchmark = cpuBenchmarkService.findByName(cpuName).orElse(null);
        double score = benchmark != null
                ? getCpuPerformanceScore(benchmark, resolutionTarget)
                : inferCpuScore(fallbackScore);
        return (int) Math.round(score);
    }

    private double getCpuPerformanceScore(CpuBenchmark cpu, ResolutionTarget resolutionTarget) {
        if (cpu == null) {
            return 0.0;
        }

        ResolutionTarget target = resolutionTarget == null ? ResolutionTarget.P1080 : resolutionTarget;
        return switch (target) {
            case P1080 -> cpu.getScore1080p() == null ? 0.0 : cpu.getScore1080p();
            case P1440 -> cpu.getScore1440p() == null ? 0.0 : cpu.getScore1440p();
            case P4K -> cpu.getScore1440p() == null ? 0.0 : cpu.getScore1440p();
        };
    }

    private double inferCpuScore(ScoreTier fallbackScore) {
        if (fallbackScore != null && fallbackScore.performanceScore > 0) {
            return fallbackScore.performanceScore;
        }
        return scoreFromTier(Tier.MID);
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

    private BuildResponse.PartDto toCpuMatchPart(JsonNode node, FallbackUsageTracker tracker, ResolutionTarget resolutionTarget) {
        String name = node.path("name").asText("CPU");
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> socketResolution = hardwareFallbackResolver.resolveCpuSocket(readText(node, "socket"), name);
        if (socketResolution.fallbackUsed()) tracker.increment();
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveCpuTierLabel(name);
        if (tierResolution.fallbackUsed()) tracker.increment();
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = node.path("tdp").isNumber()
            ? new HardwareFallbackResolver.ResolvedValue<>(node.path("tdp").intValue(), false)
            : hardwareFallbackResolver.resolveCpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) tracker.increment();
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "cpu", inferredTier, tracker);
        int performanceScore = cpuPerformanceScore(name, resolutionTarget, scoreTier);
        return new BuildResponse.PartDto(
            partId,
            name,
            // price is stored in KZT on matched records
            kztFromNode(node.has("priceKzt") ? node.path("priceKzt") : node.path("price_kzt")),
            socketResolution.value(),
            null,
            wattageResolution.value(),
            textOrNull(node.path("graphics")),
            performanceScore,
            scoreTier.tierLabel,
            parseStock(node)
        );
    }

        private BuildResponse.PartDto toGpuMatchPart(JsonNode node, FallbackUsageTracker tracker, ResolutionTarget resolutionTarget) {
        String name = node.path("name").asText("GPU");
        Long partId = node.path("id").asLong();
        HardwareFallbackResolver.ResolvedValue<String> tierResolution = hardwareFallbackResolver.resolveGpuTierLabel(name);
        if (tierResolution.fallbackUsed()) tracker.increment();
        HardwareFallbackResolver.ResolvedValue<Integer> wattageResolution = node.path("tdp").isNumber()
            ? new HardwareFallbackResolver.ResolvedValue<>(node.path("tdp").intValue(), false)
            : hardwareFallbackResolver.resolveGpuWattage(node, name);
        if (wattageResolution.fallbackUsed()) tracker.increment();
        Tier inferredTier = Tier.fromLabel(tierResolution.value());
        ScoreTier scoreTier = resolveScoreTier(partId, "gpu", inferredTier, tracker);
        int performanceScore = gpuPerformanceScore(name, resolutionTarget, scoreTier);
        return new BuildResponse.PartDto(
            partId,
            name,
            kztFromNode(node.has("priceKzt") ? node.path("priceKzt") : node.path("price_kzt")),
            null,
            null,
            wattageResolution.value(),
            textOrNull(node.path("chipset")),
            performanceScore,
            scoreTier.tierLabel,
            parseStock(node)
        );
        }

        private BuildResponse.PartDto toMotherboardMatchPart(JsonNode node, FallbackUsageTracker tracker) {
        String name = node.path("name").asText("Motherboard");
        Long partId = node.path("id").asLong();
        String socket = readText(node, "socket", "mbSocket", "cpuSocket");
        if (socket == null || socket.isBlank()) tracker.increment();
        String memoryType = resolveMotherboardMemoryType(node, name, tracker);
        if (memoryType == null) tracker.increment();
        ScoreTier scoreTier = resolveScoreTier(partId, "motherboard", Tier.MID, tracker);
        return new BuildResponse.PartDto(
            partId,
            name,
            kztFromNode(node.has("priceKzt") ? node.path("priceKzt") : node.path("price_kzt")),
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
        String memoryType = node.path("ddr").isNumber()
                ? "DDR" + node.path("ddr").asInt()
                : normalizeDdrLabel(readText(node, "ddrType", "memoryType", "ramType", "memoryStandard"));
        if (memoryType == null) {
            HardwareFallbackResolver.ResolvedValue<String> memoryResolution = hardwareFallbackResolver.resolveMemoryTypeFromName(name);
            memoryType = normalizeDdrLabel(memoryResolution.value());
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

        tracker.increment();
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

    /** DDR type from part fields, falling back to listing name (matches evaluateChecks). */
    private String effectiveMemoryDdrType(BuildResponse.PartDto memory) {
        if (memory == null) {
            return null;
        }
        String fromField = normalizeDdrLabel(memory.memoryType());
        if (fromField != null && (fromField.equals("DDR3") || fromField.equals("DDR4") || fromField.equals("DDR5"))) {
            return fromField;
        }
        HardwareFallbackResolver.ResolvedValue<String> resolved = hardwareFallbackResolver.resolveMemoryTypeFromName(memory.name());
        return normalizeDdrLabel(resolved.value());
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

        BigDecimal scaledKzt = extractScaledKztAmount(prompt);
        if (scaledKzt != null) {
            return scaledKzt.max(new BigDecimal("150000"));
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

    private BigDecimal extractScaledKztAmount(String prompt) {
        Matcher matcher = EXPLICIT_KZT_SCALED_PATTERN.matcher(prompt);
        BigDecimal best = null;

        while (matcher.find()) {
            try {
                BigDecimal amount = new BigDecimal(matcher.group(1));
                String unit = matcher.group(2).toLowerCase(Locale.ROOT);

                BigDecimal multiplier;
                if ("m".equals(unit) || "млн".equals(unit)) {
                    multiplier = new BigDecimal("1000000");
                } else {
                    multiplier = new BigDecimal("1000");
                }

                BigDecimal value = amount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
                if (best == null || value.compareTo(best) > 0) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return best;
    }

    private String readText(JsonNode node, String... fieldNames) {
        JsonNode normalizedNode = normalizedSpecsNode(node);
        if (normalizedNode != null) {
            for (String fieldName : fieldNames) {
                JsonNode valueNode = normalizedNode.path(fieldName);
                if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                    String value = valueNode.asText("").trim();
                    if (!value.isBlank()) {
                        return value;
                    }
                }
            }
        }

        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("").trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... fieldNames) {
        JsonNode normalizedNode = normalizedSpecsNode(node);
        if (normalizedNode != null) {
            for (String fieldName : fieldNames) {
                JsonNode valueNode = normalizedNode.path(fieldName);
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
        }

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

    private JsonNode normalizedSpecsNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode normalizedJsonNode = node.path("normalizedSpecsJson");
        if (normalizedJsonNode == null || normalizedJsonNode.isMissingNode() || normalizedJsonNode.isNull() || normalizedJsonNode.asText("").isBlank()) {
            normalizedJsonNode = node.path("normalized_specs_json");
        }

        if (normalizedJsonNode == null || normalizedJsonNode.isMissingNode() || normalizedJsonNode.isNull()) {
            return null;
        }

        String raw = normalizedJsonNode.asText("").trim();
        if (raw.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String powerEstimateConfidence(BuildResponse.PartDto cpu, BuildResponse.PartDto gpu) {
        boolean cpuKnown = cpu != null && cpu.wattage() != null && cpu.wattage() > 0;
        boolean gpuKnown = gpu != null && gpu.wattage() != null && gpu.wattage() > 0;
        if (cpuKnown && gpuKnown) {
            return "medium";
        }
        return "low";
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
        if (normalizedPrompt == null || normalizedPrompt.isBlank()) {
            return "mixed";
        }

        if (normalizedPrompt.contains("game")
                || normalizedPrompt.contains("gaming")
                || normalizedPrompt.contains("cyberpunk")
                || normalizedPrompt.contains("esports")
                || normalizedPrompt.contains("path tracing")
                || normalizedPrompt.contains("pathtracing")
                || normalizedPrompt.contains("ray tracing")) {
            return "gaming";
        }

        if (normalizedPrompt.contains("office")
                || normalizedPrompt.contains("work")
                || normalizedPrompt.contains("coding")
                || normalizedPrompt.contains("programming")) {
            return "work";
        }

        return "mixed";
    }

    private ResolutionTarget inferResolutionHint(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return ResolutionTarget.P1080;
        }

        if (normalized.contains("4k")) {
            return ResolutionTarget.P4K;
        }
        if (normalized.contains("1440")) {
            return ResolutionTarget.P1440;
        }

        if (normalized.contains("path tracing")
                || normalized.contains("pathtracing")
                || normalized.contains("ray tracing")
                || normalized.contains("high-end")
                || normalized.contains("ultra")
                || normalized.contains("max settings")) {
            return ResolutionTarget.P1440;
        }

        return ResolutionTarget.P1080;
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

    private ResolutionTarget normalizeResolution(String aiResolution, String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        ResolutionTarget hint = inferResolutionHint(normalizedPrompt);
        if (hint != ResolutionTarget.P1080) {
            return hint;
        }
        if (aiResolution == null || aiResolution.isBlank()) {
            return hint;
        }
        String normalized = aiResolution.toLowerCase(Locale.ROOT);
        if (normalized.contains("1440")) {
            return ResolutionTarget.P1440;
        }
        if (normalized.contains("4k")) {
            return ResolutionTarget.P4K;
        }
        return ResolutionTarget.P1080;
    }

    private WorkloadType inferWorkload(String normalizedPrompt) {
        if (normalizedPrompt == null || normalizedPrompt.isBlank()) {
            return WorkloadType.MIXED;
        }
        if (normalizedPrompt.contains("esport") || normalizedPrompt.contains("competitive") || normalizedPrompt.contains("valorant") || normalizedPrompt.contains("cs2")) {
            return WorkloadType.ESPORTS;
        }
        if (normalizedPrompt.contains("aaa")
                || normalizedPrompt.contains("single player")
                || normalizedPrompt.contains("ultra")
                || normalizedPrompt.contains("ray tracing")
                || normalizedPrompt.contains("path tracing")
                || normalizedPrompt.contains("4k")
                || normalizedPrompt.contains("1440")) {
            return WorkloadType.AAA;
        }
        return WorkloadType.fromText(normalizedPrompt);
    }

    private WorkloadType normalizeWorkload(String aiWorkload, String prompt) {
        WorkloadType promptWorkload = inferWorkload(prompt == null ? null : prompt.toLowerCase(Locale.ROOT));
        if (promptWorkload != WorkloadType.MIXED) {
            return promptWorkload;
        }
        WorkloadType aiParsed = WorkloadType.fromText(aiWorkload);
        return aiParsed == WorkloadType.MIXED ? WorkloadType.MIXED : aiParsed;
    }

    private Integer inferRefreshRate(String normalizedPrompt) {
        if (normalizedPrompt == null || normalizedPrompt.isBlank()) {
            return 60;
        }

        Matcher refreshMatcher = Pattern.compile("(?i)(\\d{2,3})\\s*hz").matcher(normalizedPrompt);
        if (refreshMatcher.find()) {
            try {
                return Integer.parseInt(refreshMatcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        if (normalizedPrompt.contains("esport") || normalizedPrompt.contains("competitive") || normalizedPrompt.contains("high refresh")) {
            return 240;
        }
        if (normalizedPrompt.contains("pure gaming") || normalizedPrompt.contains("max fps") || normalizedPrompt.contains("gaming performance")) {
            return 144;
        }
        if (normalizedPrompt.contains("144hz") || normalizedPrompt.contains("165hz") || normalizedPrompt.contains("240hz")) {
            return 144;
        }
        if (normalizedPrompt.contains("4k") || normalizedPrompt.contains("ultra") || normalizedPrompt.contains("max settings")) {
            return 60;
        }
        return 60;
    }

    private Integer normalizeRefreshRate(JsonNode refreshNode, String prompt) {
        if (refreshNode != null && refreshNode.canConvertToInt()) {
            int value = refreshNode.intValue();
            return value > 0 ? value : inferRefreshRate(prompt == null ? null : prompt.toLowerCase(Locale.ROOT));
        }
        if (refreshNode != null) {
            String raw = refreshNode.asText("");
            if (!raw.isBlank()) {
                Matcher matcher = Pattern.compile("(\\d{2,3})").matcher(raw);
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return inferRefreshRate(prompt == null ? null : prompt.toLowerCase(Locale.ROOT));
    }

    private double budgetPressure(BigDecimal budget) {
        if (budget == null || budget.signum() <= 0) {
            return 0.0;
        }
        if (budget.compareTo(new BigDecimal("350000")) < 0) {
            return 1.0;
        }
        if (budget.compareTo(new BigDecimal("550000")) < 0) {
            return 0.70;
        }
        if (budget.compareTo(new BigDecimal("850000")) < 0) {
            return 0.35;
        }
        return 0.10;
    }

    private double refreshPressure(int refreshRateHz) {
        if (refreshRateHz >= 240) {
            return 1.0;
        }
        if (refreshRateHz >= 165) {
            return 0.85;
        }
        if (refreshRateHz >= 144) {
            return 0.75;
        }
        if (refreshRateHz >= 120) {
            return 0.55;
        }
        if (refreshRateHz >= 75) {
            return 0.25;
        }
        return 0.10;
    }

    private double normalizeWeight(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String resolutionLabel(ResolutionTarget resolutionTarget) {
        return resolutionTarget == null ? ResolutionTarget.P1080.label() : resolutionTarget.label();
    }

    private int gpuPerformanceScore(String gpuName, ResolutionTarget resolutionTarget, ScoreTier fallbackScore) {
        GpuBenchmark benchmark = gpuBenchmarkService.findByName(gpuName).orElse(null);
        double score = benchmark != null
                ? getGpuPerformanceScore(benchmark, resolutionTarget)
                : inferGpuScore(gpuName, fallbackScore);
        return (int) Math.round(score);
    }

    private double getGpuPerformanceScore(GpuBenchmark gpu, ResolutionTarget resolutionTarget) {
        if (gpu == null) {
            return 0.0;
        }
        ResolutionTarget target = resolutionTarget == null ? ResolutionTarget.P1080 : resolutionTarget;
        return switch (target) {
            case P1080 -> gpu.getScore1080p() == null ? 0.0 : gpu.getScore1080p();
            case P1440 -> gpu.getScore1440p() == null ? 0.0 : gpu.getScore1440p();
            case P4K -> gpu.getScore4k() == null ? 0.0 : gpu.getScore4k();
        };
    }

    private double inferGpuScore(String gpuName, ScoreTier fallbackScore) {
        if (fallbackScore != null && fallbackScore.performanceScore > 0) {
            return fallbackScore.performanceScore;
        }
        String tierLabel = hardwareFallbackResolver.resolveGpuTierLabel(gpuName).value();
        return scoreFromTier(Tier.fromLabel(tierLabel));
    }

    private List<String> buildPrioritiesFromPrompt(String normalizedPrompt) {
        List<String> priorities = new ArrayList<>();
        if (normalizedPrompt.contains("low-end")
                || normalizedPrompt.contains("budget")
                || normalizedPrompt.contains("cheap")
                || normalizedPrompt.contains("value")) {
            priorities.add("value");
        }

        if (normalizedPrompt.contains("performance")
                || normalizedPrompt.contains("fps")
                || normalizedPrompt.contains("gaming")
                || normalizedPrompt.contains("game")
                || normalizedPrompt.contains("cyberpunk")
                || normalizedPrompt.contains("path tracing")
                || normalizedPrompt.contains("pathtracing")
                || normalizedPrompt.contains("ray tracing")
                || normalizedPrompt.contains("max settings")
                || normalizedPrompt.contains("ultra")) {
            priorities.add("performance");
        }

        if (normalizedPrompt.contains("upgrade")) {
            priorities.add("upgrade");
        }

        if (priorities.isEmpty()) {
            priorities.add("value");
            priorities.add("performance");
        }
        return List.copyOf(priorities);
    }

    private List<String> normalizePriorities(JsonNode prioritiesNode, String prompt) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();

        if (prioritiesNode != null && prioritiesNode.isArray()) {
            for (JsonNode node : prioritiesNode) {
                String priority = node == null ? null : node.asText();
                if (priority == null || priority.isBlank()) {
                    continue;
                }
                merged.add(priority.trim().toLowerCase(Locale.ROOT));
            }
        }

        for (String inferred : buildPrioritiesFromPrompt(prompt == null ? "" : prompt.toLowerCase(Locale.ROOT))) {
            merged.add(inferred.toLowerCase(Locale.ROOT));
        }

        if (merged.isEmpty()) {
            merged.add("value");
            merged.add("performance");
        }

        return List.copyOf(merged);
    }

    private String preprocessPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return prompt.toLowerCase(Locale.ROOT)
                .replace("fullhd", "1080p")
                .replace("fhd", "1080p")
                .replace("qhd", "1440p")
                .replace("uhd", "4k")
                .replace("2k", "1440p");
    }

    /**
     * Minimum hardware floors when listing metadata can be parsed from names.
     * Entry 1080p budgets use relaxed floors so budget gaming builds can clear compatibility checks.
     */
    private record GamingHardwareFloors(int minRamGb, int minGpuVramGb, int minStorageGb) {
    }

    private GamingHardwareFloors gamingHardwareFloors(BuildResponse.RequirementsDto requirements) {
        ResolutionTarget res = requirements.resolutionTarget() != null
                ? requirements.resolutionTarget()
                : ResolutionTarget.P1080;
        boolean highRes = res == ResolutionTarget.P1440 || res == ResolutionTarget.P4K;
        BigDecimal budget = requirements.budgetKzt();
        BigDecimal effective = budget == null || budget.signum() <= 0 ? DEFAULT_BUDGET : budget;
        String band = resolveBudgetBand(effective);

        if (highRes) {
            return new GamingHardwareFloors(16, 8, 500);
        }
        boolean entryLike = "entry".equals(band) || effective.compareTo(new BigDecimal("350000")) < 0;
        if (entryLike) {
            return new GamingHardwareFloors(8, 4, 250);
        }
        return new GamingHardwareFloors(16, 8, 500);
    }

    private boolean meetsGamingMinimums(
            BuildResponse.RequirementsDto requirements,
            BuildResponse.PartDto gpu,
            BuildResponse.PartDto memory,
            BuildResponse.PartDto storage
    ) {
        if (requirements == null || requirements.useCase() == null || !"gaming".equalsIgnoreCase(requirements.useCase())) {
            return true;
        }

        GamingHardwareFloors floors = gamingHardwareFloors(requirements);
        int gpuVramGb = extractGpuVramGb(gpu == null ? null : gpu.name());
        int ramGb = extractMemoryCapacityGb(memory == null ? null : memory.name());
        int storageGb = storage == null
                ? 0
                : Math.max(safeInt(storage.wattage(), 0), safeInt(extractStorageCapacity(storage.name()), 0));
        String memoryType = normalize(memory == null ? null : memory.memoryType());

        if (memoryType.contains("ddr3")) {
            return false;
        }
        if (gpuVramGb > 0 && gpuVramGb < floors.minGpuVramGb()) {
            return false;
        }
        if (ramGb > 0 && ramGb < floors.minRamGb()) {
            return false;
        }
        return storageGb <= 0 || storageGb >= floors.minStorageGb();
    }

    private int extractGpuVramGb(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*gb", Pattern.CASE_INSENSITIVE).matcher(text);
        int best = 0;
        while (matcher.find()) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                if (value > best) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private int extractMemoryCapacityGb(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher kits = Pattern.compile("(\\d{1,2})\\s*[xх]\\s*(\\d{1,2})\\s*gb", Pattern.CASE_INSENSITIVE).matcher(text);
        if (kits.find()) {
            try {
                return Integer.parseInt(kits.group(1)) * Integer.parseInt(kits.group(2));
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher single = Pattern.compile("(\\d{1,3})\\s*gb", Pattern.CASE_INSENSITIVE).matcher(text);
        int best = 0;
        while (single.find()) {
            try {
                int value = Integer.parseInt(single.group(1));
                if (value > best) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
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

            private record IntentScoringProfile(
                double gpuWeight,
                double cpuWeight,
                double ramWeight,
                double storageWeight,
                double motherboardWeight,
                double psuWeight,
                double caseWeight,
                double valueWeight,
                double rawPerformanceWeight,
                double upgradeWeight,
                double efficiencyWeight,
                double aestheticWeight,
                double noiseWeight,
                double bottleneckPenaltyWeight,
                double budgetEfficiencyWeight,
                double gpuMinShare,
                double gpuMaxShare,
                double cpuMinShare,
                double cpuMaxShare,
                double ramMinShare,
                double ramMaxShare,
                double motherboardMaxShare
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
