package org.example.partservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.example.partservice.scraper.KaspiScraperService;
import org.example.partservice.scraper.ShopScraperTask;
import org.example.partservice.service.ParsedDataCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ScraperController.class)
class ScraperControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShopScraperTask shopScraperTask;

    @MockBean
    private KaspiScraperService kaspiScraperService;

    @MockBean
    private ParsedDataCleanupService parsedDataCleanupService;

    @Test
    void shopScrapeReturnsOk() throws Exception {
        mockMvc.perform(post("/api/scraper/shop/scrape").param("limit", "5").param("partType", "gpu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("shop.kz")));

        verify(shopScraperTask).scrapeAllowlistFromShop(5, "gpu");
    }

    @Test
    void kaspiScrapeReturnsOk() throws Exception {
        mockMvc.perform(post("/api/scraper/kaspi/scrape"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kaspi")));

        verify(kaspiScraperService).scrapeAllowlistFromKaspi(null, null);
    }

    @Test
    void cleanupOldCallsService() throws Exception {
        var summary =
                new ParsedDataCleanupService.CleanupSummary(
                        14,
                        LocalDateTime.now(),
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        3L);
        when(parsedDataCleanupService.cleanupOlderThanDays(14)).thenReturn(summary);

        mockMvc.perform(post("/api/scraper/cleanup-old").param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cleanup completed")));

        verify(parsedDataCleanupService).cleanupOlderThanDays(14);
    }
}
