package org.example.partservice.controller;

import org.example.partservice.model.ReferenceCpuMatch;
import org.example.partservice.model.ReferenceGpuMatch;
import org.example.partservice.model.ReferenceMotherboardMatch;
import org.example.partservice.repository.ReferenceCpuMatchRepository;
import org.example.partservice.repository.ReferenceGpuMatchRepository;
import org.example.partservice.repository.ReferenceMotherboardMatchRepository;
import org.example.partservice.service.BuildCoresLoaderService;
import org.example.partservice.service.ProductMatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reference")
public class ReferenceDataController {
    @Autowired
    private BuildCoresLoaderService loaderService;

    @Autowired
    private ProductMatcherService matcherService;

    @Autowired
    private ReferenceCpuMatchRepository cpuMatchRepository;

    @Autowired
    private ReferenceGpuMatchRepository gpuMatchRepository;

    @Autowired
    private ReferenceMotherboardMatchRepository motherboardMatchRepository;

    @PostMapping("/load/cpu")
    public ResponseEntity<String> loadCpuData() {
        try {
            loaderService.loadCpuData();
            return ResponseEntity.ok("CPU reference data loaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading CPU data: " + e.getMessage());
        }
    }

    @PostMapping("/load/gpu")
    public ResponseEntity<String> loadGpuData() {
        try {
            loaderService.loadGpuData();
            return ResponseEntity.ok("GPU reference data loaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading GPU data: " + e.getMessage());
        }
    }

    @PostMapping("/load/motherboard")
    public ResponseEntity<String> loadMotherboardData() {
        try {
            loaderService.loadMotherboardData();
            return ResponseEntity.ok("Motherboard reference data loaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading Motherboard data: " + e.getMessage());
        }
    }

    @PostMapping("/load/all")
    public ResponseEntity<String> loadAllData() {
        try {
            loaderService.loadCpuData();
            loaderService.loadGpuData();
            loaderService.loadMotherboardData();
            return ResponseEntity.ok("All reference data loaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading data: " + e.getMessage());
        }
    }

    @PostMapping("/match")
    public ResponseEntity<?> matchProduct(@RequestParam String productName) {
        Optional<ProductMatcherService.MatchResult> result = matcherService.matchProduct(productName);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.status(404).body("No match found for: " + productName);
        }
    }

    @GetMapping("/matched-cpu")
    public ResponseEntity<List<ReferenceCpuMatch>> getMatchedCpus(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "500") int size) {
        List<ReferenceCpuMatch> matches = cpuMatchRepository.findAll(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/matched-gpu")
    public ResponseEntity<List<ReferenceGpuMatch>> getMatchedGpus(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "500") int size) {
        List<ReferenceGpuMatch> matches = gpuMatchRepository.findAll(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/matched-motherboard")
    public ResponseEntity<List<ReferenceMotherboardMatch>> getMatchedMotherboards(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "500") int size) {
        List<ReferenceMotherboardMatch> matches = motherboardMatchRepository.findAll(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(matches);
    }
}
