package org.example.partservice.repository;

import org.example.partservice.model.ParsedPcCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedPcCaseRepository extends JpaRepository<ParsedPcCase, Long> {
    Optional<ParsedPcCase> findFirstByUrl(String url);
}
