package org.example.recommendationservice.service;

import org.example.recommendationservice.dto.BuildResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComponentCompatibilityServiceTest {

    @Test
    void compatibleSocketMatches() {
        var resolver = Mockito.mock(HardwareFallbackResolver.class);
        var cpuBench = Mockito.mock(CpuBenchmarkService.class);
        var gpuBench = Mockito.mock(GpuBenchmarkService.class);
        var svc = new ComponentCompatibilityService(resolver, cpuBench, gpuBench);

        assertTrue(svc.compatibleSocket("AM4", "AM4"));
        assertFalse(svc.compatibleSocket("AM5", "AM4"));
    }

    @Test
    void memoryCompatibility() {
        var resolver = Mockito.mock(HardwareFallbackResolver.class);
        var svc = new ComponentCompatibilityService(resolver, Mockito.mock(CpuBenchmarkService.class), Mockito.mock(GpuBenchmarkService.class));

        assertTrue(svc.compatibleMemory("DDR4", "DDR4"));
        assertFalse(svc.compatibleMemory("DDR5", "DDR4"));
    }

    @Test
    void validateBuildDetectsSocketMismatch() {
        var resolver = Mockito.mock(HardwareFallbackResolver.class);
        var svc = new ComponentCompatibilityService(resolver, Mockito.mock(CpuBenchmarkService.class), Mockito.mock(GpuBenchmarkService.class));

        BuildResponse.PartDto cpu = new BuildResponse.PartDto(
                1L,
                "AMD Ryzen 5 7600X",
                null,
                "AM5",
                "DDR5",
                105,
                null,
                null,
                null,
                "in_stock"
        );

        BuildResponse.PartDto mb = new BuildResponse.PartDto(
                2L,
                "B450 Motherboard",
                null,
                "AM4",
                "DDR4",
                null,
                null,
                null,
                null,
                "in_stock"
        );

        var parts = Map.<String, BuildResponse.PartDto>of("cpu", cpu, "motherboard", mb);
        var result = svc.validateBuild(parts);
        assertFalse(result.compatible);
        assertTrue(result.issues.size() >= 1);
    }
}
