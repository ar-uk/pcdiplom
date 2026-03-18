package org.example.partservice.repository;

import org.example.partservice.model.PowerSupply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerSupplyRepository extends JpaRepository<PowerSupply, Long>, JpaSpecificationExecutor<PowerSupply> {
}