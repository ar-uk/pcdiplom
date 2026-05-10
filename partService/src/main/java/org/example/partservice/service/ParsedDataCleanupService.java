package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.repository.MarketListingRepository;
import org.example.partservice.repository.ParsedCpuCoolerRepository;
import org.example.partservice.repository.ParsedCpuRepository;
import org.example.partservice.repository.ParsedInternalHardDriveRepository;
import org.example.partservice.repository.ParsedMemoryRepository;
import org.example.partservice.repository.ParsedMotherboardRepository;
import org.example.partservice.repository.ParsedPcCaseRepository;
import org.example.partservice.repository.ParsedPowerSupplyRepository;
import org.example.partservice.repository.ParsedVideoCardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParsedDataCleanupService {

    private final ParsedCpuRepository parsedCpuRepository;
    private final ParsedCpuCoolerRepository parsedCpuCoolerRepository;
    private final ParsedVideoCardRepository parsedVideoCardRepository;
    private final ParsedPowerSupplyRepository parsedPowerSupplyRepository;
    private final ParsedPcCaseRepository parsedPcCaseRepository;
    private final ParsedMemoryRepository parsedMemoryRepository;
    private final ParsedInternalHardDriveRepository parsedInternalHardDriveRepository;
    private final ParsedMotherboardRepository parsedMotherboardRepository;
    private final MarketListingRepository marketListingRepository;

    @Value("${cleanup.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "${cleanup.cron:0 0 3 * * *}")
    public void scheduledCleanup() {
        CleanupSummary summary = cleanupOlderThanDays(retentionDays);
        System.out.println("[DATA CLEANUP] " + summary);
    }

    public CleanupSummary cleanupOlderThanDays(int days) {
        int safeDays = Math.max(days, 1);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(safeDays);

        long deletedCpu = parsedCpuRepository.deleteByLastScrapedBefore(cutoff);
        long deletedCooler = parsedCpuCoolerRepository.deleteByLastScrapedBefore(cutoff);
        long deletedGpu = parsedVideoCardRepository.deleteByLastScrapedBefore(cutoff);
        long deletedPsu = parsedPowerSupplyRepository.deleteByLastScrapedBefore(cutoff);
        long deletedCase = parsedPcCaseRepository.deleteByLastScrapedBefore(cutoff);
        long deletedMemory = parsedMemoryRepository.deleteByLastScrapedBefore(cutoff);
        long deletedStorage = parsedInternalHardDriveRepository.deleteByLastScrapedBefore(cutoff);
        long deletedMotherboard = parsedMotherboardRepository.deleteByLastScrapedBefore(cutoff);
        long deletedListings = marketListingRepository.deleteByLastScrapedBefore(cutoff);

        long totalDeleted = deletedCpu
                + deletedCooler
                + deletedGpu
                + deletedPsu
                + deletedCase
                + deletedMemory
                + deletedStorage
                + deletedMotherboard
                + deletedListings;

        return new CleanupSummary(
                safeDays,
                cutoff,
                deletedCpu,
                deletedCooler,
                deletedGpu,
                deletedPsu,
                deletedCase,
                deletedMemory,
                deletedStorage,
                deletedMotherboard,
                deletedListings,
                totalDeleted
        );
    }

    public record CleanupSummary(
            int retentionDays,
            LocalDateTime cutoff,
            long deletedCpu,
            long deletedCpuCooler,
            long deletedGpu,
            long deletedPsu,
            long deletedCase,
            long deletedMemory,
            long deletedStorage,
            long deletedMotherboard,
            long deletedMarketListings,
            long totalDeleted
    ) {
    }
}
