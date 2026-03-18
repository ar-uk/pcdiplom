package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.InternalHardDrive;
import org.example.partservice.repository.InternalHardDriveRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InternalHardDriveService {

    private final InternalHardDriveRepository internalHardDriveRepository;

    public Page<InternalHardDrive> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minCapacityGb,
            Integer maxCapacityGb,
            String driveType,
            String formFactor,
            String driveInterface,
            Pageable pageable
    ) {
        Specification<InternalHardDrive> spec = PartFilterSpec.<InternalHardDrive>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .gteInt("capacityGb", minCapacityGb)
                .lteInt("capacityGb", maxCapacityGb)
                .like("driveType", driveType)
                .like("formFactor", formFactor)
                .like("driveInterface", driveInterface)
                .build();

        return internalHardDriveRepository.findAll(spec, pageable);
    }

    public Optional<InternalHardDrive> findById(Long id) {
        return internalHardDriveRepository.findById(id);
    }
}
