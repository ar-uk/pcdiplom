package org.example.partservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AbstractParsedListing {

    @Column(nullable = false)
    private String name;

    @Column(name = "price_kzt")
    private BigDecimal priceKzt;

    @Column(nullable = false)
    private String retailer;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String url;

    @Column(name = "last_scraped", nullable = false)
    private LocalDateTime lastScraped;

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(name = "normalized_specs_json", columnDefinition = "text")
    private String normalizedSpecsJson;

    @Column(name = "source_payload_json", columnDefinition = "text")
    private String sourcePayloadJson;

    @Column(name = "confidence_score", precision = 4, scale = 3)
    private BigDecimal confidenceScore;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPriceKzt() {
        return priceKzt;
    }

    public void setPriceKzt(BigDecimal priceKzt) {
        this.priceKzt = priceKzt;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getLastScraped() {
        return lastScraped;
    }

    public void setLastScraped(LocalDateTime lastScraped) {
        this.lastScraped = lastScraped;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getNormalizedSpecsJson() {
        return normalizedSpecsJson;
    }

    public void setNormalizedSpecsJson(String normalizedSpecsJson) {
        this.normalizedSpecsJson = normalizedSpecsJson;
    }

    public String getSourcePayloadJson() {
        return sourcePayloadJson;
    }

    public void setSourcePayloadJson(String sourcePayloadJson) {
        this.sourcePayloadJson = sourcePayloadJson;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}