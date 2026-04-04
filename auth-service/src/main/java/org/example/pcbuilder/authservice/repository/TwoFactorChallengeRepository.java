package org.example.pcbuilder.authservice.repository;

import java.util.Optional;
import org.example.pcbuilder.authservice.model.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, Long> {
    Optional<TwoFactorChallenge> findByChallengeId(String challengeId);
}