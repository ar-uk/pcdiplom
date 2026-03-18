package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.VideoCard;
import org.example.partservice.service.VideoCardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/gpu")
@RequiredArgsConstructor
public class VideoCardController {

    private final VideoCardService videoCardService;

    /**
     * GET /api/gpu
     * Optional query params:
     *   name, minPrice, maxPrice, chipset, minMemoryGb, maxMemoryGb,
     *   minBoostClock, maxLengthMm, color,
     *   page, size, sort
     *
     * Examples:
     *   /api/gpu?chipset=RTX&minMemoryGb=8&maxPrice=500
     *   /api/gpu?maxLengthMm=300&sort=priceUsd,asc
     */
    @GetMapping
    public Page<VideoCard> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String chipset,
            @RequestParam(required = false) Integer minMemoryGb,
            @RequestParam(required = false) Integer maxMemoryGb,
            @RequestParam(required = false) Integer minBoostClock,
            @RequestParam(required = false) Integer maxLengthMm,
            @RequestParam(required = false) String color,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return videoCardService.findAll(
                name, minPrice, maxPrice,
                chipset, minMemoryGb, maxMemoryGb,
                minBoostClock, maxLengthMm,
                color, pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoCard> getById(@PathVariable Long id) {
        return videoCardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
