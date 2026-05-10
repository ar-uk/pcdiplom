package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildResponse;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for filtering hardware components by various criteria.
 * Provides methods for filtering CPUs, GPUs, motherboards, memory, PSUs, etc.
 */
@Service
@RequiredArgsConstructor
public class ComponentFilterService {

    @Value("${part-service.url}")
    private String partServiceUrl;

    private final RestClient restClient = RestClient.builder().build();
    private final ComponentCompatibilityService compatibilityService;


     //Retrieves all CPUs from the part service.
    public List<BuildResponse.PartDto> getAllCpus() {
        return fetchComponentsFromPartService("cpu");
    }


    //Retrieves all GPUs from the part service.
    public List<BuildResponse.PartDto> getAllGpus() {
        return fetchComponentsFromPartService("video-card");
    }

    public List<BuildResponse.PartDto> getAllMotherboards() {
        return fetchComponentsFromPartService("motherboard");
    }


    //Retrieves all RAM modules from the part service.
    public List<BuildResponse.PartDto> getAllMemory() {
        return fetchComponentsFromPartService("memory");
    }


    //Retrieves all PSUs from the part service.
    public List<BuildResponse.PartDto> getAllPsus() {
        return fetchComponentsFromPartService("power-supply");
    }


     //Retrieves all storage devices from the part service.

    public List<BuildResponse.PartDto> getAllStorage() {
        return fetchComponentsFromPartService("internal-hard-drive");
    }


     //Retrieves all cases from the part service.

    public List<BuildResponse.PartDto> getAllCases() {
        return fetchComponentsFromPartService("pc-case");
    }


    //Filters CPUs by socket.
    public List<BuildResponse.PartDto> filterCpusBySocket(String socket) {
        List<BuildResponse.PartDto> cpus = getAllCpus();
        return cpus.stream()
                .filter(cpu -> cpu.socket() != null && cpu.socket().equalsIgnoreCase(socket))
                .collect(Collectors.toList());
    }


     //Filters motherboards by socket.
    public List<BuildResponse.PartDto> filterMotherboardsBySocket(String socket) {
        List<BuildResponse.PartDto> motherboards = getAllMotherboards();
        return motherboards.stream()
                .filter(mb -> mb.socket() != null && mb.socket().equalsIgnoreCase(socket))
                .collect(Collectors.toList());
    }


     //Filters memory by DDR type (DDR4 or DDR5).
    public List<BuildResponse.PartDto> filterMemoryByType(String ddrType) {
        List<BuildResponse.PartDto> memory = getAllMemory();
        return memory.stream()
                .filter(ram -> ram.memoryType() != null && 
                        normalizeDdr(ram.memoryType()).equalsIgnoreCase(normalizeDdr(ddrType)))
                .collect(Collectors.toList());
    }


    public List<BuildResponse.PartDto> filterMemoryByCapacity(int capacityGb) {
        List<BuildResponse.PartDto> memory = getAllMemory();
        return memory.stream()
                .filter(ram -> extractCapacityGb(ram.name()) >= capacityGb)
                .collect(Collectors.toList());
    }

    public List<BuildResponse.PartDto> filterPsuByWattage(int minWattage) {
        List<BuildResponse.PartDto> psus = getAllPsus();
        return psus.stream()
                .filter(psu -> safeInt(psu.wattage(), 0) >= minWattage)
                .collect(Collectors.toList());
    }


    public List<BuildResponse.PartDto> filterByPriceRange(List<BuildResponse.PartDto> components, 
                                                          BigDecimal minPrice, 
                                                          BigDecimal maxPrice) {
        return components.stream()
                .filter(part -> {
                    BigDecimal price = part.priceKzt() != null ? part.priceKzt() : BigDecimal.ZERO;
                    return price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0;
                })
                .collect(Collectors.toList());
    }


    public List<BuildResponse.PartDto> filterMotherboardsForCpuSocket(String cpuSocket) {
        List<BuildResponse.PartDto> motherboards = getAllMotherboards();
        return motherboards.stream()
                .filter(mb -> mb.socket() != null && 
                        compatibilityService.compatibleSocket(cpuSocket, mb.socket()))
                .collect(Collectors.toList());
    }


