package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.api.dto.AssertionResult;
import com.vivek.platform.apiorchestrator.api.dto.HistoryDetailDto;
import com.vivek.platform.apiorchestrator.api.dto.HistorySummaryDto;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.exception.NotFoundException;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HistoryService {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<AssertionResult>> ASSERTION_RESULTS = new TypeReference<>() {
    };

    private final RequestHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public HistoryService(RequestHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Paged and newest-first. The endpoint used to be {@code findAll()}, which returned the whole
     * table including every stored response body on every call.
     */
    @Transactional(readOnly = true)
    public Page<HistorySummaryDto> findPage(int page, int size) {
        int safeSize = Math.max(1, Math.min(200, size));
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), safeSize))
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public HistoryDetailDto findById(UUID id) {
        RequestHistoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("History entry " + id + " not found"));
        return toDetail(entity);
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    private HistorySummaryDto toSummary(RequestHistoryEntity entity) {
        HistorySummaryDto dto = new HistorySummaryDto();
        fill(entity, dto);
        return dto;
    }

    private HistoryDetailDto toDetail(RequestHistoryEntity entity) {
        HistoryDetailDto dto = new HistoryDetailDto();
        fill(entity, dto);
        dto.setResponseHeaders(readJson(entity.getResponseHeaders(), STRING_MAP, new LinkedHashMap<>()));
        dto.setResponseBody(entity.getResponseBody());
        dto.setResponseBodyTruncated(entity.isResponseBodyTruncated());
        dto.setAssertions(readJson(entity.getAssertionResults(), ASSERTION_RESULTS, List.of()));
        return dto;
    }

    private void fill(RequestHistoryEntity entity, HistorySummaryDto dto) {
        dto.setId(entity.getId());
        dto.setUrl(entity.getUrl());
        dto.setResolvedUrl(entity.getResolvedUrl());
        dto.setMethod(entity.getMethod());
        dto.setRequestHeaders(readJson(entity.getRequestHeaders(), STRING_MAP, new LinkedHashMap<>()));
        dto.setRequestBody(entity.getRequestBody());
        dto.setStatus(entity.getStatus());
        dto.setResponseTimeMs(entity.getResponseTimeMs());
        dto.setResponseSizeBytes(entity.getResponseSizeBytes());
        dto.setAttempts(entity.getAttempts());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setAssertionsPassed(entity.getAssertionsPassed());
        dto.setEnvironmentId(entity.getEnvironmentId());
        dto.setCreatedAt(entity.getCreatedAt());
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
