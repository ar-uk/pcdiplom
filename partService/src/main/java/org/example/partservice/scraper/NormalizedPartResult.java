package org.example.partservice.scraper;

import java.math.BigDecimal;

public record NormalizedPartResult(
    String normalizedName,
    String normalizedSpecsJson,
    String sourcePayloadJson,
    BigDecimal confidenceScore
) {
}