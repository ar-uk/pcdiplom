package org.example.recommendationservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationEvaluationResponse(
        Integer totalCases,
        Integer compatibilityPassedCases,
        BigDecimal compatibilityPassRatePercent,
        Integer budgetPassedCases,
        BigDecimal budgetPassRatePercent,
        Long avgLatencyMs,
        Integer fallbackCases,
        BigDecimal fallbackRatePercent,
        List<CaseResultDto> cases
) {
    public record CaseResultDto(
            String prompt,
            Boolean compatibilityPassed,
            Boolean budgetOk,
            Long latencyMs,
            Boolean fallbackUsed,
            Integer warningCount,
            String budgetBand,
            String topVariant
    ) {
    }
}
