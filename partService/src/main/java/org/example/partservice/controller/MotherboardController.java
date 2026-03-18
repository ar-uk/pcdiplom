package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.Motherboard;
import org.example.partservice.service.MotherboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/motherboard")
@RequiredArgsConstructor
public class MotherboardController {

    private final MotherboardService motherboardService;

    /**
     * GET /api/motherboard
     * Optional query params:
     *   name, minPrice, maxPrice, socket, formFactor,
     *   minMemorySlots, maxMemorySlots, minMaxMemory, color,
     *   page, size, sort
     *
     * Examples:
     *   /api/motherboard?socket=AM5&formFactor=ATX
     *   /api/motherboard?minMemorySlots=4&maxPrice=200&sort=priceUsd,asc
     */
    @GetMapping
    public Page<Motherboard> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String socket,
            @RequestParam(required = false) String formFactor,
            @RequestParam(required = false) Integer minMemorySlots,
            @RequestParam(required = false) Integer maxMemorySlots,
            @RequestParam(required = false) Integer minMaxMemory,
            @RequestParam(required = false) String color,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return motherboardService.findAll(
                name, minPrice, maxPrice,
                socket, formFactor,
                minMemorySlots, maxMemorySlots,
                minMaxMemory, color,
                pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Motherboard> getById(@PathVariable Long id) {
        return motherboardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
