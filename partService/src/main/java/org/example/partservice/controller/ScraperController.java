package org.example.partservice.controller;

import org.example.partservice.service.ParsedDataCleanupService;
import org.example.partservice.scraper.KaspiScraperService;
import org.example.partservice.scraper.ShopScraperTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {
    
    @Autowired
    private ShopScraperTask shopScraperTask;

    @Autowired
    private KaspiScraperService kaspiScraperService;

    @Autowired
    private ParsedDataCleanupService parsedDataCleanupService;
    

    @PostMapping("/shop/scrape")
    public ResponseEntity<String> scrapeShop(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String partType) {
        shopScraperTask.scrapeAllowlistFromShop(limit, partType);
        return ResponseEntity.ok("shop.kz scraping started. Check logs for progress.");
    }


    @PostMapping("/shop/scrape-strategies")
    public ResponseEntity<String> scrapeUsingStrategies() {
        shopScraperTask.scrapeUsingSearchStrategies();
        return ResponseEntity.ok("shop.kz scraping using search strategies started. Check logs for progress.");
    }


    @PostMapping("/kaspi/scrape")
    public ResponseEntity<String> scrapeKaspi(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String partType) {
        kaspiScraperService.scrapeAllowlistFromKaspi(limit, partType);
        return ResponseEntity.ok("Kaspi.kz scraping started. Check logs for progress.");
    }


    @PostMapping("/kaspi/scrape-strategies")
    public ResponseEntity<String> scrapeKaspiUsingStrategies() {
        kaspiScraperService.scrapeUsingSearchStrategies();
        return ResponseEntity.ok("Kaspi.kz scraping using search strategies started. Check logs for progress.");
    }

    @PostMapping("/cleanup-old")
    public ResponseEntity<String> cleanupOldData(@RequestParam(required = false, defaultValue = "7") Integer days) {
        ParsedDataCleanupService.CleanupSummary summary = parsedDataCleanupService.cleanupOlderThanDays(days == null ? 7 : days);
        return ResponseEntity.ok("Cleanup completed: deleted=" + summary.totalDeleted() + ", retentionDays=" + summary.retentionDays() + ", cutoff=" + summary.cutoff());
    }

}
