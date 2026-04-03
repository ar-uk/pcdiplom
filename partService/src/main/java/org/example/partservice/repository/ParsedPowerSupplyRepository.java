package org.example.partservice.repository;

import org.example.partservice.model.ParsedPowerSupply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedPowerSupplyRepository extends JpaRepository<ParsedPowerSupply, Long> {
    Optional<ParsedPowerSupply> findFirstByUrl(String url);
}
