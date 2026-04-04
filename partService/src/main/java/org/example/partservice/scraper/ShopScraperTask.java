package org.example.partservice.scraper;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;

@Component
@AllArgsConstructor
public class ShopScraperTask {
    private ShopScraperService shopScraperService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledShopScrape() {
        System.out.println("[SHOP SCRAPER] Starting scheduled scrape...");
        scrapeAllowlistFromShop(null, null);
    }

    public void scrapeAllowlistFromShop(Integer limit, String onlyPartType) {
        try {
            System.out.println("[SHOP SCRAPER] Loading allowlist from CSV files...");
            List<AllowlistPart> allowlist = AllowlistLoader.loadAllowlist();

            if (allowlist.isEmpty()) {
                System.err.println("[SHOP SCRAPER] Allowlist is empty!");
                return;
            }

            if (onlyPartType != null && !onlyPartType.isBlank()) {
                String normalizedType = canonicalPartType(onlyPartType);
                allowlist = allowlist.stream()
                    .filter(p -> normalizedType.equals(canonicalPartType(p.getPartType())))
                    .toList();
            }

            if (limit != null && limit > 0 && allowlist.size() > limit) {
                allowlist = allowlist.subList(0, limit);
            }

            System.out.println(String.format("[SHOP SCRAPER] Loaded %d allowed parts", allowlist.size()));
            System.out.println("[SHOP SCRAPER] Starting to scrape shop.kz for each part...");

            int successCount = 0;
            for (AllowlistPart part : allowlist) {
                try {
                    shopScraperService.scrapePart(part);
                    successCount++;
                } catch (Exception e) {
                    System.err.println(String.format("[SHOP SCRAPER] Error scraping %s: %s", part.getPartName(), e.getMessage()));
                }
            }

            System.out.println(String.format("[SHOP SCRAPER] Completed! Successfully processed %d/%d parts",
                successCount, allowlist.size()));
        } catch (Exception e) {
            System.err.println("[SHOP SCRAPER] Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void scrapeAllowlistFromShop() {
        scrapeAllowlistFromShop(null, null);
    }

    private String canonicalPartType(String partType) {
        if (partType == null || partType.isBlank()) {
            return "";
        }

        String normalized = partType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "psu", "power-supply", "power supply" -> "power_supply";
            case "ram" -> "memory";
            case "storage", "internal-hard-drive", "internal_hard_drive" -> "internal_memory";
            case "case", "pc-case", "pc case" -> "pc_case";
            case "cooling", "cooler", "cpu-cooler", "cpu cooler" -> "cpu_cooler";
            default -> normalized;
        };
    }
}