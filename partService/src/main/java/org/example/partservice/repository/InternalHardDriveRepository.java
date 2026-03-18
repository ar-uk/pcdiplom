package org.example.partservice.repository;

import org.example.partservice.model.InternalHardDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalHardDriveRepository extends JpaRepository<InternalHardDrive, Long>, JpaSpecificationExecutor<InternalHardDrive> {
}