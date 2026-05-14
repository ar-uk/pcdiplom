package org.example.partservice.service;

import org.example.partservice.repository.ReferenceCpuRepository;
import org.example.partservice.repository.ReferenceGpuRepository;
import org.example.partservice.repository.ReferenceMotherboardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductMatcherServiceTest {

    @Test
    void matchProductReturnsEmptyWhenReposEmpty() throws Exception {
        ProductMatcherService svc = new ProductMatcherService();

        ReferenceCpuRepository cpuRepo = Mockito.mock(ReferenceCpuRepository.class);
        ReferenceGpuRepository gpuRepo = Mockito.mock(ReferenceGpuRepository.class);
        ReferenceMotherboardRepository mbRepo = Mockito.mock(ReferenceMotherboardRepository.class);

        Mockito.when(cpuRepo.findAll()).thenReturn(Collections.emptyList());
        Mockito.when(gpuRepo.findAll()).thenReturn(Collections.emptyList());
        Mockito.when(mbRepo.findAll()).thenReturn(Collections.emptyList());

        // inject mocks into private fields
        Field f1 = ProductMatcherService.class.getDeclaredField("cpuRepository");
        f1.setAccessible(true);
        f1.set(svc, cpuRepo);

        Field f2 = ProductMatcherService.class.getDeclaredField("gpuRepository");
        f2.setAccessible(true);
        f2.set(svc, gpuRepo);

        Field f3 = ProductMatcherService.class.getDeclaredField("motherboardRepository");
        f3.setAccessible(true);
        f3.set(svc, mbRepo);

        var opt = svc.matchProduct("ASUS RTX 3060 12GB TUF");
        assertTrue(opt.isEmpty());
    }
}
