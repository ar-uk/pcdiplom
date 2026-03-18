package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "pc_case")
public class PcCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "case_type")
    private String caseType;

    private String color;

    private String psu;

    @Column(name = "side_panel")
    private String sidePanel;

    @Column(name = "external_volume")
    private BigDecimal externalVolume;

    @Column(name = "internal_35_bays")
    private Integer internal35Bays;
}