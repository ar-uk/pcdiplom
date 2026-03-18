package org.example.partservice.service;

import
        lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.Cpu;
import org.example.partservice.repository.CpuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CpuService {

    private final CpuRepository cpuRepository;

    public Page<Cpu> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minCoreCount,
            Integer maxCoreCount,
            String microarchitecture,
            Integer minTdp,
            Integer maxTdp,
            Pageable pageable
    ) {
        Specification<Cpu> spec = PartFilterSpec.<Cpu>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .gteInt("coreCount", minCoreCount)
                .lteInt("coreCount", maxCoreCount)
                .like("microarchitecture", microarchitecture)
                .gteInt("tdp", minTdp)
                .lteInt("tdp", maxTdp)
                .build();

        return cpuRepository.findAll(spec, pageable);
    }

    public Optional<Cpu> findById(Long id) {
        return cpuRepository.findById(id);
    }
}
