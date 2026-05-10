package org.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendationservice.model.PartMetadata;
import org.example.recommendationservice.repository.PartMetadataRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartMetadataService {

    private final PartMetadataRepository repository;

    public Optional<PartMetadata> findByTypeAndName(String partType, String partName) {
        if (partType == null || partType.isBlank() || partName == null || partName.isBlank()) {
            return Optional.empty();
        }
        Optional<PartMetadata> exact = repository.findFirstByPartTypeIgnoreCaseAndPartNameIgnoreCase(partType, partName);
        if (exact.isPresent()) {
            return exact;
        }
        return repository.findFirstByPartTypeIgnoreCaseAndPartNameContainingIgnoreCase(partType, partName);
    }

    public Optional<Integer> findTdpWatts(String partType, String partName) {
        return findByTypeAndName(partType, partName).map(PartMetadata::getTdpWatts);
    }
}