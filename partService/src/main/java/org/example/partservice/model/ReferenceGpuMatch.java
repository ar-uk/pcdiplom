package org.example.partservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reference_gpu_match")
public class ReferenceGpuMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opendb_id")
    private String opendbId;

    private String name;
    private String manufacturer;
    private String chipset;

    @Column(name = "memory_gb")
    private Integer memoryGb;

    @Column(name = "memory_type")
    private String memoryType;

    @Column(name = "base_clock_mhz")
    private Integer baseClockMhz;

    @Column(name = "boost_clock_mhz")
    private Integer boostClockMhz;

    @Column(name = "core_count")
    private Integer coreCount;

    @Column(name = "interface_")
    private String interfaceName;

    private Integer tdp;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "parsed_part_id")
    private Long parsedPartId;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "price_kzt")
    private java.math.BigDecimal priceKzt;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOpendbId() { return opendbId; }
    public void setOpendbId(String opendbId) { this.opendbId = opendbId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getChipset() { return chipset; }
    public void setChipset(String chipset) { this.chipset = chipset; }

    public Integer getMemoryGb() { return memoryGb; }
    public void setMemoryGb(Integer memoryGb) { this.memoryGb = memoryGb; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public Integer getBaseClockMhz() { return baseClockMhz; }
    public void setBaseClockMhz(Integer baseClockMhz) { this.baseClockMhz = baseClockMhz; }

    public Integer getBoostClockMhz() { return boostClockMhz; }
    public void setBoostClockMhz(Integer boostClockMhz) { this.boostClockMhz = boostClockMhz; }

    public Integer getCoreCount() { return coreCount; }
    public void setCoreCount(Integer coreCount) { this.coreCount = coreCount; }

    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

    public Integer getTdp() { return tdp; }
    public void setTdp(Integer tdp) { this.tdp = tdp; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public Long getParsedPartId() { return parsedPartId; }
    public void setParsedPartId(Long parsedPartId) { this.parsedPartId = parsedPartId; }

    public String getSourceLink() { return sourceLink; }
    public void setSourceLink(String sourceLink) { this.sourceLink = sourceLink; }

    public java.math.BigDecimal getPriceKzt() { return priceKzt; }
    public void setPriceKzt(java.math.BigDecimal priceKzt) { this.priceKzt = priceKzt; }
}
