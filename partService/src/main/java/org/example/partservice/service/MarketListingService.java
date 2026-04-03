package org.example.partservice.service;

import org.example.partservice.model.MarketListing;
import org.example.partservice.repository.MarketListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MarketListingService {

    @Autowired
    private MarketListingRepository marketListingRepository;

    /**
     * Get all active listings for a part, sorted by price
     */
    public List<MarketListing> getListingsForPart(String partType, Long partId) {
        return marketListingRepository.findByPart(partType, partId);
    }

    /**
     * Get the cheapest listing for a part
     */
    public Optional<MarketListing> getCheapestListing(String partType, Long partId) {
        return marketListingRepository.findCheapestListing(partType, partId);
    }

    /**
     * Get listing for specific retailer
     */
    public Optional<MarketListing> getListingByRetailer(String partType, Long partId, String retailer) {
        return marketListingRepository.findByPartTypeAndPartIdAndRetailer(partType, partId, retailer);
    }

    /**
     * Add or update a market listing
     */
    public MarketListing saveOrUpdate(MarketListing listing) {
        Optional<MarketListing> existing = marketListingRepository
                .findByPartTypeAndPartIdAndRetailer(listing.getPartType(), listing.getPartId(), listing.getRetailer());

        if (existing.isPresent()) {
            MarketListing updated = existing.get();
            updated.setPrice(listing.getPrice());
            updated.setUrl(listing.getUrl());
            updated.setCurrency(listing.getCurrency());
            updated.setActive(listing.getActive());
            return marketListingRepository.save(updated);
        }

        return marketListingRepository.save(listing);
    }

    /**
     * Find listings by part name (for search functionality)
     */
    public List<MarketListing> searchByPartName(String partType, String partName) {
        return marketListingRepository.findByPartNameContaining(partType, partName);
    }

    /**
     * Deactivate a listing
     */
    public void deactivateListing(Long listingId) {
        Optional<MarketListing> listing = marketListingRepository.findById(listingId);
        if (listing.isPresent()) {
            MarketListing m = listing.get();
            m.setActive(false);
            marketListingRepository.save(m);
        }
    }
}
