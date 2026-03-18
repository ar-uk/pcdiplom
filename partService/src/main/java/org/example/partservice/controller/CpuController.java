package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.Cpu;
import org.example.partservice.service.CpuService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cpu")
@RequiredArgsConstructor
public class CpuController {

    private final CpuService cpuService;

    /**
     * GET /api/cpu
     * Optional query params:
     *   name, minPrice, maxPrice, minCoreCount, maxCoreCount,
     *   microarchitecture, minTdp, maxTdp,
     *   page, size, sort (from Pageable)
     *
     * Examples:
     *   /api/cpu?name=ryzen&minCoreCount=6&maxPrice=300&page=0&size=20
     *   /api/cpu?microarchitecture=zen3&sort=priceUsd,asc
     */
    @GetMapping
    public Page<Cpu> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minCoreCount,
            @RequestParam(required = false) Integer maxCoreCount,
            @RequestParam(required = false) String microarchitecture,
            @RequestParam(required = false) Integer minTdp,
            @RequestParam(required = false) Integer maxTdp,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return cpuService.findAll(
                name, minPrice, maxPrice,
                minCoreCount, maxCoreCount,
                microarchitecture,
                minTdp, maxTdp,
                pageable
        );
    }

    /**
     * GET /api/cpu/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cpu> getById(@PathVariable Long id) {
        return cpuService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
