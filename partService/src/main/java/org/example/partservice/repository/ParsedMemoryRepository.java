package org.example.partservice.repository;

import org.example.partservice.model.ParsedMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedMemoryRepository extends JpaRepository<ParsedMemory, Long> {
    Optional<ParsedMemory> findFirstByUrl(String url);
}
