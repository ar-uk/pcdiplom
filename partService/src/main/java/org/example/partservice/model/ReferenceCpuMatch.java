package org.example.partservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reference_cpu_match")
public class ReferenceCpuMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opendb_id")
    private String opendbId;

    private String name;
    private String manufacturer;
    private String series;
    private String variant;

    private Integer cores;
    private Integer threads;

    @Column(name = "base_clock_ghz")
    private BigDecimal baseClockGhz;

    @Column(name = "boost_clock_ghz")
    private BigDecimal boostClockGhz;

    private String socket;

    private String microarchitecture;

    private Integer tdp;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "parsed_part_id")
    private Long parsedPartId;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "price_kzt")
    private BigDecimal priceKzt;

    // Getters and setters

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

    public Long getParsedPartId() { return parsedPartId; }
    public void setParsedPartId(Long parsedPartId) { this.parsedPartId = parsedPartId; }

    public String getSourceLink() { return sourceLink; }
    public void setSourceLink(String sourceLink) { this.sourceLink = sourceLink; }

    public BigDecimal getPriceKzt() { return priceKzt; }
    public void setPriceKzt(BigDecimal priceKzt) { this.priceKzt = priceKzt; }
}
