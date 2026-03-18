package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cpu")
public class Cpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "core_count")
    private Integer coreCount;

    @Column(name = "core_clock")
    private BigDecimal coreClock;

    @Column(name = "boost_clock")
    private BigDecimal boostClock;

    @Column(name = "microarchitecture")
    private String microarchitecture;

    private Integer tdp;

    private String graphics;
}
