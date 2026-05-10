package org.example.partservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reference_motherboard_match")
public class ReferenceMotherboardMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opendb_id")
    private String opendbId;

    private String name;
    private String manufacturer;
    private String series;
    private String variant;
    private String socket;

    @Column(name = "form_factor")
    private String formFactor;

    private String chipset;

    @Column(name = "memory_max_gb")
    private Integer memoryMaxGb;

    @Column(name = "memory_ram_type")
    private String memoryRamType;

    @Column(name = "memory_slots")
    private Integer memorySlots;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "parsed_part_id")
    private Long parsedPartId;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "price_kzt")
    private java.math.BigDecimal priceKzt;

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

    public String getSocket() { return socket; }
    public void setSocket(String socket) { this.socket = socket; }

    public String getFormFactor() { return formFactor; }
    public void setFormFactor(String formFactor) { this.formFactor = formFactor; }

    public String getChipset() { return chipset; }
    public void setChipset(String chipset) { this.chipset = chipset; }

    public Integer getMemoryMaxGb() { return memoryMaxGb; }
    public void setMemoryMaxGb(Integer memoryMaxGb) { this.memoryMaxGb = memoryMaxGb; }

    public String getMemoryRamType() { return memoryRamType; }
    public void setMemoryRamType(String memoryRamType) { this.memoryRamType = memoryRamType; }

    public Integer getMemorySlots() { return memorySlots; }
    public void setMemorySlots(Integer memorySlots) { this.memorySlots = memorySlots; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public Long getParsedPartId() { return parsedPartId; }
    public void setParsedPartId(Long parsedPartId) { this.parsedPartId = parsedPartId; }

    public String getSourceLink() { return sourceLink; }
    public void setSourceLink(String sourceLink) { this.sourceLink = sourceLink; }

    public java.math.BigDecimal getPriceKzt() { return priceKzt; }
    public void setPriceKzt(java.math.BigDecimal priceKzt) { this.priceKzt = priceKzt; }
}