    public List<BuildResponse.PartDto> filterMemoryForMotherboard(String motherboardMemoryType) {
        List<BuildResponse.PartDto> memory = getAllMemory();
        String normalized = normalizeDdr(motherboardMemoryType);
        return memory.stream()
                .filter(ram -> ram.memoryType() != null && 
                        normalizeDdr(ram.memoryType()).equalsIgnoreCase(normalized))
                .collect(Collectors.toList());
    }

    public Set<String> getAvailableCpuSockets() {
        return getAllCpus().stream()
                .map(BuildResponse.PartDto::socket)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    public Set<String> getAvailableMotherboardSockets() {
        return getAllMotherboards().stream()
                .map(BuildResponse.PartDto::socket)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<String> getAvailableMemoryTypes() {
        return getAllMemory().stream()
                .map(BuildResponse.PartDto::memoryType)
                .filter(Objects::nonNull)
                .map(this::normalizeDdr)
                .collect(Collectors.toSet());
    }


    public List<Integer> getAvailablePsuWattages() {
        return getAllPsus().stream()
                .map(BuildResponse.PartDto::wattage)
                .filter(Objects::nonNull)
                .filter(w -> w > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<BuildResponse.PartDto> filterCpusByTier(String tier) {
        List<BuildResponse.PartDto> cpus = getAllCpus();
        return cpus.stream()
                .filter(cpu -> matchesTier(cpu.name(), tier))
                .collect(Collectors.toList());
    }

    public List<BuildResponse.PartDto> filterGpusByVram(int minVram) {
        List<BuildResponse.PartDto> gpus = getAllGpus();
        return gpus.stream()
                .filter(gpu -> extractVramGb(gpu.name()) >= minVram)
                .collect(Collectors.toList());
    }

    public List<BuildResponse.PartDto> filterStorageByCapacity(int minCapacityGb) {
        List<BuildResponse.PartDto> storage = getAllStorage();
        return storage.stream()
                .filter(s -> extractStorageCapacityGb(s.name()) >= minCapacityGb)
                .collect(Collectors.toList());
    }

    // ===== Helper Methods =====

    private List<BuildResponse.PartDto> fetchComponentsFromPartService(String category) {
        try {
            String url = partServiceUrl + "/api/parsed/" + category;
            // This would be implemented with RestClient/RestTemplate
            // For now, return empty list as placeholder
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String normalizeDdr(String ddrType) {
        if (ddrType == null || ddrType.isBlank()) {
            return "";
        }
        String upper = ddrType.toUpperCase(Locale.ROOT);
        if (upper.contains("DDR5")) return "DDR5";
        if (upper.contains("DDR4")) return "DDR4";
        if (upper.contains("DDR3")) return "DDR3";
        return upper;
    }

    private int extractCapacityGb(String name) {
        if (name == null || name.isBlank()) return 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*GB");
        java.util.regex.Matcher matcher = pattern.matcher(name.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private int extractVramGb(String name) {
        if (name == null || name.isBlank()) return 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(?:GB|G)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(name.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private int extractStorageCapacityGb(String name) {
        if (name == null || name.isBlank()) return 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*([GT]B)");
        java.util.regex.Matcher matcher = pattern.matcher(name.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            return unit.equals("TB") ? value * 1024 : value;
        }
        return 0;
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }

    private boolean matchesTier(String componentName, String tier) {
        if (componentName == null || tier == null) return false;
        String upper = componentName.toUpperCase(Locale.ROOT);
        
        switch (tier.toUpperCase(Locale.ROOT)) {
            case "BUDGET":
                return upper.contains("RYZEN 3") || upper.contains("I3") || 
                       upper.contains("RTX 4060") || upper.contains("RX 6600");
            case "MID":
                return upper.contains("RYZEN 5") || upper.contains("I5") ||
                       upper.contains("RTX 4070") || upper.contains("RX 7700");
            case "HIGH":
                return upper.contains("RYZEN 7") || upper.contains("I7") ||
                       upper.contains("RTX 4080") || upper.contains("RX 7900");
            case "FLAGSHIP":
                return upper.contains("RYZEN 9") || upper.contains("I9") ||
                       upper.contains("RTX 4090") || upper.contains("RX 7950");
            default:
                return false;
        }
    }
}
