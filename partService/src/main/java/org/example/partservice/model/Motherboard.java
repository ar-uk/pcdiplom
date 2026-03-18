package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "motherboard")
public class Motherboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    private String socket;

    @Column(name = "form_factor")
    private String formFactor;

    @Column(name = "max_memory")
    private Integer maxMemory;

    @Column(name = "memory_slots")
    private Integer memorySlots;

    private String color;
}