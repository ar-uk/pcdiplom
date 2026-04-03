package org.example.recommendationservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BuildResponse(
        String sessionId,
        IntentDto intent,
        Map<String, PartDto> build,
        Map<String, List<PartOptionDto>> options,
        TotalsDto totals,
        ChecksDto checks,
        List<String> reasoning,
        List<AlternativeDto> alternatives,
        List<String> marketInsights
) {
    public record IntentDto(
                        BigDecimal budgetKzt,
            String useCase,
            String targetResolution,
            List<String> priorities,
            ConstraintsDto constraints
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
            String chipset
    ) {
    }

    public record TotalsDto(
            BigDecimal partsTotalKzt,
            Integer estimatedSystemPowerW,
            BigDecimal psuHeadroomPercent
    ) {
    }

    public record ChecksDto(
            Boolean socketCompatible,
            Boolean memoryCompatible,
            Boolean powerHeadroomOk,
            Boolean budgetOk
    ) {
    }

    public record AlternativeDto(
            String label,
            BigDecimal totalKzt,
            String tradeoff
    ) {
    }

    public record PartOptionDto(
            PartDto part,
            BigDecimal score,
            String reason
    ) {
    }
}
