package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.InternalHardDrive;
import org.example.partservice.service.InternalHardDriveService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class InternalHardDriveController {

    private final InternalHardDriveService internalHardDriveService;

    /**
     * GET /api/storage
     * Optional query params:
     *   name, minPrice, maxPrice, minCapacityGb, maxCapacityGb,
     *   driveType, formFactor, driveInterface,
     *   page, size, sort
     *
     * Examples:
     *   /api/storage?driveType=SSD&minCapacityGb=500
     *   /api/storage?driveInterface=NVMe&maxPrice=100&sort=pricePerGb,asc
     */
    @GetMapping
    public Page<InternalHardDrive> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minCapacityGb,
            @RequestParam(required = false) Integer maxCapacityGb,
            @RequestParam(required = false) String driveType,
            @RequestParam(required = false) String formFactor,
            @RequestParam(required = false) String driveInterface,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return internalHardDriveService.findAll(
                name, minPrice, maxPrice,
                minCapacityGb, maxCapacityGb,
                driveType, formFactor, driveInterface,
                pageable
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternalHardDrive> getById(@PathVariable Long id) {
        return internalHardDriveService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
