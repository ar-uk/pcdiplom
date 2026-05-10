package org.example.partservice.repository;

import org.example.partservice.model.ReferenceMotherboardMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferenceMotherboardMatchRepository extends JpaRepository<ReferenceMotherboardMatch, Long> {
    Optional<ReferenceMotherboardMatch> findByOpendbId(String opendbId);
    Optional<ReferenceMotherboardMatch> findByOpendbIdAndParsedPartId(String opendbId, Long parsedPartId);
}
