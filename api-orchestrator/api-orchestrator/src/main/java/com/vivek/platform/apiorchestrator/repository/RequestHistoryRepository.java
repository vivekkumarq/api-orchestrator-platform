package com.vivek.platform.apiorchestrator.repository;

import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestHistoryRepository extends JpaRepository<RequestHistoryEntity, UUID> {
}