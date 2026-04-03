package org.example.partservice.repository;

import org.example.partservice.model.ParsedVideoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedVideoCardRepository extends JpaRepository<ParsedVideoCard, Long> {
    Optional<ParsedVideoCard> findFirstByUrl(String url);
}
