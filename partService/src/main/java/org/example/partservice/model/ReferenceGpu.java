package org.example.partservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reference_gpu")
public class ReferenceGpu {
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
    private String chipset;

    @Column
    private Integer memoryGb;

    @Column
    private String memoryType;

    @Column
    private BigDecimal baseClockMhz;

    @Column
    private BigDecimal boostClockMhz;

    @Column
    private Integer coreCount;

    @Column
    private String interface_;

    @Column
    private Integer tdp;

    @Column
    private Integer releaseYear;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    public ReferenceGpu() {}

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

    public String getChipset() { return chipset; }
    public void setChipset(String chipset) { this.chipset = chipset; }

    public Integer getMemoryGb() { return memoryGb; }
    public void setMemoryGb(Integer memoryGb) { this.memoryGb = memoryGb; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public BigDecimal getBaseClockMhz() { return baseClockMhz; }
    public void setBaseClockMhz(BigDecimal baseClockMhz) { this.baseClockMhz = baseClockMhz; }

    public BigDecimal getBoostClockMhz() { return boostClockMhz; }
    public void setBoostClockMhz(BigDecimal boostClockMhz) { this.boostClockMhz = boostClockMhz; }

    public Integer getCoreCount() { return coreCount; }
    public void setCoreCount(Integer coreCount) { this.coreCount = coreCount; }

    public String getInterface_() { return interface_; }
    public void setInterface_(String interface_) { this.interface_ = interface_; }

    public Integer getTdp() { return tdp; }
    public void setTdp(Integer tdp) { this.tdp = tdp; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
