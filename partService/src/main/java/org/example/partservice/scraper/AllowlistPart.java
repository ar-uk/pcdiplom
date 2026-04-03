package org.example.partservice.scraper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllowlistPart {
    private String partName;
    private String partType; // "gpu" or "cpu"
    private String brand;
    private String chipsetOrSeries;
}
