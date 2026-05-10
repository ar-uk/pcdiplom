package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "parsed_internal_hard_drive")
public class ParsedInternalHardDrive extends AbstractParsedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
