package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "video_card")
public class VideoCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    private String chipset;

    @Column(name = "memory_gb")
    private Integer memoryGb;

    @Column(name = "core_clock")
    private Integer coreClock;

    @Column(name = "boost_clock")
    private Integer boostClock;

    private String color;

    @Column(name = "length_mm")
    private Integer lengthMm;
}