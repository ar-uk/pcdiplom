package org.example.partservice.service;

import org.example.partservice.model.ReferenceCpu;
import org.example.partservice.model.ReferenceGpu;
import org.example.partservice.model.ReferenceMotherboard;
import org.example.partservice.repository.ReferenceCpuRepository;
import org.example.partservice.repository.ReferenceGpuRepository;
import org.example.partservice.repository.ReferenceMotherboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ProductMatcherService {
    @Autowired
    private ReferenceCpuRepository cpuRepository;

    @Autowired
    private ReferenceGpuRepository gpuRepository;

    @Autowired
    private ReferenceMotherboardRepository motherboardRepository;

    public static class MatchResult {
        public String type; // "CPU", "GPU", or "MOTHERBOARD"
        public Long referenceId;
        public String referenceName;
        public Double confidence;
        public Map<String, String> reasoning;

        public MatchResult(String type, Long refId, String refName, Double conf, Map<String, String> reason) {
            this.type = type;
            this.referenceId = refId;
            this.referenceName = refName;
            this.confidence = conf;
            this.reasoning = reason;
        }
    }

    public Optional<MatchResult> matchProduct(String productName) {
        if (looksLikeMotherboard(productName)) {
            Optional<MatchResult> motherboardMatch = matchMotherboard(productName);
            if (motherboardMatch.isPresent() && motherboardMatch.get().confidence > 0.6) {
                return motherboardMatch;
            }
        }

        // Try CPU match first
        Optional<MatchResult> cpuMatch = matchCpu(productName);
        if (cpuMatch.isPresent() && cpuMatch.get().confidence > 0.7) {
            return cpuMatch;
        }

        // Try GPU match
        Optional<MatchResult> gpuMatch = matchGpu(productName);
        if (gpuMatch.isPresent() && gpuMatch.get().confidence > 0.7) {
            return gpuMatch;
        }

        Optional<MatchResult> motherboardMatch = matchMotherboard(productName);
        if (motherboardMatch.isPresent() && motherboardMatch.get().confidence > 0.7) {
            return motherboardMatch;
        }

        // Return best match if confidence > 0.5
        List<MatchResult> allMatches = new ArrayList<>();
        cpuMatch.ifPresent(allMatches::add);
        gpuMatch.ifPresent(allMatches::add);
        motherboardMatch.ifPresent(allMatches::add);

        return allMatches.stream()
                .max(Comparator.comparingDouble(m -> m.confidence));
    }

    private Optional<MatchResult> matchCpu(String productName) {
        List<ReferenceCpu> allCpus = cpuRepository.findAll();
        if (allCpus.isEmpty()) {
            return Optional.empty();
        }

        MatchResult bestMatch = null;
        double bestConfidence = 0;

        for (ReferenceCpu cpu : allCpus) {
            double confidence = calculateCpuSimilarity(productName, cpu);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                Map<String, String> reasoning = buildCpuReasoning(productName, cpu, confidence);
                bestMatch = new MatchResult("CPU", cpu.getId(), cpu.getName(), confidence, reasoning);
            }
        }

        return bestMatch != null && bestConfidence > 0.3 ? Optional.of(bestMatch) : Optional.empty();
    }

    private Optional<MatchResult> matchGpu(String productName) {
        List<ReferenceGpu> allGpus = gpuRepository.findAll();
        if (allGpus.isEmpty()) {
            return Optional.empty();
        }

        MatchResult bestMatch = null;
        double bestConfidence = 0;

        for (ReferenceGpu gpu : allGpus) {
            double confidence = calculateGpuSimilarity(productName, gpu);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                Map<String, String> reasoning = buildGpuReasoning(productName, gpu, confidence);
                bestMatch = new MatchResult("GPU", gpu.getId(), gpu.getName(), confidence, reasoning);
            }
        }

        return bestMatch != null && bestConfidence > 0.3 ? Optional.of(bestMatch) : Optional.empty();
    }

    private Optional<MatchResult> matchMotherboard(String productName) {
        List<ReferenceMotherboard> allMotherboards = motherboardRepository.findAll();
        if (allMotherboards.isEmpty()) {
            return Optional.empty();
        }

        MatchResult bestMatch = null;
        double bestConfidence = 0;

        for (ReferenceMotherboard motherboard : allMotherboards) {
            double confidence = calculateMotherboardSimilarity(productName, motherboard);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                Map<String, String> reasoning = buildMotherboardReasoning(productName, motherboard, confidence);
                bestMatch = new MatchResult("MOTHERBOARD", motherboard.getId(), motherboard.getName(), confidence, reasoning);
            }
        }

        return bestMatch != null && bestConfidence > 0.3 ? Optional.of(bestMatch) : Optional.empty();
    }

    private double calculateMotherboardSimilarity(String productName, ReferenceMotherboard motherboard) {
        double score = 0;
        double totalWeight = 0;

        String prodNorm = normalizeProduct(productName);
        String refNorm = normalizeProduct(motherboard.getName());

        if (prodNorm.equals(refNorm)) {
            return 1.0;
        }

        if (prodNorm.contains(refNorm) || refNorm.contains(prodNorm)) {
            return 0.95;
        }

        String productSocket = extractSocketToken(prodNorm);
        if (productSocket != null && motherboard.getSocket() != null) {
            String refSocket = normalizeProduct(motherboard.getSocket()).replace(" ", "");
            if (!productSocket.equals(refSocket)) {
                return 0.0;
            }
        }

        String productDdr = extractDdrToken(prodNorm);
        if (productDdr != null && motherboard.getMemoryRamType() != null) {
            String refDdr = normalizeProduct(motherboard.getMemoryRamType());
            if (!productDdr.equals(refDdr)) {
                return 0.0;
            }
        }

        String productChipset = extractMotherboardChipsetToken(prodNorm);
        String referenceChipset = extractMotherboardChipsetToken(normalizeProduct(motherboard.getChipset()));
        if (productChipset != null && referenceChipset != null && !productChipset.equals(referenceChipset)) {
            return 0.0;
        }

        if (motherboard.getManufacturer() != null) {
            String mfgNorm = normalizeProduct(motherboard.getManufacturer());
            if (!mfgNorm.isBlank() && prodNorm.contains(mfgNorm)) {
                score += 0.20;
            }
        }
        totalWeight += 0.20;

        if (productSocket != null && motherboard.getSocket() != null) {
            score += 0.20;
        }
        totalWeight += 0.20;

        if (productChipset != null && referenceChipset != null) {
            score += 0.20;
        }
        totalWeight += 0.20;

        Set<String> prodTokens = tokenize(prodNorm);
        Set<String> refTokens = tokenize(refNorm);
        Set<String> commonTokens = new HashSet<>(prodTokens);
        commonTokens.retainAll(refTokens);
        double tokenSimilarity = (double) commonTokens.size() / Math.max(1, Math.max(prodTokens.size(), refTokens.size()));
        score += tokenSimilarity * 0.35;
        totalWeight += 0.35;

        double levenshtein = calculateLevenshteinSimilarity(prodNorm, refNorm);
        score += levenshtein * 0.05;
        totalWeight += 0.05;

        return totalWeight > 0 ? score / totalWeight : 0;
    }

    private double calculateCpuSimilarity(String productName, ReferenceCpu cpu) {
        double score = 0;
        double totalWeight = 0;

        // Normalize inputs
        String prodNorm = productName.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
        String refNorm = cpu.getName().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();

        // Exact match (1.0)
        if (prodNorm.equals(refNorm)) {
            return 1.0;
        }

        // Substring match (0.95)
        if (prodNorm.contains(refNorm) || refNorm.contains(prodNorm)) {
            return 0.95;
        }

        // Hard filters: model-number, core count, socket, suffix (PRO etc.)
        Set<String> prodModelNums = extractNumericModelTokens(prodNorm);
        Set<String> refModelNums = extractNumericModelTokens(refNorm);
        if (!prodModelNums.isEmpty() && !refModelNums.isEmpty()) {
            Set<String> common = new HashSet<>(prodModelNums);
            common.retainAll(refModelNums);
            if (common.isEmpty()) {
                return 0.0; // different numeric models (e.g., 3350 vs 3600)
            }
        }

        Integer prodCores = extractCoreCount(prodNorm);
        if (prodCores != null && cpu.getCores() != null && !prodCores.equals(cpu.getCores())) {
            return 0.0; // core count mismatch
        }

        String prodSocket = extractSocketToken(prodNorm);
        if (prodSocket != null && cpu.getSocket() != null && !prodSocket.equalsIgnoreCase(cpu.getSocket())) {
            return 0.0; // socket mismatch
        }

        String prodSuffix = extractSuffixToken(prodNorm);
        String refSuffix = extractSuffixToken(refNorm);
        if (prodSuffix != null && refSuffix != null && !prodSuffix.equals(refSuffix)) {
            return 0.0; // suffix (PRO/Ti/XT) mismatch
        }

        // Manufacturer match (0.25 weight)
        if (cpu.getManufacturer() != null) {
            String mfgNorm = cpu.getManufacturer().toLowerCase();
            if (prodNorm.contains(mfgNorm)) {
                score += 0.25;
            }
        }
        totalWeight += 0.25;

        // Token matching
        Set<String> prodTokens = tokenize(prodNorm);
        Set<String> refTokens = tokenize(refNorm);
        Set<String> commonTokens = new HashSet<>(prodTokens);
        commonTokens.retainAll(refTokens);

        double tokenSimilarity = (double) commonTokens.size() / Math.max(prodTokens.size(), refTokens.size());
        score += tokenSimilarity * 0.55;
        totalWeight += 0.55;

        // Levenshtein similarity
        double levenshtein = calculateLevenshteinSimilarity(prodNorm, refNorm);
        score += levenshtein * 0.2;
        totalWeight += 0.2;

        return totalWeight > 0 ? score / totalWeight : 0;
    }

    private Set<String> extractNumericModelTokens(String text) {
        Set<String> out = new HashSet<>();
        String[] tokens = text.split("\\s+");
        for (String t : tokens) {
            if (t.matches("\\d{3,5}")) {
                out.add(t);
            } else if (t.matches("[a-z]*\\d{3,5}[a-z]*")) {
                String digits = t.replaceAll("[^0-9]", "");
                if (digits.length() >= 3) out.add(digits);
            }
        }
        return out;
    }

    private Integer extractCoreCount(String text) {
        // look for patterns like '6 core', '6-core', '6-core' or '6core'
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{1,2})\\s*-?core");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String extractSocketToken(String text) {
        // common socket tokens
        String normalized = text.replace(" ", "");
        String[] sockets = new String[]{"am4","am5","lga1151","lga1200","lga1700","strx4","str4"};
        for (String s : sockets) {
            if (normalized.contains(s.toLowerCase())) return s.toLowerCase();
        }
        return null;
    }

    private String extractSuffixToken(String text) {
        // look for tokens like pro, ti, xt, super
        String[] suffixes = new String[]{"pro","ti","xt","super","k","s"};
        for (String s : suffixes) {
            if (text.contains(" " + s + " ") || text.endsWith(" " + s) || text.startsWith(s + " ") ) {
                return s;
            }
        }
        return null;
    }

    private double calculateGpuSimilarity(String productName, ReferenceGpu gpu) {
        double score = 0;
        double totalWeight = 0;

        // Normalize inputs
        String prodNorm = productName.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
        String refNorm = gpu.getName().toLowerCase().replaceAll("[^a-z0-9]", " ").trim();

        // Exact match (1.0)
        if (prodNorm.equals(refNorm)) {
            return 1.0;
        }

        // Substring match (0.95)
        if (prodNorm.contains(refNorm) || refNorm.contains(prodNorm)) {
            return 0.95;
        }

        // Hard filter: if parsed text clearly contains VRAM GB and it conflicts with reference memory,
        // this candidate is invalid and should be ignored.
        Set<Integer> productMemoryGb = extractGpuMemoryGbCandidates(prodNorm);
        if (gpu.getMemoryGb() != null && !productMemoryGb.isEmpty() && !productMemoryGb.contains(gpu.getMemoryGb())) {
            return 0.0;
        }

        // Prioritize GPU numbering/model scheme (e.g. 4090, 7800, a770).
        // If both sides have model numbers and they do not overlap, treat as a hard mismatch.
        Set<String> productModelNumbers = extractGpuModelNumberTokens(prodNorm);
        Set<String> referenceModelNumbers = extractGpuModelNumberTokens(refNorm);
        if (!productModelNumbers.isEmpty() && !referenceModelNumbers.isEmpty()) {
            Set<String> commonModelNumbers = new HashSet<>(productModelNumbers);
            commonModelNumbers.retainAll(referenceModelNumbers);
            if (commonModelNumbers.isEmpty()) {
                return 0.0;
            }
        }

        double modelNumberSimilarity = calculateGpuModelNumberSimilarity(prodNorm, refNorm);
        score += modelNumberSimilarity * 0.70;
        totalWeight += 0.70;

        // Manufacturer match (0.2 weight)
        if (gpu.getManufacturer() != null) {
            String mfgNorm = gpu.getManufacturer().toLowerCase();
            if (prodNorm.contains(mfgNorm)) {
                score += 0.2;
            }
        }
        totalWeight += 0.2;

        // Token matching
        Set<String> prodTokens = tokenize(prodNorm);
        Set<String> refTokens = tokenize(refNorm);
        Set<String> commonTokens = new HashSet<>(prodTokens);
        commonTokens.retainAll(refTokens);

        double tokenSimilarity = (double) commonTokens.size() / Math.max(prodTokens.size(), refTokens.size());
        score += tokenSimilarity * 0.18;
        totalWeight += 0.18;

        // Memory match bonus
        if (gpu.getMemoryGb() != null) {
            if (prodNorm.contains(gpu.getMemoryGb() + "gb") || prodNorm.contains(gpu.getMemoryGb() + "g")) {
                score += 0.15;
            }
        }
        totalWeight += 0.10;

        // Levenshtein similarity
        double levenshtein = calculateLevenshteinSimilarity(prodNorm, refNorm);
        score += levenshtein * 0.02;
        totalWeight += 0.02;

        return totalWeight > 0 ? score / totalWeight : 0;
    }

    private double calculateGpuModelNumberSimilarity(String productNorm, String referenceNorm) {
        Set<String> productModelTokens = extractGpuModelNumberTokens(productNorm);
        Set<String> referenceModelTokens = extractGpuModelNumberTokens(referenceNorm);

        if (productModelTokens.isEmpty() || referenceModelTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> common = new HashSet<>(productModelTokens);
        common.retainAll(referenceModelTokens);
        if (!common.isEmpty()) {
            return 1.0;
        }

        // Soft match for near model numbers like 4070 vs 4070ti naming variants.
        for (String p : productModelTokens) {
            for (String r : referenceModelTokens) {
                if (p.equals(r)) {
                    return 1.0;
                }
                if ((p.contains(r) || r.contains(p)) && (p.length() >= 3 && r.length() >= 3)) {
                    return 0.85;
                }
            }
        }

        return 0.0;
    }

    private Set<String> extractGpuModelNumberTokens(String normalizedText) {
        Set<String> result = new HashSet<>();
        String[] tokens = normalizedText.split("\\s+");

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }

            // Pure numeric model codes, e.g. 4090, 7800, 9070.
            if (t.matches("\\d{3,5}")) {
                result.add(t);
                continue;
            }

            // Alpha-numeric model codes, e.g. a770, rtx4090, 7600xt.
            if (t.matches("[a-z]{1,5}\\d{3,5}[a-z]{0,3}") || t.matches("\\d{3,5}[a-z]{1,3}")) {
                String digitsOnly = t.replaceAll("[^0-9]", "");
                if (digitsOnly.length() >= 3) {
                    result.add(digitsOnly);
                }
            }
        }

        return result;
    }

    private Set<Integer> extractGpuMemoryGbCandidates(String normalizedText) {
        Set<Integer> result = new HashSet<>();
        String[] tokens = normalizedText.split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].trim();
            if (t.isEmpty()) {
                continue;
            }

            // Pattern like: 16gb
            if (t.matches("\\d{1,3}gb")) {
                try {
                    result.add(Integer.parseInt(t.replace("gb", "")));
                } catch (NumberFormatException ignored) {
                }
                continue;
            }

            // Pattern like: 16 g or 16 gb
            if (t.matches("\\d{1,3}") && i + 1 < tokens.length) {
                String next = tokens[i + 1].trim();
                if ("g".equals(next) || "gb".equals(next)) {
                    try {
                        result.add(Integer.parseInt(t));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return result;
    }

    private Set<String> tokenize(String text) {
        return new HashSet<>(Arrays.asList(text.split("\\s+")));
    }

    private String normalizeProduct(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean looksLikeMotherboard(String productName) {
        String norm = normalizeProduct(productName);
        return norm.contains("motherboard")
                || norm.contains("mainboard")
                || norm.contains("materinskaya")
                || norm.matches(".*\\b(a|b|h|x|z)\\d{3}\\b.*")
                || norm.contains("lga ")
                || norm.contains("am4")
                || norm.contains("am5");
    }

    private String extractDdrToken(String text) {
        if (text.contains("ddr5")) return "ddr5";
        if (text.contains("ddr4")) return "ddr4";
        if (text.contains("ddr3")) return "ddr3";
        if (text.contains("ddr2")) return "ddr2";
        return null;
    }

    private String extractMotherboardChipsetToken(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([abghxz]\\d{3}|trx\\d|wrx\\d)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private Map<String, String> buildCpuReasoning(String product, ReferenceCpu ref, double conf) {
        Map<String, String> reasoning = new LinkedHashMap<>();
        reasoning.put("matchType", "CPU");
        reasoning.put("confidence", String.format("%.2f", conf));
        reasoning.put("referenceName", ref.getName());
        reasoning.put("inputProduct", product);
        if (ref.getManufacturer() != null) {
            reasoning.put("manufacturer", ref.getManufacturer());
        }
        if (ref.getCores() != null) {
            reasoning.put("cores", ref.getCores().toString());
        }
        if (ref.getSocket() != null) {
            reasoning.put("socket", ref.getSocket());
        }
        return reasoning;
    }

    private Map<String, String> buildGpuReasoning(String product, ReferenceGpu ref, double conf) {
        Map<String, String> reasoning = new LinkedHashMap<>();
        reasoning.put("matchType", "GPU");
        reasoning.put("confidence", String.format("%.2f", conf));
        reasoning.put("referenceName", ref.getName());
        reasoning.put("inputProduct", product);
        if (ref.getManufacturer() != null) {
            reasoning.put("manufacturer", ref.getManufacturer());
        }
        if (ref.getMemoryGb() != null) {
            reasoning.put("memory", ref.getMemoryGb() + " GB");
        }
        if (ref.getMemoryType() != null) {
            reasoning.put("memoryType", ref.getMemoryType());
        }
        if (ref.getTdp() != null) {
            reasoning.put("tdp", ref.getTdp() + " W");
        }
        return reasoning;
    }

    private Map<String, String> buildMotherboardReasoning(String product, ReferenceMotherboard ref, double conf) {
        Map<String, String> reasoning = new LinkedHashMap<>();
        reasoning.put("matchType", "MOTHERBOARD");
        reasoning.put("confidence", String.format("%.2f", conf));
        reasoning.put("referenceName", ref.getName());
        reasoning.put("inputProduct", product);
        if (ref.getManufacturer() != null) {
            reasoning.put("manufacturer", ref.getManufacturer());
        }
        if (ref.getSocket() != null) {
            reasoning.put("socket", ref.getSocket());
        }
        if (ref.getChipset() != null) {
            reasoning.put("chipset", ref.getChipset());
        }
        if (ref.getMemoryRamType() != null) {
            reasoning.put("ramType", ref.getMemoryRamType());
        }
        return reasoning;
    }
}
