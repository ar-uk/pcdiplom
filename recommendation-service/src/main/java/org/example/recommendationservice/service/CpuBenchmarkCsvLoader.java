package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.CpuBenchmark;
import org.example.recommendationservice.repository.CpuBenchmarkRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CpuBenchmarkCsvLoader implements ApplicationRunner {

    private static final String CSV_PATH = "cpu_reference_scores.csv";
    private static final String SOURCE_NAME = "cpu_reference_scores.csv";
    private static final String SOURCE_VERSION = "2026-05-09";

    private final CpuBenchmarkRepository cpuBenchmarkRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        loadCsv();
    }

    private void loadCsv() throws Exception {
        ClassPathResource resource = new ClassPathResource(CSV_PATH);
        if (!resource.exists()) {
            return;
        }

        List<CpuBenchmark> benchmarks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                return;
            }

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length < 3) {
                    continue;
                }

                CpuBenchmark benchmark = new CpuBenchmark();
                benchmark.setCpuName(CpuNameNormalizer.canonicalize(columns[0]));
                benchmark.setScore1080p(parseDouble(columns[1]));
                benchmark.setScore1440p(parseDouble(columns[2]));
                if (columns.length > 3) {
                    benchmark.setTdpWatts(parseInteger(columns[3]));
                }
                benchmark.setSourceName(SOURCE_NAME);
                benchmark.setSourceVersion(SOURCE_VERSION);
                benchmark.setImportedAt(LocalDateTime.now());
                benchmarks.add(benchmark);
            }
        }

        cpuBenchmarkRepository.deleteAllInBatch();
        cpuBenchmarkRepository.saveAll(benchmarks);
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception exception) {
            return null;
        }
    }
}