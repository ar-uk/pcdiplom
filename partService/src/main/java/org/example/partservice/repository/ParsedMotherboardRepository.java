package org.example.partservice.repository;

import org.example.partservice.model.ParsedMotherboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ParsedMotherboardRepository extends JpaRepository<ParsedMotherboard, Long> {
    Optional<ParsedMotherboard> findFirstByUrl(String url);
    long deleteByLastScrapedBefore(LocalDateTime cutoff);
}
