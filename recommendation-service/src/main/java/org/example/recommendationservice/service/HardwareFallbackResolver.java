package org.example.recommendationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.CpuBenchmark;
import org.example.recommendationservice.model.GpuBenchmark;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class HardwareFallbackResolver {

    private static final Pattern CPU_QUERY_PATTERN = Pattern.compile("(?i)(ryzen\\s+[3579]\\s+\\d{4}x?|ryzen\\s+[3579]\\s+\\d{4}|core\\s+i[3579]-?\\d{4,5}f?|i[3579]-?\\d{4,5}f?)");
    private static final Pattern GPU_QUERY_PATTERN = Pattern.compile("(?i)(rtx\\s*\\d{3,4}(?:\\s*ti)?|rx\\s*\\d{3,4}(?:\\s*xt)?|arc\\s*\\w+)");

    private final CpuBenchmarkService cpuBenchmarkService;
    private final GpuBenchmarkService gpuBenchmarkService;
    private final PartMetadataService partMetadataService;

    public ResolvedValue<String> resolveCpuSocket(String normalizedSocket, String cpuName) {
        if (normalizedSocket != null && !normalizedSocket.isBlank()) {
            return new ResolvedValue<>(normalizedSocket, false);
        }
        String normalized = normalize(cpuName);
        if (normalized.contains("am5") || normalized.matches(".*ryzen [3579] [789]\\d{3}.*")) {
            return new ResolvedValue<>("AM5", true);
        }
        if (normalized.contains("am4") || normalized.matches(".*ryzen [3579] [1-5]\\d{3}.*")) {
            return new ResolvedValue<>("AM4", true);
        }
        if (normalized.contains("lga1700") || normalized.contains("12th") || normalized.contains("13th") || normalized.contains("14th") || normalized.matches(".*i[3579] 1[2-4]\\d{3}.*")) {
            return new ResolvedValue<>("LGA1700", true);
        }
        if (normalized.contains("lga1200") || normalized.contains("10th") || normalized.contains("11th")) {
            return new ResolvedValue<>("LGA1200", true);
        }
        return new ResolvedValue<>(null, true);
    }

    public ResolvedValue<String> resolveMemoryTypeFromName(String memoryName) {
        String normalized = normalize(memoryName);
        if (normalized.contains("ddr5")) {
            return new ResolvedValue<>("DDR5", true);
        }
        if (normalized.contains("ddr4")) {
            return new ResolvedValue<>("DDR4", true);
        }
        return new ResolvedValue<>(null, true);
    }

    public ResolvedValue<Integer> resolveCpuWattage(JsonNode node, String cpuName) {
        Integer normalized = readInteger(node, "tdpW", "wattageW", "cpuTdpW");
        if (normalized != null && normalized > 0) {
            return new ResolvedValue<>(normalized, false);
        }

        Integer benchmarkWatts = cpuBenchmarkService.findByName(cpuName)
                .map(CpuBenchmark::getTdpWatts)
                .filter(value -> value != null && value > 0)
                .orElse(null);
        if (benchmarkWatts != null) {
            return new ResolvedValue<>(benchmarkWatts, false);
        }

        Integer metadataWatts = partMetadataService.findTdpWatts("cpu", cpuName)
                .filter(value -> value != null && value > 0)
                .orElse(null);
        if (metadataWatts != null) {
            return new ResolvedValue<>(metadataWatts, false);
        }
        return new ResolvedValue<>(extractCpuWattage(cpuName), true);
    }

    public ResolvedValue<Integer> resolveGpuWattage(JsonNode node, String gpuName) {
        Integer normalized = readInteger(node, "tdpW", "wattageW", "gpuTdpW");
        if (normalized != null && normalized > 0) {
            return new ResolvedValue<>(normalized, false);
        }

        Integer benchmarkWatts = gpuBenchmarkService.findByName(gpuName)
                .map(GpuBenchmark::getTdpWatts)
                .filter(value -> value != null && value > 0)
                .orElse(null);
        if (benchmarkWatts != null) {
            return new ResolvedValue<>(benchmarkWatts, false);
        }

        Integer metadataWatts = partMetadataService.findTdpWatts("gpu", gpuName)
                .filter(value -> value != null && value > 0)
                .orElse(null);
        if (metadataWatts != null) {
            return new ResolvedValue<>(metadataWatts, false);
        }

        String name = normalize(gpuName).replace(" ", "");
        if (name.contains("5090") || name.contains("4090") || name.contains("7900xtx")) {
            return new ResolvedValue<>(420, true);
        }
        if (name.contains("5080") || name.contains("4080") || name.contains("7900xt") || name.contains("7800xt")) {
            return new ResolvedValue<>(320, true);
        }
        if (name.contains("5070ti") || name.contains("4070ti")) {
            return new ResolvedValue<>(285, true);
        }
        if (name.contains("5070") || name.contains("4070super") || name.contains("4070") || name.contains("7700xt")) {
            return new ResolvedValue<>(245, true);
        }
        if (name.contains("4060ti")) {
            return new ResolvedValue<>(160, true);
        }
        if (name.contains("5060") || name.contains("4060") || name.contains("7600")) {
            return new ResolvedValue<>(165, true);
        }
        if (name.contains("3060") || name.contains("3050") || name.contains("6600")) {
            return new ResolvedValue<>(130, true);
        }

        return new ResolvedValue<>(170, true);
    }

    public ResolvedValue<String> resolveCpuTierLabel(String cpuName) {
        String normalized = normalize(cpuName);
        if (normalized.contains("ryzen 9") || normalized.contains("core i9")) {
            return new ResolvedValue<>("enthusiast", true);
        }
        if (normalized.contains("ryzen 7") || normalized.contains("core i7")) {
            return new ResolvedValue<>("high", true);
        }
        if (normalized.contains("ryzen 5") || normalized.contains("core i5")) {
            return new ResolvedValue<>("mid_high", true);
        }
        if (normalized.contains("ryzen 3") || normalized.contains("core i3")) {
            return new ResolvedValue<>("mid", true);
        }
        return new ResolvedValue<>("low_mid", true);
    }

    public ResolvedValue<String> resolveGpuTierLabel(String gpuName) {
        String normalized = normalize(gpuName);
        String compact = normalized.replace(" ", "");
        if (compact.contains("5090") || compact.contains("4090") || compact.contains("7900xtx")) {
            return new ResolvedValue<>("enthusiast", true);
        }
        if (compact.contains("5080") || compact.contains("4080")
                || compact.contains("7900xt") || compact.contains("7800xt")
                || compact.contains("5070ti") || compact.contains("4070ti")) {
            return new ResolvedValue<>("high", true);
        }
        if (compact.contains("5070") || compact.contains("4070super")
                || compact.contains("4070") || compact.contains("7700xt")
                || compact.contains("7700")) {
            return new ResolvedValue<>("mid_high", true);
        }
        if (compact.contains("5060") || compact.contains("4060ti")
                || compact.contains("4060") || compact.contains("7600xt")
                || compact.contains("7600")) {
            return new ResolvedValue<>("mid", true);
        }
        if (compact.contains("3060") || compact.contains("3050")
                || compact.contains("6600") || compact.contains("6650")) {
            return new ResolvedValue<>("low_mid", true);
        }
        return new ResolvedValue<>("entry", true);
    }

    public String extractCpuQuery(String prompt) {
        return extractPattern(prompt, CPU_QUERY_PATTERN);
    }

    public String extractGpuQuery(String prompt) {
        return extractPattern(prompt, GPU_QUERY_PATTERN);
    }

    private String extractPattern(String prompt, Pattern pattern) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Integer resolveFirstInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                if (valueNode.isInt() || valueNode.isLong()) {
                    return valueNode.intValue();
                }
                if (valueNode.isNumber()) {
                    return valueNode.numberValue().intValue();
                }
                String raw = valueNode.asText("").replaceAll("[^0-9]", "").trim();
                if (!raw.isBlank()) {
                    try {
                        return Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... fieldNames) {
        return resolveFirstInteger(node, fieldNames);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int extractCpuWattage(String text) {
        String normalized = normalize(text);
        if (normalized.contains("ryzen 9") || normalized.contains("core i9")) {
            return 170;
        }
        if (normalized.contains("ryzen 7") || normalized.contains("core i7")) {
            return 125;
        }
        if (normalized.contains("ryzen 5") || normalized.contains("core i5")) {
            return 95;
        }
        return 65;
    }

    public record ResolvedValue<T>(T value, boolean fallbackUsed) {
    }
}
