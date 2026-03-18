package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "memory")
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    private Integer ddr;

    @Column(name = "speed_mhz")
    private Integer speedMhz;

    private Integer sticks;

    @Column(name = "gb_per_stick")
    private Integer gbPerStick;

    @Column(name = "price_per_gb")
    private BigDecimal pricePerGb;

    private String color;

    @Column(name = "first_word_latency")
    private BigDecimal firstWordLatency;

    @Column(name = "cas_latency")
    private Integer casLatency;
}