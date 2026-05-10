package org.example.partservice.repository;

import org.example.partservice.model.ParsedInternalHardDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ParsedInternalHardDriveRepository extends JpaRepository<ParsedInternalHardDrive, Long> {
    Optional<ParsedInternalHardDrive> findFirstByUrl(String url);
    long deleteByLastScrapedBefore(LocalDateTime cutoff);
}
