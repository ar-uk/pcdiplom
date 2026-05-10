package org.example.partservice.repository;

import org.example.partservice.model.ReferenceCpuMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferenceCpuMatchRepository extends JpaRepository<ReferenceCpuMatch, Long> {
    Optional<ReferenceCpuMatch> findByOpendbId(String opendbId);
    Optional<ReferenceCpuMatch> findByOpendbIdAndParsedPartId(String opendbId, Long parsedPartId);
}
