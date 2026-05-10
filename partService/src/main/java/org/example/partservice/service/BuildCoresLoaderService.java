package org.example.partservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.partservice.model.ReferenceCpu;
import org.example.partservice.model.ReferenceGpu;
import org.example.partservice.model.ReferenceMotherboard;
import org.example.partservice.repository.ReferenceCpuRepository;
import org.example.partservice.repository.ReferenceGpuRepository;
import org.example.partservice.repository.ReferenceMotherboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class BuildCoresLoaderService {
    @Autowired
    private ReferenceCpuRepository cpuRepository;

    @Autowired
    private ReferenceGpuRepository gpuRepository;

    @Autowired
    private ReferenceMotherboardRepository motherboardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BUILDCORES_PATH = "c:/Users/User/Desktop/buildcores-open-db/open-db/";
    private static final Pattern SOCKET_LGA_PATTERN = Pattern.compile("\\bLGA\\s*-?\\s*(\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOCKET_AM_PATTERN = Pattern.compile("\\bAM\\s*-?\\s*(\\d{1,2}(?:\\+)?)\\b", Pattern.CASE_INSENSITIVE);

    public void loadCpuData() throws Exception {
        Path cpuPath = Paths.get(BUILDCORES_PATH + "CPU");
        if (!Files.exists(cpuPath)) {
            throw new RuntimeException("CPU directory not found: " + cpuPath);
        }

        try (Stream<Path> paths = Files.list(cpuPath)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            loadCpuFile(p);
                        } catch (Exception e) {
                            System.err.println("Error loading CPU file " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    public void loadGpuData() throws Exception {
        Path gpuPath = Paths.get(BUILDCORES_PATH + "GPU");
        if (!Files.exists(gpuPath)) {
            throw new RuntimeException("GPU directory not found: " + gpuPath);
        }

        try (Stream<Path> paths = Files.list(gpuPath)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            loadGpuFile(p);
                        } catch (Exception e) {
                            System.err.println("Error loading GPU file " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    public void loadMotherboardData() throws Exception {
        Path motherboardPath = Paths.get(BUILDCORES_PATH + "Motherboard");
        if (!Files.exists(motherboardPath)) {
            throw new RuntimeException("Motherboard directory not found: " + motherboardPath);
        }

        try (Stream<Path> paths = Files.list(motherboardPath)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            loadMotherboardFile(p);
                        } catch (Exception e) {
                            System.err.println("Error loading Motherboard file " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    private void loadCpuFile(Path filePath) throws Exception {
        String content = Files.readString(filePath);
        JsonNode root = objectMapper.readTree(content);

        String opendbId = root.get("opendb_id").asText();

        // Upsert existing reference rows so socket nulls can be repaired on reload.
        Optional<ReferenceCpu> existing = cpuRepository.findByOpendbId(opendbId);
        ReferenceCpu cpu = existing.orElseGet(ReferenceCpu::new);
        cpu.setOpendbId(opendbId);
        cpu.setRawJson(content);

        // Extract metadata
        JsonNode metadata = root.get("metadata");
        if (metadata != null) {
            if (metadata.has("name")) cpu.setName(metadata.get("name").asText());
            if (metadata.has("manufacturer")) cpu.setManufacturer(metadata.get("manufacturer").asText());
            if (metadata.has("series")) cpu.setSeries(metadata.get("series").asText());
            if (metadata.has("variant")) cpu.setVariant(metadata.get("variant").asText());
        }

        // Extract specs
        JsonNode cores = root.get("cores");
        if (cores != null) {
            if (cores.has("total")) cpu.setCores(cores.get("total").asInt());
            if (cores.has("threads")) cpu.setThreads(cores.get("threads").asInt());
        }

        JsonNode clocks = root.get("clocks");
        if (clocks != null) {
            JsonNode perfClocks = clocks.get("performance");
            if (perfClocks != null) {
                if (perfClocks.has("base")) {
                    cpu.setBaseClockGhz(new BigDecimal(perfClocks.get("base").asDouble()));
                }
                if (perfClocks.has("boost")) {
                    cpu.setBoostClockGhz(new BigDecimal(perfClocks.get("boost").asDouble()));
                }
            }
        }

        JsonNode specs = root.get("specifications");
        if (specs != null) {
            if (specs.has("socket")) cpu.setSocket(specs.get("socket").asText());
            if (specs.has("tdp")) cpu.setTdp(specs.get("tdp").asInt());
            if (specs.has("lithography")) {
                // Extract nm from lithography (e.g., "5nm" -> 5)
            }
        }

        if (cpu.getSocket() == null || cpu.getSocket().isBlank()) {
            cpu.setSocket(deriveSocketFromCpuName(cpu.getName()));
        }

        if (root.has("microarchitecture")) {
            cpu.setMicroarchitecture(root.get("microarchitecture").asText());
        }

        cpuRepository.save(cpu);
    }

    private String deriveSocketFromCpuName(String cpuName) {
        if (cpuName == null || cpuName.isBlank()) {
            return null;
        }

        Matcher lgaMatcher = SOCKET_LGA_PATTERN.matcher(cpuName);
        if (lgaMatcher.find()) {
            return "LGA" + lgaMatcher.group(1);
        }

        Matcher amMatcher = SOCKET_AM_PATTERN.matcher(cpuName);
        if (amMatcher.find()) {
            return "AM" + amMatcher.group(1).toUpperCase();
        }

        return null;
    }

    private void loadGpuFile(Path filePath) throws Exception {
        String content = Files.readString(filePath);
        JsonNode root = objectMapper.readTree(content);

        String opendbId = root.get("opendb_id").asText();

        // Skip if already exists
        if (gpuRepository.findByOpendbId(opendbId).isPresent()) {
            return;
        }

        ReferenceGpu gpu = new ReferenceGpu();
        gpu.setOpendbId(opendbId);
        gpu.setRawJson(content);

        // Extract metadata
        JsonNode metadata = root.get("metadata");
        if (metadata != null) {
            if (metadata.has("name")) gpu.setName(metadata.get("name").asText());
            if (metadata.has("manufacturer")) gpu.setManufacturer(metadata.get("manufacturer").asText());
            if (metadata.has("series")) gpu.setSeries(metadata.get("series").asText());
            if (metadata.has("releaseYear")) gpu.setReleaseYear(metadata.get("releaseYear").asInt());
        }

        // Extract specs
        if (root.has("chipset")) gpu.setChipset(root.get("chipset").asText());
        if (root.has("memory")) gpu.setMemoryGb(root.get("memory").asInt());
        if (root.has("memory_type")) gpu.setMemoryType(root.get("memory_type").asText());
        if (root.has("core_count")) gpu.setCoreCount(root.get("core_count").asInt());
        if (root.has("interface")) gpu.setInterface_(root.get("interface").asText());
        if (root.has("tdp")) gpu.setTdp(root.get("tdp").asInt());

        if (root.has("core_base_clock")) {
            gpu.setBaseClockMhz(new BigDecimal(root.get("core_base_clock").asDouble()));
        }
        if (root.has("core_boost_clock")) {
            gpu.setBoostClockMhz(new BigDecimal(root.get("core_boost_clock").asDouble()));
        }

        gpuRepository.save(gpu);
    }

    private void loadMotherboardFile(Path filePath) throws Exception {
        String content = Files.readString(filePath);
        JsonNode root = objectMapper.readTree(content);

        String opendbId = root.path("opendb_id").asText(null);
        if (opendbId == null || opendbId.isBlank()) {
            return;
        }

        if (motherboardRepository.findByOpendbId(opendbId).isPresent()) {
            return;
        }

        ReferenceMotherboard motherboard = new ReferenceMotherboard();
        motherboard.setOpendbId(opendbId);
        motherboard.setRawJson(content);

        JsonNode metadata = root.path("metadata");
        if (!metadata.isMissingNode()) {
            if (metadata.hasNonNull("name")) motherboard.setName(metadata.get("name").asText());
            if (metadata.hasNonNull("manufacturer")) motherboard.setManufacturer(metadata.get("manufacturer").asText());
            if (metadata.hasNonNull("series")) motherboard.setSeries(metadata.get("series").asText());
            if (metadata.hasNonNull("variant")) motherboard.setVariant(metadata.get("variant").asText());
        }

        if (motherboard.getName() == null || motherboard.getName().isBlank()) {
            return;
        }

        if (root.hasNonNull("socket")) motherboard.setSocket(root.get("socket").asText());
        if (root.hasNonNull("form_factor")) motherboard.setFormFactor(root.get("form_factor").asText());
        if (root.hasNonNull("chipset")) motherboard.setChipset(root.get("chipset").asText());

        JsonNode memory = root.path("memory");
        if (!memory.isMissingNode()) {
            if (memory.hasNonNull("max")) motherboard.setMemoryMaxGb(memory.get("max").asInt());
            if (memory.hasNonNull("ram_type")) motherboard.setMemoryRamType(memory.get("ram_type").asText());
            if (memory.hasNonNull("slots")) motherboard.setMemorySlots(memory.get("slots").asInt());
        }

        motherboardRepository.save(motherboard);
    }
}
