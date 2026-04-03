package org.example.partservice.controller;

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
    
    /**
     * Manually trigger shop.kz scraping
     * POST /api/scraper/shop/scrape?limit=10&partType=gpu
     */
    @PostMapping("/shop/scrape")
    public ResponseEntity<String> scrapeShop(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String partType) {
        shopScraperTask.scrapeAllowlistFromShop(limit, partType);
        return ResponseEntity.ok("shop.kz scraping started. Check logs for progress.");
    }
}
