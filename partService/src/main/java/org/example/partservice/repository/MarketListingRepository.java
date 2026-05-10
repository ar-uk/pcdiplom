package org.example.partservice.repository;

import org.example.partservice.model.MarketListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    long deleteByLastScrapedBefore(LocalDateTime cutoff);

    @Query("SELECT m FROM MarketListing m WHERE m.partType = :partType AND m.partId = :partId AND m.active = true ORDER BY m.price ASC")
    List<MarketListing> findByPart(@Param("partType") String partType, @Param("partId") Long partId);

    Optional<MarketListing> findByPartTypeAndPartIdAndRetailer(String partType, Long partId, String retailer);

    List<MarketListing> findByRetailerAndActive(String retailer, Boolean active);

    @Query("SELECT m FROM MarketListing m WHERE m.partType = :partType AND m.partId = :partId AND m.active = true ORDER BY m.price ASC LIMIT 1")
    Optional<MarketListing> findCheapestListing(@Param("partType") String partType, @Param("partId") Long partId);

    @Query("SELECT m FROM MarketListing m WHERE m.partType = :partType AND m.partName LIKE %:partName% AND m.active = true ORDER BY m.price ASC")
    List<MarketListing> findByPartNameContaining(@Param("partType") String partType, @Param("partName") String partName);

    default MarketListing saveOrUpdate(MarketListing listing) {
        Optional<MarketListing> existing = findByPartTypeAndPartIdAndRetailer(
            listing.getPartType(), listing.getPartId(), listing.getRetailer());
        
        if (existing.isPresent()) {
            MarketListing updated = existing.get();
            updated.setPrice(listing.getPrice());
            updated.setUrl(listing.getUrl());
            updated.setCurrency(listing.getCurrency());
            updated.setActive(listing.getActive());
            return save(updated);
        }
        
        return save(listing);
    }
}
