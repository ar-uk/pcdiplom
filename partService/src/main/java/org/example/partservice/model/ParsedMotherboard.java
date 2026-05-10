package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "parsed_motherboard")
public class ParsedMotherboard extends AbstractParsedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
