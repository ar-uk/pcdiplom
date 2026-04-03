package org.example.partservice.repository;

import org.example.partservice.model.Cpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CpuRepository extends JpaRepository<Cpu, Long>, JpaSpecificationExecutor<Cpu> {
	Optional<Cpu> findFirstByNameIgnoreCase(String name);
}