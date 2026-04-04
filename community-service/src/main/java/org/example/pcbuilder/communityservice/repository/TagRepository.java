package org.example.pcbuilder.communityservice.repository;

import java.util.Optional;
import org.example.pcbuilder.communityservice.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
}
