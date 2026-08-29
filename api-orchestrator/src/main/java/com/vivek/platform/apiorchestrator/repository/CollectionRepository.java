package com.vivek.platform.apiorchestrator.repository;

import com.vivek.platform.apiorchestrator.domain.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {

    List<CollectionEntity> findAllByOrderByNameAsc();

    Optional<CollectionEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
