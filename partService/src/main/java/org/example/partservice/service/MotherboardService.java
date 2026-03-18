package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.Motherboard;
import org.example.partservice.repository.MotherboardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MotherboardService {

    private final MotherboardRepository motherboardRepository;

    public Page<Motherboard> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String socket,
            String formFactor,
            Integer minMemorySlots,
            Integer maxMemorySlots,
            Integer minMaxMemory,
            String color,
            Pageable pageable
    ) {
        Specification<Motherboard> spec = PartFilterSpec.<Motherboard>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .like("socket", socket)
                .like("formFactor", formFactor)
                .gteInt("memorySlots", minMemorySlots)
                .lteInt("memorySlots", maxMemorySlots)
                .gteInt("maxMemory", minMaxMemory)
                .like("color", color)
                .build();

        return motherboardRepository.findAll(spec, pageable);
    }

    public Optional<Motherboard> findById(Long id) {
        return motherboardRepository.findById(id);
    }
}
