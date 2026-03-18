package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.PowerSupply;
import org.example.partservice.repository.PowerSupplyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PowerSupplyService {

    private final PowerSupplyRepository powerSupplyRepository;

    public Page<PowerSupply> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String psuType,
            String efficiency,
            Integer minWattage,
            Integer maxWattage,
            String modular,
            String color,
            Pageable pageable
    ) {
        Specification<PowerSupply> spec = PartFilterSpec.<PowerSupply>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .like("psuType", psuType)
                .like("efficiency", efficiency)
                .gteInt("wattage", minWattage)
                .lteInt("wattage", maxWattage)
                .like("modular", modular)
                .like("color", color)
                .build();

        return powerSupplyRepository.findAll(spec, pageable);
    }

    public Optional<PowerSupply> findById(Long id) {
        return powerSupplyRepository.findById(id);
    }
}
