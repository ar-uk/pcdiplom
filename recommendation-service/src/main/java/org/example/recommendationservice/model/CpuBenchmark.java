package org.example.recommendationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cpu_benchmark")
public class CpuBenchmark {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "cpu_name", nullable = false, unique = true)
    private String cpuName;

    @Column(name = "score_1080p")
    private Double score1080p;

    @Column(name = "score_1440p")
    private Double score1440p;

    @Column(name = "tdp_watts")
    private Integer tdpWatts;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "source_version", nullable = false)
    private String sourceVersion;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;
}