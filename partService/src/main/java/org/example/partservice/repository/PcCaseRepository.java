package org.example.partservice.repository;

import org.example.partservice.model.PcCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PcCaseRepository extends JpaRepository<PcCase, Long>, JpaSpecificationExecutor<PcCase> {
}
