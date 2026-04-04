package org.example.pcbuilder.communityservice.repository;

import java.util.List;
import java.util.Optional;
import org.example.pcbuilder.communityservice.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
}
