package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.PcCase;
import org.example.partservice.repository.PcCaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PcCaseService {

    private final PcCaseRepository pcCaseRepository;

    public Page<PcCase> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String caseType,
            String color,
            String sidePanel,
            Pageable pageable
    ) {
        Specification<PcCase> spec = PartFilterSpec.<PcCase>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .like("caseType", caseType)
                .like("color", color)
                .like("sidePanel", sidePanel)
                .build();

        return pcCaseRepository.findAll(spec, pageable);
    }

    public Optional<PcCase> findById(Long id) {
        return pcCaseRepository.findById(id);
    }
}
