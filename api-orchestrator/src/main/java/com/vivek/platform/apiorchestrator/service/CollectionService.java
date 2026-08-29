package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.CollectionDto;
import com.vivek.platform.apiorchestrator.api.dto.ExtractionSpec;
import com.vivek.platform.apiorchestrator.api.dto.SavedRequestDto;
import com.vivek.platform.apiorchestrator.domain.CollectionEntity;
import com.vivek.platform.apiorchestrator.domain.SavedRequestEntity;
import com.vivek.platform.apiorchestrator.exception.NotFoundException;
import com.vivek.platform.apiorchestrator.repository.CollectionRepository;
import com.vivek.platform.apiorchestrator.repository.SavedRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CollectionService {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<AssertionSpec>> ASSERTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<ExtractionSpec>> EXTRACTION_LIST = new TypeReference<>() {
    };

    private final CollectionRepository collectionRepository;
    private final SavedRequestRepository savedRequestRepository;
    private final ObjectMapper objectMapper;

    public CollectionService(CollectionRepository collectionRepository,
                             SavedRequestRepository savedRequestRepository,
                             ObjectMapper objectMapper) {
        this.collectionRepository = collectionRepository;
        this.savedRequestRepository = savedRequestRepository;
        this.objectMapper = objectMapper;
    }

    // ---- collections ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CollectionDto> findAll() {
        return collectionRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CollectionDto findById(UUID id) {
        return toDto(requireCollection(id));
    }

    @Transactional
    public CollectionDto create(CollectionDto dto) {
        CollectionEntity entity = new CollectionEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        return toDto(collectionRepository.save(entity));
    }

    @Transactional
    public CollectionDto update(UUID id, CollectionDto dto) {
        CollectionEntity entity = requireCollection(id);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedAt(Instant.now());
        return toDto(collectionRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        collectionRepository.delete(requireCollection(id));
    }

    // ---- saved requests ---------------------------------------------------------------

    @Transactional
    public SavedRequestDto addRequest(UUID collectionId, SavedRequestDto dto) {
        CollectionEntity collection = requireCollection(collectionId);
        SavedRequestEntity entity = new SavedRequestEntity();
        apply(dto, entity);
        entity.setSortOrder(dto.getSortOrder() > 0 ? dto.getSortOrder() : collection.getRequests().size());
        collection.addRequest(entity);
        collection.setUpdatedAt(Instant.now());
        // Flush the child explicitly: cascading from the already-managed parent would not assign
        // the generated id until the transaction commits, and the caller needs it in the response.
        SavedRequestEntity saved = savedRequestRepository.saveAndFlush(entity);
        return toDto(saved);
    }

    @Transactional
    public SavedRequestDto updateRequest(UUID collectionId, UUID requestId, SavedRequestDto dto) {
        SavedRequestEntity entity = requireRequest(collectionId, requestId);
        apply(dto, entity);
        entity.setSortOrder(dto.getSortOrder());
        return toDto(savedRequestRepository.save(entity));
    }

    @Transactional
    public void deleteRequest(UUID collectionId, UUID requestId) {
        SavedRequestEntity entity = requireRequest(collectionId, requestId);
        CollectionEntity collection = entity.getCollection();
        collection.getRequests().remove(entity);
        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);
    }

    @Transactional(readOnly = true)
    public SavedRequestDto findRequest(UUID collectionId, UUID requestId) {
        return toDto(requireRequest(collectionId, requestId));
    }

    // ---- helpers ----------------------------------------------------------------------

    CollectionEntity requireCollection(UUID id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collection " + id + " not found"));
    }

    private SavedRequestEntity requireRequest(UUID collectionId, UUID requestId) {
        SavedRequestEntity entity = savedRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request " + requestId + " not found"));
        // Guard against reaching a request through the wrong collection's URL.
        if (!entity.getCollection().getId().equals(collectionId)) {
            throw new NotFoundException("Request " + requestId + " is not in collection " + collectionId);
        }
        return entity;
    }

    private void apply(SavedRequestDto dto, SavedRequestEntity entity) {
        entity.setName(dto.getName());
        entity.setUrl(dto.getUrl());
        entity.setMethod(dto.getMethod() == null ? "GET" : dto.getMethod().toUpperCase(Locale.ROOT));
        entity.setHeadersJson(writeJson(dto.getHeaders()));
        entity.setQueryParamsJson(writeJson(dto.getQueryParams()));
        entity.setBody(dto.getBody());
        entity.setAssertionsJson(writeJson(dto.getAssertions()));
        entity.setExtractionsJson(writeJson(dto.getExtractions()));
        entity.setTimeoutMs(dto.getTimeoutMs());
        entity.setMaxRetries(dto.getMaxRetries());
        entity.setRetryBackoffMs(dto.getRetryBackoffMs());
    }

    CollectionDto toDto(CollectionEntity entity) {
        CollectionDto dto = new CollectionDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setRequests(entity.getRequests().stream().map(this::toDto).toList());
        return dto;
    }

    SavedRequestDto toDto(SavedRequestEntity entity) {
        SavedRequestDto dto = new SavedRequestDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUrl(entity.getUrl());
        dto.setMethod(entity.getMethod());
        dto.setHeaders(readJson(entity.getHeadersJson(), STRING_MAP, new LinkedHashMap<>()));
        dto.setQueryParams(readJson(entity.getQueryParamsJson(), STRING_MAP, new LinkedHashMap<>()));
        dto.setBody(entity.getBody());
        dto.setAssertions(readJson(entity.getAssertionsJson(), ASSERTION_LIST, List.of()));
        dto.setExtractions(readJson(entity.getExtractionsJson(), EXTRACTION_LIST, List.of()));
        dto.setTimeoutMs(entity.getTimeoutMs());
        dto.setMaxRetries(entity.getMaxRetries());
        dto.setRetryBackoffMs(entity.getRetryBackoffMs());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialise request field", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return fallback;
        }
    }
}
