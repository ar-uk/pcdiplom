package org.example.recommendationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.service.ComponentFilterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

//REST Controller for filtering hardware components by various criteria.
//Provides endpoints for filtering CPUs, GPUs, motherboards, memory, PSUs, and storage.

@RestController
@RequestMapping("/api/components/filter")
@RequiredArgsConstructor
public class ComponentFilterController {

    private final ComponentFilterService filterService;

    @GetMapping("/cpus")
    public ResponseEntity<List<BuildResponse.PartDto>> getAllCpus(
            @RequestParam(required = false) String socket) {
        List<BuildResponse.PartDto> cpus = socket != null ? 
                filterService.filterCpusBySocket(socket) : 
                filterService.getAllCpus();
        return ResponseEntity.ok(cpus);
    }


    @GetMapping("/gpus")
    public ResponseEntity<List<BuildResponse.PartDto>> getAllGpus(
            @RequestParam(required = false) Integer minVram) {
        List<BuildResponse.PartDto> gpus = minVram != null ? 
                filterService.filterGpusByVram(minVram) : 
                filterService.getAllGpus();
        return ResponseEntity.ok(gpus);
    }

    @GetMapping("/motherboards")
    public ResponseEntity<List<BuildResponse.PartDto>> getAllMotherboards(
            @RequestParam(required = false) String socket) {
        List<BuildResponse.PartDto> motherboards = socket != null ? 
                filterService.filterMotherboardsBySocket(socket) : 
                filterService.getAllMotherboards();
        return ResponseEntity.ok(motherboards);
    }

    @GetMapping("/memory")
    public ResponseEntity<List<BuildResponse.PartDto>> getMemory(
            @RequestParam(required = false) String ddrType,
            @RequestParam(required = false) Integer minCapacityGb) {
        List<BuildResponse.PartDto> memory;
        
        if (ddrType != null) {
            memory = filterService.filterMemoryByType(ddrType);
        } else {
            memory = filterService.getAllMemory();
        }
        
        if (minCapacityGb != null) {
            memory = filterService.filterMemoryByCapacity(minCapacityGb);
        }
        
        return ResponseEntity.ok(memory);
    }

    @GetMapping("/psus")
    public ResponseEntity<List<BuildResponse.PartDto>> getPsus(
            @RequestParam(required = false) Integer minWattage) {
        List<BuildResponse.PartDto> psus = minWattage != null ? 
                filterService.filterPsuByWattage(minWattage) : 
                filterService.getAllPsus();
        return ResponseEntity.ok(psus);
    }

    @GetMapping("/storage")
    public ResponseEntity<List<BuildResponse.PartDto>> getStorage(
            @RequestParam(required = false) Integer minCapacityGb) {
        List<BuildResponse.PartDto> storage = minCapacityGb != null ? 
                filterService.filterStorageByCapacity(minCapacityGb) : 
                filterService.getAllStorage();
        return ResponseEntity.ok(storage);
    }

    @GetMapping("/cases")
    public ResponseEntity<List<BuildResponse.PartDto>> getCases() {
        return ResponseEntity.ok(filterService.getAllCases());
    }




    //@param tier One of: BUDGET, MID, HIGH, FLAGSHIP
    @GetMapping("/cpus/tier/{tier}")
    public ResponseEntity<List<BuildResponse.PartDto>> getCpusByTier(
            @PathVariable String tier) {
        return ResponseEntity.ok(filterService.filterCpusByTier(tier));
    }


    @GetMapping("/sockets/cpu")
    public ResponseEntity<Set<String>> getAvailableCpuSockets() {
        return ResponseEntity.ok(filterService.getAvailableCpuSockets());
    }


    @GetMapping("/sockets/motherboard")
    public ResponseEntity<Set<String>> getAvailableMotherboardSockets() {
        return ResponseEntity.ok(filterService.getAvailableMotherboardSockets());
    }


    @GetMapping("/memory-types")
    public ResponseEntity<Set<String>> getAvailableMemoryTypes() {
        return ResponseEntity.ok(filterService.getAvailableMemoryTypes());
    }

    @GetMapping("/psu-wattages")
    public ResponseEntity<List<Integer>> getAvailablePsuWattages() {
        return ResponseEntity.ok(filterService.getAvailablePsuWattages());
    }

    /**
     Filter components by price range.
     @param minPrice Minimum price in KZT
     @param maxPrice Maximum price in KZT
     @param category Component category (cpu, gpu, motherboard, memory, psu, storage, case)
     **/
    @GetMapping("/price-range")
    public ResponseEntity<List<BuildResponse.PartDto>> getComponentsByPriceRange(
            @RequestParam(required = false) String category,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        
        List<BuildResponse.PartDto> components;
        
        if ("cpu".equalsIgnoreCase(category)) {
            components = filterService.getAllCpus();
        } else if ("gpu".equalsIgnoreCase(category)) {
            components = filterService.getAllGpus();
        } else if ("motherboard".equalsIgnoreCase(category)) {
            components = filterService.getAllMotherboards();
        } else if ("memory".equalsIgnoreCase(category)) {
            components = filterService.getAllMemory();
        } else if ("psu".equalsIgnoreCase(category)) {
            components = filterService.getAllPsus();
        } else if ("storage".equalsIgnoreCase(category)) {
            components = filterService.getAllStorage();
        } else if ("case".equalsIgnoreCase(category)) {
            components = filterService.getAllCases();
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(filterService.filterByPriceRange(components, minPrice, maxPrice));
    }


    //Get motherboards compatible with a CPU socket.
    @GetMapping("/motherboards-for-cpu/{cpuSocket}")
    public ResponseEntity<List<BuildResponse.PartDto>> getMotherboardsForCpu(
            @PathVariable String cpuSocket) {
        return ResponseEntity.ok(filterService.filterMotherboardsForCpuSocket(cpuSocket));
    }


     //@param memoryType The motherboard's memory type (e.g., "DDR5", "DDR4")
    @GetMapping("/memory-for-motherboard/{memoryType}")
    public ResponseEntity<List<BuildResponse.PartDto>> getMemoryForMotherboard(
            @PathVariable String memoryType) {
        return ResponseEntity.ok(filterService.filterMemoryForMotherboard(memoryType));
    }
}
