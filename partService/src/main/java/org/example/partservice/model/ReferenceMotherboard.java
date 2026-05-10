package org.example.partservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reference_motherboard")
public class ReferenceMotherboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String opendbId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String manufacturer;

    @Column
    private String series;

    @Column
    private String variant;

    @Column
    private String socket;

    @Column
    private String formFactor;

    @Column
    private String chipset;

    @Column
    private Integer memoryMaxGb;

    @Column
    private String memoryRamType;

    @Column
    private Integer memorySlots;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

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
}
