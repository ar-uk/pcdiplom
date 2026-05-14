package org.example.recommendationservice.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.service.ComponentCompatibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;


//REST Controller for checking hardware component compatibility.
//Provides endpoints for validating socket compatibility, memory types, PSU wattage, etc.

@RestController
@RequestMapping("/api/components/compatibility")
@RequiredArgsConstructor
public class ComponentCompatibilityController {

    private final ComponentCompatibilityService compatibilityService;


     //Check if a CPU socket is compatible with a motherboard socket.

    @PostMapping("/socket-check")
    public ResponseEntity<CompatibilityResponse> checkSocketCompatibility(
            @RequestBody SocketCheckRequest request) {
        boolean compatible = compatibilityService.compatibleSocket(request.cpuSocket, request.motherboardSocket);
        String reason = compatible ? 
                "CPU socket " + request.cpuSocket + " is compatible with motherboard socket " + request.motherboardSocket :
                "CPU socket " + request.cpuSocket + " is NOT compatible with motherboard socket " + request.motherboardSocket;
        return ResponseEntity.ok(new CompatibilityResponse(compatible, reason));
    }


    //Check if memory type is compatible with motherboard.

    @PostMapping("/memory-check")
    public ResponseEntity<CompatibilityResponse> checkMemoryCompatibility(
            @RequestBody MemoryCheckRequest request) {
        boolean compatible = compatibilityService.compatibleMemory(request.motherboardMemoryType, request.ramMemoryType);
        String reason = compatible ? 
                request.ramMemoryType + " is compatible with motherboard requiring " + request.motherboardMemoryType :
                request.ramMemoryType + " is NOT compatible with motherboard requiring " + request.motherboardMemoryType;
        return ResponseEntity.ok(new CompatibilityResponse(compatible, reason));
    }

     //Check if PSU has sufficient wattage for CPU and GPU.

    @PostMapping("/psu-check")
    public ResponseEntity<CompatibilityResponse> checkPsuWattage(
            @RequestBody PsuCheckRequest request) {
        boolean sufficient = compatibilityService.sufficientPsWattage(
                request.psuWattage, request.cpuName, request.gpuName, request.preferHeadroom);
        String reason = sufficient ?
                "PSU " + request.psuWattage + "W has sufficient wattage for CPU + GPU" :
                "PSU " + request.psuWattage + "W may be insufficient for CPU + GPU power draw";
        return ResponseEntity.ok(new CompatibilityResponse(sufficient, reason));
    }

    //Check if CPU and GPU tier pairing is balanced.

    @PostMapping("/cpu-gpu-pairing-check")
    public ResponseEntity<CompatibilityResponse> checkCpuGpuPairing(
            @RequestBody CpuGpuPairingRequest request) {
        boolean allowed = compatibilityService.cpuGpuPairingAllowed(
                request.cpuName, request.gpuName, request.useCase, request.resolution);
        String reason = allowed ?
                "CPU and GPU tier pairing is balanced" :
                "CPU and GPU tier pairing may cause performance bottleneck";
        return ResponseEntity.ok(new CompatibilityResponse(allowed, reason));
    }


     //Validate an entire build for compatibility across all components.
     //Performs comprehensive checks on socket, memory, PSU, and tier pairing.
     //@return BuildCompatibilityReport with detailed findings

    @PostMapping("/validate-build")
    public ResponseEntity<BuildCompatibilityReport> validateBuild(
            @RequestBody BuildValidationRequest request) {
        
        // Convert request to parts map
        Map<String, BuildResponse.PartDto> parts = new HashMap<>();
        parts.put("cpu", convertToPartDto(request.cpu));
        parts.put("gpu", convertToPartDto(request.gpu));
        parts.put("motherboard", convertToPartDto(request.motherboard));
        parts.put("memory", convertToPartDto(request.memory));
        parts.put("powerSupply", convertToPartDto(request.psu));
        
        ComponentCompatibilityService.CompatibilityCheckResult result = 
                compatibilityService.validateBuild(parts);
        
        return ResponseEntity.ok(new BuildCompatibilityReport(
                result.compatible,
                result.issues,
                result.warnings
        ));
    }

     //Quick endpoint to check if socket is available and get compatible components.
     //@param socket The socket type (e.g., "AM5", "LGA1700")
     //@return SocketAvailabilityInfo with socket info and tips
    @GetMapping("/socket-info/{socket}")
    public ResponseEntity<SocketAvailabilityInfo> getSocketInfo(
            @PathVariable String socket) {
        return ResponseEntity.ok(new SocketAvailabilityInfo(socket, 
                "Socket " + socket + " is a common platform with many component options available"));
    }


    public static class SocketCheckRequest {
        @JsonProperty("cpu_socket")
        public String cpuSocket;
        
        @JsonProperty("motherboard_socket")
        public String motherboardSocket;
    }

    public static class MemoryCheckRequest {
        @JsonProperty("motherboard_memory_type")
        public String motherboardMemoryType;
        
        @JsonProperty("ram_memory_type")
        public String ramMemoryType;
    }

    public static class PsuCheckRequest {
        @JsonProperty("psu_wattage")
        public int psuWattage;
        
        @JsonProperty("cpu_name")
        public String cpuName;
        
        @JsonProperty("gpu_name")
        public String gpuName;
        
        @JsonProperty("prefer_headroom")
        public boolean preferHeadroom = true;
    }

    public static class CpuGpuPairingRequest {
        @JsonProperty("cpu_name")
        public String cpuName;
        
        @JsonProperty("gpu_name")
        public String gpuName;
        
        @JsonProperty("use_case")
        public String useCase = "gaming";
        
        @JsonProperty("resolution")
        public String resolution = "1440p";
    }

    public static class BuildValidationRequest {
        public ComponentInfo cpu;
        public ComponentInfo gpu;
        public ComponentInfo motherboard;
        public ComponentInfo memory;
        @JsonProperty("psu")
        public ComponentInfo psu;
    }

    public static class ComponentInfo {
        public String id;
        public String name;
        public String socket;
        @JsonProperty("memory_type")
        public String memoryType;
        public Integer wattage;
        @JsonProperty("price_kzt")
        public java.math.BigDecimal priceKzt;
    }

    public static class CompatibilityResponse {
        public boolean compatible;
        public String reason;

        public CompatibilityResponse(boolean compatible, String reason) {
            this.compatible = compatible;
            this.reason = reason;
        }
    }

    public static class BuildCompatibilityReport {
        public boolean compatible;
        public List<String> issues;
        public List<String> warnings;

        public BuildCompatibilityReport(boolean compatible, List<String> issues, List<String> warnings) {
            this.compatible = compatible;
            this.issues = issues;
            this.warnings = warnings;
        }
    }

    public static class SocketAvailabilityInfo {
        public String socket;
        public String info;

        public SocketAvailabilityInfo(String socket, String info) {
            this.socket = socket;
            this.info = info;
        }
    }

    private BuildResponse.PartDto convertToPartDto(ComponentInfo info) {
        if (info == null) return null;
        return new BuildResponse.PartDto(
                parseLongOrNull(info.id),
                info.name,
                info.priceKzt != null ? info.priceKzt : java.math.BigDecimal.ZERO,
                info.socket,
                info.memoryType,
                info.wattage,
                null,
            null,
            null,
            null
        );
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
