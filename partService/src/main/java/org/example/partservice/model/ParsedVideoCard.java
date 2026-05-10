package org.example.partservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "parsed_video_card")
public class ParsedVideoCard extends AbstractParsedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
