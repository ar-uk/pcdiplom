package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.AiSavedBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSavedBuildRepository extends JpaRepository<AiSavedBuild, Long> {
    Optional<AiSavedBuild> findFirstBySessionId(String sessionId);
}
