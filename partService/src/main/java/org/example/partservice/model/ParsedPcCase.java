package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "parsed_pc_case")
public class ParsedPcCase extends AbstractParsedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
