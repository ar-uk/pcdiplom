package org.example.partservice.repository;

import org.example.partservice.model.ReferenceGpuMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferenceGpuMatchRepository extends JpaRepository<ReferenceGpuMatch, Long> {
    Optional<ReferenceGpuMatch> findByOpendbId(String opendbId);
    Optional<ReferenceGpuMatch> findByOpendbIdAndParsedPartId(String opendbId, Long parsedPartId);
}
