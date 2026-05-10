package org.example.partservice.scraper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defines a search strategy for scraping parts from shop.kz.
 * Example: search for "Ryzen 5" CPUs within budget 100K-200K KZT
 */
public class SearchStrategy {
    private String partType;           // "cpu", "gpu", "memory", etc.
    private List<String> queries;      // search terms: ["Ryzen 5", "Ryzen 7"]
    private BigDecimal minPrice;       // optional budget floor
    private BigDecimal maxPrice;       // optional budget ceiling
    private String retailer;           // default: "shop.kz"

    // Constructors
    public SearchStrategy() {
    }

    public SearchStrategy(String partType, List<String> queries) {
        this.partType = partType;
        this.queries = queries;
        this.retailer = "shop.kz";
    }

    public SearchStrategy(String partType, List<String> queries, BigDecimal minPrice, BigDecimal maxPrice) {
        this.partType = partType;
        this.queries = queries;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.retailer = "shop.kz";
    }

    // Getters and Setters
    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    @Override
    public String toString() {
        return "SearchStrategy{" +
                "partType='" + partType + '\'' +
                ", queries=" + queries +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", retailer='" + retailer + '\'' +
                '}';
    }
}
