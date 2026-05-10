package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.PartMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartMetadataRepository extends JpaRepository<PartMetadata, UUID> {

    Optional<PartMetadata> findFirstByPartTypeIgnoreCaseAndPartNameIgnoreCase(String partType, String partName);

    Optional<PartMetadata> findFirstByPartTypeIgnoreCaseAndPartNameContainingIgnoreCase(String partType, String partName);
}