package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.GpuBenchmark;
import org.example.recommendationservice.repository.GpuBenchmarkRepository;
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
public class GpuBenchmarkCsvLoader implements ApplicationRunner {

    private static final String CSV_PATH = "gpu_reference_scores.csv";
    private static final String SOURCE_NAME = "gpu_reference_scores.csv";
    private static final String SOURCE_VERSION = "2026-05-09";

    private final GpuBenchmarkRepository gpuBenchmarkRepository;

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

        List<GpuBenchmark> benchmarks = new ArrayList<>();
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
                if (columns.length < 4) {
                    continue;
                }

                GpuBenchmark benchmark = new GpuBenchmark();
                benchmark.setGpuName(columns[0].trim());
                benchmark.setCanonicalName(GpuNameNormalizer.canonicalize(columns[0]));
                benchmark.setScore1080p(parseDouble(columns[1]));
                benchmark.setScore1440p(parseDouble(columns[2]));
                benchmark.setScore4k(parseDouble(columns[3]));
                if (columns.length > 4) {
                    benchmark.setVramGb(parseInteger(columns[4]));
                }
                if (columns.length > 5) {
                    benchmark.setTdpWatts(parseInteger(columns[5]));
                }
                benchmark.setSourceName(SOURCE_NAME);
                benchmark.setSourceVersion(SOURCE_VERSION);
                benchmark.setImportedAt(LocalDateTime.now());
                benchmarks.add(benchmark);
            }
        }

        gpuBenchmarkRepository.deleteAllInBatch();
        gpuBenchmarkRepository.saveAll(benchmarks);
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