package org.example.partservice.repository;

import org.example.partservice.model.VideoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoCardRepository extends JpaRepository<VideoCard, Long>, JpaSpecificationExecutor<VideoCard> {
	Optional<VideoCard> findFirstByNameIgnoreCase(String name);
}