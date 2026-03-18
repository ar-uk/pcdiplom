package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.PcCase;
import org.example.partservice.service.PcCaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/case")
@RequiredArgsConstructor
public class PcCaseController {

    private final PcCaseService pcCaseService;

    /**
     * GET /api/case
     * Optional query params:
     *   name, minPrice, maxPrice, caseType, color, sidePanel,
     *   page, size, sort
     *
     * Examples:
     *   /api/case?caseType=ATX&sidePanel=Tempered+Glass
     *   /api/case?color=black&maxPrice=80&sort=priceUsd,asc
     */
    @GetMapping
    public Page<PcCase> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String caseType,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String sidePanel,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return pcCaseService.findAll(
                name, minPrice, maxPrice,
                caseType, color, sidePanel,
                pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PcCase> getById(@PathVariable Long id) {
        return pcCaseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
