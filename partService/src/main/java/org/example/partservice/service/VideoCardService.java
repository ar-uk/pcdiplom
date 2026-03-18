package org.example.partservice.service;

import lombok.RequiredArgsConstructor;
import org.example.partservice.filter.PartFilterSpec;
import org.example.partservice.model.VideoCard;
import org.example.partservice.repository.VideoCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoCardService {

    private final VideoCardRepository videoCardRepository;

    public Page<VideoCard> findAll(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String chipset,
            Integer minMemoryGb,
            Integer maxMemoryGb,
            Integer minBoostClock,
            Integer maxLengthMm,
            String color,
            Pageable pageable
    ) {
        Specification<VideoCard> spec = PartFilterSpec.<VideoCard>builder()
                .like("name", name)
                .gte("priceUsd", minPrice)
                .lte("priceUsd", maxPrice)
                .like("chipset", chipset)
                .gteInt("memoryGb", minMemoryGb)
                .lteInt("memoryGb", maxMemoryGb)
                .gteInt("boostClock", minBoostClock)
                .lteInt("lengthMm", maxLengthMm)
                .like("color", color)
                .build();

        return videoCardRepository.findAll(spec, pageable);
    }

    public Optional<VideoCard> findById(Long id) {
        return videoCardRepository.findById(id);
    }
}
