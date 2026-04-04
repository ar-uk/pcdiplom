package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.PartPerformanceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartPerformanceMappingRepository extends JpaRepository<PartPerformanceMapping, Long> {
    Optional<PartPerformanceMapping> findFirstByPartTypeAndPartIdAndScoreVersion(String partType, Long partId, String scoreVersion);
}
