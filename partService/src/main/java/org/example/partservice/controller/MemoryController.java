package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.Memory;
import org.example.partservice.service.MemoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * GET /api/memory
     * Optional query params:
     *   name, minPrice, maxPrice, ddr, minSpeedMhz, maxSpeedMhz,
     *   minGbPerStick, maxGbPerStick, color,
     *   page, size, sort
     *
     * Examples:
     *   /api/memory?ddr=5&minGbPerStick=16&maxPrice=150
     *   /api/memory?color=black&sort=priceUsd,asc
     */
    @GetMapping
    public Page<Memory> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer ddr,
            @RequestParam(required = false) Integer minSpeedMhz,
            @RequestParam(required = false) Integer maxSpeedMhz,
            @RequestParam(required = false) Integer minGbPerStick,
            @RequestParam(required = false) Integer maxGbPerStick,
            @RequestParam(required = false) String color,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return memoryService.findAll(
                name, minPrice, maxPrice,
                ddr, minSpeedMhz, maxSpeedMhz,
                minGbPerStick, maxGbPerStick,
                color, pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Memory> getById(@PathVariable Long id) {
        return memoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
