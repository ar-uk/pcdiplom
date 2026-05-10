package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.PowerSupply;
import org.example.partservice.service.PowerSupplyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/psu")
@RequiredArgsConstructor
public class PowerSupplyController {

    private final PowerSupplyService powerSupplyService;

    @GetMapping
    public Page<PowerSupply> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String psuType,
            @RequestParam(required = false) String efficiency,
            @RequestParam(required = false) Integer minWattage,
            @RequestParam(required = false) Integer maxWattage,
            @RequestParam(required = false) String modular,
            @RequestParam(required = false) String color,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return powerSupplyService.findAll(
                name, minPrice, maxPrice,
                psuType, efficiency,
                minWattage, maxWattage,
                modular, color,
                pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PowerSupply> getById(@PathVariable Long id) {
        return powerSupplyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
