package org.example.recommendationservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BuildResponse(
        String sessionId,
        RequirementsDto requirements,
        String budgetBand,
        List<BuildVariantDto> top3Builds,
        List<String> explanations,
        ChecksDto checks,
        MetricsDto metrics,
        List<String> warnings
) {
    public record RequirementsDto(
            BigDecimal budgetKzt,
            String useCase,
            String targetResolution,
            List<String> priorities,
            ConstraintsDto constraints,
            String market,
            Boolean strictBudget,
            Boolean strictStockOnly
    ) {
    }

    public record ConstraintsDto(
            String brandCpu,
            String brandGpu,
            Boolean rgb,
            String caseSize,
            Boolean wifiRequired
    ) {
    }

    public record PartDto(
            Long id,
            String name,
            BigDecimal priceKzt,
            String socket,
            String memoryType,
            Integer wattage,
            String chipset,
            Integer performanceScore,
            String tierLabel,
            String stockStatus
    ) {
    }

    public record BuildVariantDto(
            String label,
            Map<String, PartDto> parts,
            TotalsDto totals,
            BigDecimal score,
            List<String> tradeoffs,
            ChecksDto checks
    ) {
    }

    public record TotalsDto(
            BigDecimal partsTotalKzt,
            Integer estimatedSystemPowerW,
            BigDecimal psuHeadroomPercent
    ) {
    }

    public record ChecksDto(
            Boolean compatibilityPassed,
            Boolean socketCompatible,
            Boolean memoryCompatible,
            Boolean psuMinimumHeadroomPassed,
            Boolean psuPreferredHeadroomPassed,
            Boolean budgetOk,
            Boolean cpuGpuBalanceOk,
            Boolean stockValidated,
            Boolean stockValidationEnforced,
            Boolean caseFitValidated,
            String powerEstimateConfidence
    ) {
    }

    public record MetricsDto(
            Long latencyMs,
            Integer candidateBuildsEvaluated,
            Integer compatibleBuilds,
            String scoreVersion,
            BigDecimal performanceMappingCoveragePercent,
            Integer fallbackInferenceCount,
            String normalizedDataConfidence
    ) {
    }
}
