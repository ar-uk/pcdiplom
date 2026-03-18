package org.example.partservice.repository;

import org.example.partservice.model.Motherboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MotherboardRepository extends JpaRepository<Motherboard, Long>, JpaSpecificationExecutor<Motherboard> {
}