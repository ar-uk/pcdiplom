package org.example.partservice.repository;

import org.example.partservice.model.ReferenceMotherboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface    ReferenceMotherboardRepository extends JpaRepository<ReferenceMotherboard, Long> {
    Optional<ReferenceMotherboard> findByOpendbId(String opendbId);
    Optional<ReferenceMotherboard> findByName(String name);
}
