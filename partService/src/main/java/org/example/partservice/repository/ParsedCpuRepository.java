package org.example.partservice.repository;

import org.example.partservice.model.ParsedCpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedCpuRepository extends JpaRepository<ParsedCpu, Long> {
    Optional<ParsedCpu> findFirstByUrl(String url);
}
