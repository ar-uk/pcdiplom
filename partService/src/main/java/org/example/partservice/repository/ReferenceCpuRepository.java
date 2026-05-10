package org.example.partservice.repository;

import org.example.partservice.model.ReferenceCpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReferenceCpuRepository extends JpaRepository<ReferenceCpu, Long> {
    Optional<ReferenceCpu> findByOpendbId(String opendbId);
    Optional<ReferenceCpu> findByName(String name);
}
