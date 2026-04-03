package org.example.recommendationservice.repository;

import org.example.recommendationservice.model.UserBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBuildRepository extends JpaRepository<UserBuild, Long> {
    List<UserBuild> findAllByUserIdOrderByUpdatedAtDesc(String userId);
}
