package org.example.recommendationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "part_metadata")
public class PartMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "part_type", nullable = false)
    private String partType;

    @Column(name = "part_name", nullable = false)
    private String partName;

    @Column(name = "tdp_watts")
    private Integer tdpWatts;

    @Column(name = "socket")
    private String socket;

    @Column(name = "memory_type")
    private String memoryType;
}