package org.example.partservice.repository;

import org.example.partservice.model.ParsedCpuCooler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedCpuCoolerRepository extends JpaRepository<ParsedCpuCooler, Long> {
    Optional<ParsedCpuCooler> findFirstByUrl(String url);
}
