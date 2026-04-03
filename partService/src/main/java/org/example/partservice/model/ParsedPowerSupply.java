package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "parsed_power_supply")
public class ParsedPowerSupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_kzt")
    private BigDecimal priceKzt;

    @Column(nullable = false)
    private String retailer;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String url;

    @Column(name = "last_scraped", nullable = false)
    private LocalDateTime lastScraped;
}
