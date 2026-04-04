package org.example.pcbuilder.communityservice.repository;

import java.util.List;
import java.util.Optional;
import org.example.pcbuilder.communityservice.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long postId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
