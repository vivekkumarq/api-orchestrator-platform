package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.EnvironmentDto;
import com.vivek.platform.apiorchestrator.domain.EnvironmentEntity;
import com.vivek.platform.apiorchestrator.exception.NotFoundException;
import com.vivek.platform.apiorchestrator.repository.EnvironmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EnvironmentService {

    private final EnvironmentRepository repository;

    public EnvironmentService(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(EnvironmentService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentDto findById(UUID id) {
        return toDto(require(id));
    }

    @Transactional
    public EnvironmentDto create(EnvironmentDto dto) {
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setName(dto.getName());
        entity.setVariables(sanitise(dto.getVariables()));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        return toDto(repository.save(entity));
    }

    @Transactional
    public EnvironmentDto update(UUID id, EnvironmentDto dto) {
        EnvironmentEntity entity = require(id);
        entity.setName(dto.getName());
        // Replace in place rather than reassigning: Hibernate tracks the managed collection.
        entity.getVariables().clear();
        entity.getVariables().putAll(sanitise(dto.getVariables()));
        entity.setUpdatedAt(Instant.now());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(require(id));
    }

    /** Sets or replaces a single variable; used by the UI's inline variable editor. */
    @Transactional
    public EnvironmentDto setVariable(UUID id, String key, String value) {
        EnvironmentEntity entity = require(id);
        entity.getVariables().put(key, value == null ? "" : value);
        entity.setUpdatedAt(Instant.now());
        return toDto(repository.save(entity));
    }

    private EnvironmentEntity require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Environment " + id + " not found"));
    }

    private static Map<String, String> sanitise(Map<String, String> variables) {
        Map<String, String> clean = new LinkedHashMap<>();
        if (variables != null) {
            variables.forEach((k, v) -> {
                if (k != null && !k.isBlank()) {
                    clean.put(k.trim(), v == null ? "" : v);
                }
            });
        }
        return clean;
    }

    static EnvironmentDto toDto(EnvironmentEntity entity) {
        EnvironmentDto dto = new EnvironmentDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setVariables(new LinkedHashMap<>(entity.getVariables()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
