package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class ComponentCompatibilityService {

    private static final Logger log = LoggerFactory.getLogger(ComponentCompatibilityService.class);

    private final HardwareFallbackResolver hardwareFallbackResolver;
    private final CpuBenchmarkService cpuBenchmarkService;
    private final GpuBenchmarkService gpuBenchmarkService;

    /**
    Checks if a CPU socket is compatible with a motherboard socket.
    Supports token-based matching for flexibility (Ex "AM5" matches "AM5" chipsets).
     */
    public boolean compatibleSocket(String cpuSocket, String motherboardSocket) {
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


    public boolean compatibleMemory(String motherbaordMemoryType, String ramMemoryType) {
        if (motherbaordMemoryType == null || ramMemoryType == null) {
            return true; // Assume compatible if unknown
        }
        String normalizedMb = normalizeDdrLabel(motherbaordMemoryType);
        String normalizedRam = normalizeDdrLabel(ramMemoryType);
        return normalizedMb != null && normalizedRam != null && normalizedMb.equalsIgnoreCase(normalizedRam);
    }

    public boolean sufficientPsWattage(int psuWattage, String cpuName, String gpuName, boolean preferHeadroom) {
        int cpuWatts = extractCpuWattage(cpuName);
        int gpuWatts = extractGpuWattage(gpuName);
        int totalEstimated = cpuWatts + gpuWatts;
        int required = preferHeadroom ? psuPreferredWattage(totalEstimated) : psuMinimumWattage(totalEstimated);
        return psuWattage >= required;
    }


    public boolean cpuGpuPairingAllowed(String cpuName, String gpuName, String useCase, String resolution) {
        Tier cpuTier = tierOf(cpuName);
        Tier gpuTier = tierOf(gpuName);
        return cpuGpuPairingAllowed(cpuTier, gpuTier, useCase, resolution);
    }


    public CompatibilityCheckResult validateBuild(Map<String, BuildResponse.PartDto> parts) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BuildResponse.PartDto cpu = parts.get("cpu");
        BuildResponse.PartDto gpu = parts.get("gpu");
        BuildResponse.PartDto motherboard = parts.get("motherboard");
        BuildResponse.PartDto memory = parts.get("memory");
        BuildResponse.PartDto psu = parts.get("powerSupply");

        if (cpu != null && motherboard != null) {
            if (!compatibleSocket(cpu.socket(), motherboard.socket())) {
                issues.add("CPU socket (" + cpu.socket() + ") incompatible with motherboard socket (" + motherboard.socket() + ")");
            }
        }

        if (motherboard != null && memory != null) {
            if (!compatibleMemory(motherboard.memoryType(), memory.memoryType())) {
                issues.add("Memory type (" + memory.memoryType() + ") not supported by motherboard (" + motherboard.memoryType() + ")");
            }
        }

        if (cpu != null && gpu != null && psu != null) {
            int psuWatts = safeInt(psu.wattage(), 0);
            int cpuWatts = resolveCpuTdp(cpu);
            int gpuWatts = resolveGpuTdp(gpu);
            int totalEstimated = cpuWatts + gpuWatts;
            int required = psuPreferredWattage(totalEstimated);
            if (psuWatts < required) {
                warnings.add("PSU wattage (" + psuWatts + "W) may be tight for CPU + GPU power draw — estimated " + totalEstimated + "W");
            }
        }

        if (cpu != null && gpu != null) {
            Tier cpuTier = tierOfPart(cpu);
            Tier gpuTier = tierOfPart(gpu);
            if (!cpuGpuPairingAllowed(cpuTier, gpuTier, "gaming", "1440p")) {
                warnings.add("CPU and GPU tier mismatch - consider adjusting component selection");
            }
        }

        boolean compatible = issues.isEmpty();
        return new CompatibilityCheckResult(compatible, issues, warnings);
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

        return tokens;
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String normalizeDdrLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String upper = label.toUpperCase(Locale.ROOT);
        if (upper.contains("DDR5")) return "DDR5";
        if (upper.contains("DDR4")) return "DDR4";
        if (upper.contains("DDR3")) return "DDR3";
        return null;
    }

    private Tier tierOf(String componentName) {
        if (componentName == null || componentName.isBlank()) {
            return Tier.BUDGET;
        }

        String upper = componentName.toUpperCase(Locale.ROOT);

        if (upper.contains("RYZEN 9") || upper.contains("I9") || upper.contains("I9-")) return Tier.FLAGSHIP;
        if (upper.contains("RYZEN 7") || upper.contains("I7") || upper.contains("I7-")) return Tier.HIGH;
        if (upper.contains("RYZEN 5") || upper.contains("I5") || upper.contains("I5-")) return Tier.MID;
        if (upper.contains("RYZEN 3") || upper.contains("I3") || upper.contains("I3-")) return Tier.BUDGET;

        if (upper.contains("RTX 4090") || upper.contains("RTX 3090") || upper.contains("RX 7900")) return Tier.FLAGSHIP;
        if (upper.contains("RTX 4080") || upper.contains("RTX 3080") || upper.contains("RX 7800")) return Tier.HIGH;
        if (upper.contains("RTX 4070") || upper.contains("RTX 3070") || upper.contains("RX 7700")) return Tier.MID;
        if (upper.contains("RTX 4060") || upper.contains("RTX 3060") || upper.contains("RX 7600")) return Tier.BUDGET;

        return Tier.MID;
    }

    private boolean cpuGpuPairingAllowed(Tier cpuTier, Tier gpuTier, String useCase, String resolution) {
        if ("gaming".equalsIgnoreCase(useCase)) {
            if (gpuTier == Tier.FLAGSHIP && cpuTier.ordinal() < Tier.MID.ordinal()) {
                return false; // Budget CPU + Flagship GPU = bottleneck
            }
            if (gpuTier == Tier.HIGH && cpuTier == Tier.BUDGET) {
                return false; // Severe bottleneck
            }
        }
        return true;
    }

    private int estimatePower(String cpuName, String gpuName) {
        int cpuWatts = extractCpuWattage(cpuName);
        int gpuWatts = extractGpuWattage(gpuName);
        return cpuWatts + gpuWatts;
    }

    private int extractCpuWattage(String cpuName) {
        if (cpuName == null || cpuName.isBlank()) return 65;

        String upper = cpuName.toUpperCase(Locale.ROOT);

        if (upper.contains("RYZEN 5 7500F") || upper.contains("RYZEN 5 5600")) return 65;
        if (upper.contains("RYZEN 5 7600X") || upper.contains("RYZEN 5 9600X")) return 105;
        if (upper.contains("RYZEN 7 5700X")) return 65;
        if (upper.contains("RYZEN 7 5800X") || upper.contains("RYZEN 7 7700X") || upper.contains("RYZEN 7 9700X")) return 105;
        if (upper.contains("RYZEN 9")) return 125;
        if (upper.contains("RYZEN 7")) return 105;
        if (upper.contains("RYZEN 5")) return 65;

        if (upper.matches(".*I[3579]-1[2-9]\\d{3}K[F]?.*")) return 125;
        if (upper.matches(".*I[3579]-1[2-9]\\d{3}(?![A-Z0-9]).*")) return 65;
        if (upper.matches(".*I[3579]-[7-9]\\d{3}K.*")) return 91;
        if (upper.matches(".*I[3579]-[7-9]\\d{3}(?![A-Z0-9]).*")) return 65;
        if (upper.contains("I9")) return 125;
        if (upper.contains("I7")) return 100;
        if (upper.contains("I5")) return 65;
        if (upper.contains("I3")) return 50;

        return 65;
    }

    private int extractGpuWattage(String gpuName) {
        if (gpuName == null || gpuName.isBlank()) return 200;

        String upper = gpuName.toUpperCase(Locale.ROOT);

        // NVIDIA RTX 40
        if (upper.contains("RTX 4090")) return 450;
        if (upper.contains("RTX 4080")) return 320;
        if (upper.contains("RTX 4070")) return 200;
        if (upper.contains("RTX 4060")) return 115;

        // NVIDIA RTX 30
        if (upper.contains("RTX 3090")) return 350;
        if (upper.contains("RTX 3080")) return 320;
        if (upper.contains("RTX 3070")) return 220;
        if (upper.contains("RTX 3060")) return 170;

        // AMD RX 7000
        if (upper.contains("RX 7900")) return 315;
        if (upper.contains("RX 7800")) return 263;
        if (upper.contains("RX 7700")) return 245;
        if (upper.contains("RX 7600")) return 165;

        // NVIDIA RTX 50
        if (upper.contains("RTX 5090")) return 600;
        if (upper.contains("RTX 5080")) return 400;
        if (upper.contains("RTX 5070")) return 250;
        if (upper.contains("RTX 5060")) return 170;

        // AMD RX 6000 (RDNA 2)
        if (upper.contains("RX 6900") || upper.contains("RX 6950")) return 300;
        if (upper.contains("RX 6800")) return 250;
        if (upper.contains("RX 6700") || upper.contains("RX 6750")) return 230;
        if (upper.contains("RX 6600")) return 132;
        return 200; // Default
    }

    private int resolveCpuTdp(BuildResponse.PartDto cpu) {
        if (cpu == null) return 65;
        if (cpu.wattage() != null && cpu.wattage() > 0) return cpu.wattage();
        return cpuBenchmarkService.findByName(cpu.name())
                .map(b -> b.getTdpWatts() == null ? extractCpuWattage(cpu.name()) : b.getTdpWatts())
                .orElseGet(() -> extractCpuWattage(cpu.name()));
    }

    private int resolveGpuTdp(BuildResponse.PartDto gpu) {
        if (gpu == null) return 200;
        if (gpu.wattage() != null && gpu.wattage() > 0) return gpu.wattage();
        return gpuBenchmarkService.findByName(gpu.name())
                .map(b -> b.getTdpWatts() == null ? extractGpuWattage(gpu.name()) : b.getTdpWatts())
                .orElseGet(() -> extractGpuWattage(gpu.name()));
    }

    private Tier tierOfPart(BuildResponse.PartDto part) {
        if (part == null) return Tier.BUDGET;
        String label = part.tierLabel();
        if (label != null && !label.isBlank()) {
            String low = label.toLowerCase(Locale.ROOT);
            if (low.contains("flag") || low.contains("enthusiast")) return Tier.FLAGSHIP;
            if (low.contains("high")) return Tier.HIGH;
            if (low.contains("mid")) return Tier.MID;
            return Tier.BUDGET;
        }
        // fallback to name-based heuristic
        return tierOf(part.name());
    }

    private int psuMinimumWattage(int estimatedPower) {
        return Math.max((int) (estimatedPower * 1.15), 350);
    }

    private int psuPreferredWattage(int estimatedPower) {
        return Math.max((int) (estimatedPower * 1.25), 450);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }

    // ===== Enums and DTOs =====

    public enum Tier {
        BUDGET, MID, HIGH, FLAGSHIP
    }

    public static class CompatibilityCheckResult {
        public final boolean compatible;
        public final List<String> issues;
        public final List<String> warnings;

        public CompatibilityCheckResult(boolean compatible, List<String> issues, List<String> warnings) {
            this.compatible = compatible;
            this.issues = issues;
            this.warnings = warnings;
        }
    }
}
