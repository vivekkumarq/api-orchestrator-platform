package com.vivek.platform.apiorchestrator.repository;

import com.vivek.platform.apiorchestrator.domain.SavedRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SavedRequestRepository extends JpaRepository<SavedRequestEntity, UUID> {
}
