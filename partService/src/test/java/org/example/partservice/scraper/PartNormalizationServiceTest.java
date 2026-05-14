package org.example.partservice.scraper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PartNormalizationServiceTest {

    @Test
    void normalizeGpuExtractsBrandAndVram() {
        PartNormalizationService svc = new PartNormalizationService();
        ShopProduct product = new ShopProduct("ASUS TUF RTX 3060 12GB", null, "", "", "");

        NormalizedPartResult res = svc.normalize("shop.kz", "gpu", product);
        assertNotNull(res);
        assertFalse(res.normalizedName().isBlank());
        assertTrue(res.confidenceScore().doubleValue() >= 0.45);
        String json = res.normalizedSpecsJson();
        assertTrue(json.contains("vramGb") || json.toLowerCase().contains("vram"));
    }
}
