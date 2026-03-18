package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.Memory;
import org.example.partservice.repository.MemoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public Page<Memory> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer ddr,
            Integer minSpeedMhz,
            Integer maxSpeedMhz,
            Integer minGbPerStick,
            Integer maxGbPerStick,
            String color,
            Pageable pageable
    ) {
        Specification<Memory> spec = PartFilterSpec.<Memory>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .eq("ddr", ddr)
                .gteInt("speedMhz", minSpeedMhz)
                .lteInt("speedMhz", maxSpeedMhz)
                .gteInt("gbPerStick", minGbPerStick)
                .lteInt("gbPerStick", maxGbPerStick)
                .like("color", color)
                .build();

        return memoryRepository.findAll(spec, pageable);
    }

    public Optional<Memory> findById(Long id) {
        return memoryRepository.findById(id);
    }
}
