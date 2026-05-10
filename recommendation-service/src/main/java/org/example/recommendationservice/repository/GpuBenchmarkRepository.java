package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.GpuBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GpuBenchmarkRepository extends JpaRepository<GpuBenchmark, UUID> {

    Optional<GpuBenchmark> findByCanonicalNameIgnoreCase(String canonicalName);

    Optional<GpuBenchmark> findByGpuNameIgnoreCase(String gpuName);
}