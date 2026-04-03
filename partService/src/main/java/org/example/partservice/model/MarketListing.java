package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "market_listing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"part_type", "part_id", "retailer"})
})
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_type", nullable = false)
    private String partType;  // "cpu", "gpu", "motherboard", etc.

    @Column(name = "part_id", nullable = false)
    private Long partId;

    @Column(name = "part_name", nullable = false)
    private String partName;

    @Column(nullable = false)
    private String retailer;  // "Kaspi", "Technodom", "shop.kz", etc.

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;  // "KZT", "USD"

    @Column(length = 1000)
    private String url;

    @Column(name = "last_scraped")
    private LocalDateTime lastScraped;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastScraped = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
