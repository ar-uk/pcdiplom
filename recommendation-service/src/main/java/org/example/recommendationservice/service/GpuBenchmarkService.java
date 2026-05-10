package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.GpuBenchmark;
import org.example.recommendationservice.repository.GpuBenchmarkRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GpuBenchmarkService {

    private final GpuBenchmarkRepository repository;

    public Optional<GpuBenchmark> findByName(String gpuName) {
        if (gpuName == null || gpuName.isBlank()) {
            return Optional.empty();
        }
        String canonicalName = GpuNameNormalizer.canonicalize(gpuName);
        return repository.findByCanonicalNameIgnoreCase(canonicalName)
                .or(() -> repository.findByGpuNameIgnoreCase(gpuName));
    }
}