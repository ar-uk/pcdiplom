package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "parsed_power_supply")
public class ParsedPowerSupply extends AbstractParsedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
