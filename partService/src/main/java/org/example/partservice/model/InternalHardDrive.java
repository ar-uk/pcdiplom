package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "internal_hard_drive")
public class InternalHardDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "capacity_gb")
    private Integer capacityGb;

    @Column(name = "price_per_gb")
    private BigDecimal pricePerGb;

    @Column(name = "drive_type")
    private String driveType;

    @Column(name = "cache_mb")
    private Integer cacheMb;

    @Column(name = "form_factor")
    private String formFactor;

    @Column(name = "interface")
    private String driveInterface;
}