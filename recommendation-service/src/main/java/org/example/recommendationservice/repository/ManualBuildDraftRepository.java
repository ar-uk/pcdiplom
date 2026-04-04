package org.example.recommendationservice.repository;

import java.util.Optional;
import org.example.recommendationservice.model.ManualBuildDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManualBuildDraftRepository extends JpaRepository<ManualBuildDraft, Long> {
    Optional<ManualBuildDraft> findByIdAndUserId(Long id, String userId);
}
