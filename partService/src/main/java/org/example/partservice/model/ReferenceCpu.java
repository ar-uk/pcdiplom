package org.example.partservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reference_cpu")
public class ReferenceCpu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String opendbId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String series;

    @Column
    private String variant;

    @Column
    private Integer cores;

    @Column
    private Integer threads;

    @Column
    private BigDecimal baseClockGhz;

    @Column
    private BigDecimal boostClockGhz;

    @Column
    private String socket;

    @Column
    private String microarchitecture;

    @Column
    private Integer tdp;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    public ReferenceCpu() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOpendbId() { return opendbId; }
    public void setOpendbId(String opendbId) { this.opendbId = opendbId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public Integer getCores() { return cores; }
    public void setCores(Integer cores) { this.cores = cores; }

    public Integer getThreads() { return threads; }
    public void setThreads(Integer threads) { this.threads = threads; }

    public BigDecimal getBaseClockGhz() { return baseClockGhz; }
    public void setBaseClockGhz(BigDecimal baseClockGhz) { this.baseClockGhz = baseClockGhz; }

    public BigDecimal getBoostClockGhz() { return boostClockGhz; }
    public void setBoostClockGhz(BigDecimal boostClockGhz) { this.boostClockGhz = boostClockGhz; }

    public String getSocket() { return socket; }
    public void setSocket(String socket) { this.socket = socket; }

    public String getMicroarchitecture() { return microarchitecture; }
    public void setMicroarchitecture(String microarchitecture) { this.microarchitecture = microarchitecture; }

    public Integer getTdp() { return tdp; }
    public void setTdp(Integer tdp) { this.tdp = tdp; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
