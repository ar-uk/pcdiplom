package org.example.partservice.repository;

import org.example.partservice.model.ReferenceGpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReferenceGpuRepository extends JpaRepository<ReferenceGpu, Long> {
    Optional<ReferenceGpu> findByOpendbId(String opendbId);
    Optional<ReferenceGpu> findByName(String name);
}
