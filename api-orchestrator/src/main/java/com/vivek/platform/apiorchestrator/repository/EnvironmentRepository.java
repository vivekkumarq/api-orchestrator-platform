package com.vivek.platform.apiorchestrator.repository;

import com.vivek.platform.apiorchestrator.domain.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {

    List<EnvironmentEntity> findAllByOrderByNameAsc();

    Optional<EnvironmentEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
