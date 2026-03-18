package org.example.partservice.repository;

import org.example.partservice.model.VideoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoCardRepository extends JpaRepository<VideoCard, Long>, JpaSpecificationExecutor<VideoCard> {
}