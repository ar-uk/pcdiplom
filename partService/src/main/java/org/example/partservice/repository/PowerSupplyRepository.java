package org.example.partservice.repository;

import org.example.partservice.model.PowerSupply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PowerSupplyRepository extends JpaRepository<PowerSupply, Long>, JpaSpecificationExecutor<PowerSupply> {
	Optional<PowerSupply> findFirstByNameIgnoreCase(String name);
}