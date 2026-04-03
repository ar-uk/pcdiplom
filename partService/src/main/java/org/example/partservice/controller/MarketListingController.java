package org.example.partservice.controller;

import org.example.partservice.model.MarketListing;
import org.example.partservice.service.MarketListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/market-listings")
public class MarketListingController {

    @Autowired
    private MarketListingService marketListingService;

    /**
     * Get all listings for a specific part
     * Example: GET /api/market-listings?partType=gpu&partId=42
     */
    @GetMapping
    public ResponseEntity<List<MarketListing>> getListingsForPart(
            @RequestParam String partType,
            @RequestParam Long partId) {
        List<MarketListing> listings = marketListingService.getListingsForPart(partType, partId);
        return ResponseEntity.ok(listings);
    }

    /**
     * Get the cheapest listing for a part
     * Example: GET /api/market-listings/cheapest?partType=gpu&partId=42
     */
    @GetMapping("/cheapest")
    public ResponseEntity<MarketListing> getCheapestListing(
            @RequestParam String partType,
            @RequestParam Long partId) {
        return marketListingService.getCheapestListing(partType, partId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get listing for a specific retailer
     * Example: GET /api/market-listings/by-retailer?partType=gpu&partId=42&retailer=Kaspi
     */
    @GetMapping("/by-retailer")
    public ResponseEntity<MarketListing> getListingByRetailer(
            @RequestParam String partType,
            @RequestParam Long partId,
            @RequestParam String retailer) {
        return marketListingService.getListingByRetailer(partType, partId, retailer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search listings by part name
     * Example: GET /api/market-listings/search?partType=gpu&partName=RTX
     */
    @GetMapping("/search")
    public ResponseEntity<List<MarketListing>> searchByPartName(
            @RequestParam String partType,
            @RequestParam String partName) {
        List<MarketListing> listings = marketListingService.searchByPartName(partType, partName);
        return ResponseEntity.ok(listings);
    }

    /**
     * Add or update a market listing
     * POST /api/market-listings
     */
    @PostMapping
    public ResponseEntity<MarketListing> createOrUpdateListing(@RequestBody MarketListing listing) {
        MarketListing saved = marketListingService.saveOrUpdate(listing);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deactivate a listing
     * DELETE /api/market-listings/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateListing(@PathVariable Long id) {
        marketListingService.deactivateListing(id);
        return ResponseEntity.noContent().build();
    }
}
