package org.example.partservice.scraper;

import lombok.AllArgsConstructor;
import org.example.partservice.model.ParsedCpu;
import org.example.partservice.model.ParsedInternalHardDrive;
import org.example.partservice.model.ParsedMemory;
import org.example.partservice.model.ParsedMotherboard;
import org.example.partservice.model.ParsedPcCase;
import org.example.partservice.model.ParsedPowerSupply;
import org.example.partservice.model.ParsedVideoCard;
import org.example.partservice.repository.ParsedCpuRepository;
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

@Service
@AllArgsConstructor
public class ShopScraperService {
    private ParsedCpuRepository parsedCpuRepository;
    private ParsedVideoCardRepository parsedVideoCardRepository;
    private ParsedPowerSupplyRepository parsedPowerSupplyRepository;
    private ParsedPcCaseRepository parsedPcCaseRepository;
    private ParsedMemoryRepository parsedMemoryRepository;
    private ParsedInternalHardDriveRepository parsedInternalHardDriveRepository;
    private ParsedMotherboardRepository parsedMotherboardRepository;

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

            if (saveIntoParsedTable(normalizedPartType, productName, price, product.getUrl())) {
                savedCount++;
            }
        }

        System.out.println(String.format("Saved %d shop.kz listings for allowlist item: %s", savedCount, allowlistPartName));
    }

    private boolean saveIntoParsedTable(String partType, String productName, BigDecimal priceKzt, String url) {
        LocalDateTime now = LocalDateTime.now();

        switch (partType) {
            case "cpu" -> {
                ParsedCpu cpu = parsedCpuRepository.findFirstByUrl(url).orElseGet(ParsedCpu::new);
                cpu.setName(productName);
                cpu.setPriceKzt(priceKzt);
                cpu.setRetailer("shop.kz");
                cpu.setCurrency("KZT");
                cpu.setUrl(url);
                cpu.setLastScraped(now);
                parsedCpuRepository.save(cpu);
                return true;
            }
            case "gpu" -> {
                ParsedVideoCard gpu = parsedVideoCardRepository.findFirstByUrl(url).orElseGet(ParsedVideoCard::new);
                gpu.setName(productName);
                gpu.setPriceKzt(priceKzt);
                gpu.setRetailer("shop.kz");
                gpu.setCurrency("KZT");
                gpu.setUrl(url);
                gpu.setLastScraped(now);
                parsedVideoCardRepository.save(gpu);
                return true;
            }
            case "power_supply" -> {
                ParsedPowerSupply psu = parsedPowerSupplyRepository.findFirstByUrl(url).orElseGet(ParsedPowerSupply::new);
                psu.setName(productName);
                psu.setPriceKzt(priceKzt);
                psu.setRetailer("shop.kz");
                psu.setCurrency("KZT");
                psu.setUrl(url);
                psu.setLastScraped(now);
                parsedPowerSupplyRepository.save(psu);
                return true;
            }
            case "pc_case" -> {
                ParsedPcCase pcCase = parsedPcCaseRepository.findFirstByUrl(url).orElseGet(ParsedPcCase::new);
                pcCase.setName(productName);
                pcCase.setPriceKzt(priceKzt);
                pcCase.setRetailer("shop.kz");
                pcCase.setCurrency("KZT");
                pcCase.setUrl(url);
                pcCase.setLastScraped(now);
                parsedPcCaseRepository.save(pcCase);
                return true;
            }
            case "memory", "ram" -> {
                ParsedMemory memory = parsedMemoryRepository.findFirstByUrl(url).orElseGet(ParsedMemory::new);
                memory.setName(productName);
                memory.setPriceKzt(priceKzt);
                memory.setRetailer("shop.kz");
                memory.setCurrency("KZT");
                memory.setUrl(url);
                memory.setLastScraped(now);
                parsedMemoryRepository.save(memory);
                return true;
            }
            case "internal_memory", "internal_hard_drive" -> {
                ParsedInternalHardDrive drive = parsedInternalHardDriveRepository.findFirstByUrl(url).orElseGet(ParsedInternalHardDrive::new);
                drive.setName(productName);
                drive.setPriceKzt(priceKzt);
                drive.setRetailer("shop.kz");
                drive.setCurrency("KZT");
                drive.setUrl(url);
                drive.setLastScraped(now);
                parsedInternalHardDriveRepository.save(drive);
                return true;
            }
            case "motherboard" -> {
                ParsedMotherboard motherboard = parsedMotherboardRepository.findFirstByUrl(url).orElseGet(ParsedMotherboard::new);
                motherboard.setName(productName);
                motherboard.setPriceKzt(priceKzt);
                motherboard.setRetailer("shop.kz");
                motherboard.setCurrency("KZT");
                motherboard.setUrl(url);
                motherboard.setLastScraped(now);
                parsedMotherboardRepository.save(motherboard);
                return true;
            }
            default -> {
                System.out.println("[SHOP SCRAPER] Unsupported part type for parsed table save: " + partType);
                return false;
            }
        }
    }

    private String normalizePartType(String partType) {
        if (partType == null || partType.isBlank()) {
            return "unknown";
        }
        return partType.trim().toLowerCase(Locale.ROOT);
    }
}