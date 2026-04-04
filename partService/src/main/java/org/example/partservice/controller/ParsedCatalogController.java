package org.example.partservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.partservice.model.ParsedCpu;
import org.example.partservice.model.ParsedCpuCooler;
import org.example.partservice.model.ParsedInternalHardDrive;
import org.example.partservice.model.ParsedMemory;
import org.example.partservice.model.ParsedMotherboard;
import org.example.partservice.model.ParsedPcCase;
import org.example.partservice.model.ParsedPowerSupply;
import org.example.partservice.model.ParsedVideoCard;
import org.example.partservice.repository.ParsedCpuRepository;
import org.example.partservice.repository.ParsedCpuCoolerRepository;
import org.example.partservice.repository.ParsedInternalHardDriveRepository;
import org.example.partservice.repository.ParsedMemoryRepository;
import org.example.partservice.repository.ParsedMotherboardRepository;
import org.example.partservice.repository.ParsedPcCaseRepository;
import org.example.partservice.repository.ParsedPowerSupplyRepository;
import org.example.partservice.repository.ParsedVideoCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parsed")
public class ParsedCatalogController {

    private final ParsedCpuRepository parsedCpuRepository;
    private final ParsedCpuCoolerRepository parsedCpuCoolerRepository;
    private final ParsedVideoCardRepository parsedVideoCardRepository;
    private final ParsedPowerSupplyRepository parsedPowerSupplyRepository;
    private final ParsedPcCaseRepository parsedPcCaseRepository;
    private final ParsedMemoryRepository parsedMemoryRepository;
    private final ParsedInternalHardDriveRepository parsedInternalHardDriveRepository;
    private final ParsedMotherboardRepository parsedMotherboardRepository;

    @GetMapping("/cpu")
    public Page<ParsedCpu> cpu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedCpuRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping({"/cpu-cooler", "/cooling"})
    public Page<ParsedCpuCooler> cpuCooler(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedCpuCoolerRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/video-card")
    public Page<ParsedVideoCard> gpu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedVideoCardRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/power-supply")
    public Page<ParsedPowerSupply> psu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedPowerSupplyRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/pc-case")
    public Page<ParsedPcCase> caseCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedPcCaseRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/memory")
    public Page<ParsedMemory> memory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedMemoryRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/internal-hard-drive")
    public Page<ParsedInternalHardDrive> storage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedInternalHardDriveRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }

    @GetMapping("/motherboard")
    public Page<ParsedMotherboard> motherboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return parsedMotherboardRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "priceKzt")));
    }
}