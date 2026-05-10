package org.example.partservice.scraper;

import lombok.AllArgsConstructor;
import org.example.partservice.model.AbstractParsedListing;
import org.example.partservice.model.ParsedCpu;
import org.example.partservice.model.ParsedCpuCooler;
import org.example.partservice.model.ParsedInternalHardDrive;
import org.example.partservice.model.ParsedMemory;
import org.example.partservice.model.ParsedMotherboard;
import org.example.partservice.model.ParsedPcCase;
import org.example.partservice.model.ParsedPowerSupply;
import org.example.partservice.model.ParsedVideoCard;
import org.example.partservice.repository.ParsedCpuRepository;
import org.example.partservice.repository.ParsedCpuCoolerRepository;
import org.example.partservice.repository.ParsedInternalHardDriveRepository;
import org.example.partservice.repository.ParsedMemoryRepository;
import org.example.partservice.repository.ParsedMotherboardRepository;
import org.example.partservice.repository.ParsedPcCaseRepository;
import org.example.partservice.repository.ParsedPowerSupplyRepository;
import org.example.partservice.repository.ParsedVideoCardRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ShopScraperService {
    private ParsedCpuRepository parsedCpuRepository;
    private ParsedCpuCoolerRepository parsedCpuCoolerRepository;
    private ParsedVideoCardRepository parsedVideoCardRepository;
    private ParsedPowerSupplyRepository parsedPowerSupplyRepository;
    private ParsedPcCaseRepository parsedPcCaseRepository;
    private ParsedMemoryRepository parsedMemoryRepository;
    private ParsedInternalHardDriveRepository parsedInternalHardDriveRepository;
    private ParsedMotherboardRepository parsedMotherboardRepository;
    private PartNormalizationService partNormalizationService;

    public void scrapePart(AllowlistPart part) {
        try {
            long delayMs = 10000 + (int) (Math.random() * 10000);
            System.out.println(String.format("[SHOP SCRAPER] Waiting %d ms before scraping %s...", delayMs, part.getPartName()));
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(String.format("Scraping shop.kz for: %s (%s)", part.getPartName(), part.getPartType()));

        List<ShopProduct> shopResults = ShopProduct.searchShop(part.getPartName(), part.getPartType());

        if (shopResults.isEmpty()) {
            System.out.println(String.format("No shop.kz results for %s", part.getPartName()));
            return;
        }

        saveScrapedListings(part.getPartType(), part.getPartName(), shopResults);
    }

    /**
     * Scrape using search strategies instead of allowlist.
     * Loads strategies from search-strategies.json and executes each query.
     */
    public void scrapeUsingStrategies() {
        List<SearchStrategy> strategies = SearchStrategyLoader.loadSearchStrategies();

        if (strategies.isEmpty()) {
            System.out.println("[SHOP SCRAPER] No search strategies loaded");
            return;
        }

        int totalResults = 0;

        for (SearchStrategy strategy : strategies) {
            System.out.println("\n[SHOP SCRAPER] Processing strategy: " + strategy);
            List<String> queries = strategy.getQueries();

            if (queries == null || queries.isEmpty()) {
                System.out.println("[SHOP SCRAPER] No queries in strategy, skipping");
                continue;
            }

            for (String query : queries) {
                totalResults += scrapeQuery(strategy, query);
            }
        }

        System.out.println(String.format("\n[SHOP SCRAPER] Completed scraping. Total results saved: %d", totalResults));
    }

    /**
     * Execute a single search query within a strategy context
     */
    private int scrapeQuery(SearchStrategy strategy, String query) {
        try {
            // Random delay between queries
            long delayMs = 5000 + (int) (Math.random() * 5000);
            System.out.println(String.format("[SHOP SCRAPER] Waiting %d ms before searching '%s'...", delayMs, query));
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }

        System.out.println(String.format("[SHOP SCRAPER] Searching '%s' for %s...", query, strategy.getPartType()));

        List<ShopProduct> shopResults = ShopProduct.searchShop(query, strategy.getPartType());

        if (shopResults.isEmpty()) {
            System.out.println(String.format("[SHOP SCRAPER] No results for query '%s'", query));
            return 0;
        }

        // Apply price filtering if budget constraints are set
        List<ShopProduct> filteredResults = applyPriceFilter(shopResults, strategy.getMinPrice(), strategy.getMaxPrice());

        if (filteredResults.isEmpty()) {
            System.out.println(String.format("[SHOP SCRAPER] No results after price filtering for query '%s'", query));
            return 0;
        }

        return saveScrapedListingsFromStrategy(strategy.getPartType(), query, filteredResults);
    }

    /**
     * Apply price filtering to search results
     */
    private List<ShopProduct> applyPriceFilter(List<ShopProduct> results, BigDecimal minPrice, BigDecimal maxPrice) {
        return results.stream()
                .filter(product -> {
                    if (product.getPriceKzt() == null) {
                        return true; // Include items with no price
                    }
                    if (minPrice != null && product.getPriceKzt().compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && product.getPriceKzt().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void saveScrapedListings(String partType, String allowlistPartName, List<ShopProduct> shopResults) {
        String normalizedPartType = normalizePartType(partType);

        int savedCount = 0;
        for (ShopProduct product : shopResults) {
            if (product == null || product.getUrl() == null || product.getUrl().isBlank()) {
                continue;
            }

            String productName = product.getName() == null || product.getName().isBlank()
                ? allowlistPartName
                : product.getName().trim();
            BigDecimal price = product.getPriceKzt() == null ? BigDecimal.ZERO : product.getPriceKzt();

            NormalizedPartResult normalizedPart = partNormalizationService.normalize("shop.kz", normalizedPartType, product);

            if (saveIntoParsedTable(normalizedPartType, productName, price, product.getUrl(), normalizedPart)) {
                savedCount++;
            }
        }

        System.out.println(String.format("Saved %d shop.kz listings for allowlist item: %s", savedCount, allowlistPartName));
    }

    /**
     * Save scraped listings from a search strategy query
     */
    private int saveScrapedListingsFromStrategy(String partType, String searchQuery, List<ShopProduct> shopResults) {
        String normalizedPartType = normalizePartType(partType);

        int savedCount = 0;
        for (ShopProduct product : shopResults) {
            if (product == null || product.getUrl() == null || product.getUrl().isBlank()) {
                continue;
            }

            String productName = product.getName() == null || product.getName().isBlank()
                ? searchQuery
                : product.getName().trim();
            BigDecimal price = product.getPriceKzt() == null ? BigDecimal.ZERO : product.getPriceKzt();

            NormalizedPartResult normalizedPart = partNormalizationService.normalize("shop.kz", normalizedPartType, product);

            if (saveIntoParsedTable(normalizedPartType, productName, price, product.getUrl(), normalizedPart)) {
                savedCount++;
            }
        }

        System.out.println(String.format("[SHOP SCRAPER] Saved %d results for query '%s' (%s)", savedCount, searchQuery, partType));
        return savedCount;
    }

    private boolean saveIntoParsedTable(String partType, String productName, BigDecimal priceKzt, String url, NormalizedPartResult normalizedPart) {
        LocalDateTime now = LocalDateTime.now();

        switch (partType) {
            case "cpu" -> {
                ParsedCpu cpu = parsedCpuRepository.findFirstByUrl(url).orElseGet(ParsedCpu::new);
                populateListing(cpu, productName, priceKzt, url, normalizedPart, now);
                parsedCpuRepository.save(cpu);
                return true;
            }
            case "cpu_cooler" -> {
                ParsedCpuCooler cooler = parsedCpuCoolerRepository.findFirstByUrl(url).orElseGet(ParsedCpuCooler::new);
                populateListing(cooler, productName, priceKzt, url, normalizedPart, now);
                parsedCpuCoolerRepository.save(cooler);
                return true;
            }
            case "gpu" -> {
                ParsedVideoCard gpu = parsedVideoCardRepository.findFirstByUrl(url).orElseGet(ParsedVideoCard::new);
                populateListing(gpu, productName, priceKzt, url, normalizedPart, now);
                parsedVideoCardRepository.save(gpu);
                return true;
            }
            case "power_supply" -> {
                ParsedPowerSupply psu = parsedPowerSupplyRepository.findFirstByUrl(url).orElseGet(ParsedPowerSupply::new);
                populateListing(psu, productName, priceKzt, url, normalizedPart, now);
                parsedPowerSupplyRepository.save(psu);
                return true;
            }
            case "pc_case" -> {
                ParsedPcCase pcCase = parsedPcCaseRepository.findFirstByUrl(url).orElseGet(ParsedPcCase::new);
                populateListing(pcCase, productName, priceKzt, url, normalizedPart, now);
                parsedPcCaseRepository.save(pcCase);
                return true;
            }
            case "memory", "ram" -> {
                ParsedMemory memory = parsedMemoryRepository.findFirstByUrl(url).orElseGet(ParsedMemory::new);
                populateListing(memory, productName, priceKzt, url, normalizedPart, now);
                parsedMemoryRepository.save(memory);
                return true;
            }
            case "internal_memory", "internal_hard_drive" -> {
                ParsedInternalHardDrive drive = parsedInternalHardDriveRepository.findFirstByUrl(url).orElseGet(ParsedInternalHardDrive::new);
                populateListing(drive, productName, priceKzt, url, normalizedPart, now);
                parsedInternalHardDriveRepository.save(drive);
                return true;
            }
            case "motherboard" -> {
                ParsedMotherboard motherboard = parsedMotherboardRepository.findFirstByUrl(url).orElseGet(ParsedMotherboard::new);
                populateListing(motherboard, productName, priceKzt, url, normalizedPart, now);
                parsedMotherboardRepository.save(motherboard);
                return true;
            }
            default -> {
                System.out.println("[SHOP SCRAPER] Unsupported part type for parsed table save: " + partType);
                return false;
            }
        }
    }

    private void populateListing(AbstractParsedListing listing, String productName, BigDecimal priceKzt, String url, NormalizedPartResult normalizedPart, LocalDateTime now) {
        listing.setName(productName);
        listing.setPriceKzt(priceKzt);
        listing.setRetailer("shop.kz");
        listing.setCurrency("KZT");
        listing.setUrl(url);
        listing.setLastScraped(now);
        if (normalizedPart != null) {
            listing.setNormalizedName(normalizedPart.normalizedName());
            listing.setNormalizedSpecsJson(normalizedPart.normalizedSpecsJson());
            listing.setSourcePayloadJson(normalizedPart.sourcePayloadJson());
            listing.setConfidenceScore(normalizedPart.confidenceScore());
        }
    }

    private String normalizePartType(String partType) {
        if (partType == null || partType.isBlank()) {
            return "unknown";
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