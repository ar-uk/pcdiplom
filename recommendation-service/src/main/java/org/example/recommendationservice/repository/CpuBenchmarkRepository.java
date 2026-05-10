package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.CpuBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CpuBenchmarkRepository extends JpaRepository<CpuBenchmark, UUID> {

    Optional<CpuBenchmark> findByCpuNameIgnoreCase(String cpuName);
}