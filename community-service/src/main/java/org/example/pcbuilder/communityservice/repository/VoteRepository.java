package org.example.pcbuilder.communityservice.repository;

import java.util.Optional;
import org.example.pcbuilder.communityservice.model.Vote;
import org.example.pcbuilder.communityservice.model.VoteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByUserIdAndTargetTypeAndTargetId(String userId, VoteTargetType targetType, Long targetId);

    long deleteByUserIdAndTargetTypeAndTargetId(String userId, VoteTargetType targetType, Long targetId);

    @Query("select coalesce(sum(v.value), 0) from Vote v where v.targetType = :targetType and v.targetId = :targetId")
    Integer sumForTarget(@Param("targetType") VoteTargetType targetType, @Param("targetId") Long targetId);
}
