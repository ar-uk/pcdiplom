package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.CpuBenchmark;
import org.example.recommendationservice.repository.CpuBenchmarkRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CpuBenchmarkService {

    private final CpuBenchmarkRepository repository;

    public Optional<CpuBenchmark> findByName(String cpuName) {
        if (cpuName == null || cpuName.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCpuNameIgnoreCase(CpuNameNormalizer.canonicalize(cpuName));
    }
}