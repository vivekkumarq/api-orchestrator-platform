package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * History row as returned by the list endpoint. Deliberately excludes response bodies: the list
 * used to be served by returning entities straight from {@code findAll()}, which streamed every
 * captured response body in the database on every page load.
 */
@Schema(description = "A history row without response bodies; fetch one by id for the full record.")
public class HistorySummaryDto {

    private UUID id;
    private String url;
    private String resolvedUrl;
    private String method;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private int status;
    private long responseTimeMs;
    private long responseSizeBytes;
    private int attempts;
    private String errorMessage;
    private Boolean assertionsPassed;
    private UUID environmentId;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getResolvedUrl() { return resolvedUrl; }
    public void setResolvedUrl(String resolvedUrl) { this.resolvedUrl = resolvedUrl; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public long getResponseSizeBytes() { return responseSizeBytes; }
    public void setResponseSizeBytes(long responseSizeBytes) { this.responseSizeBytes = responseSizeBytes; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Boolean getAssertionsPassed() { return assertionsPassed; }
    public void setAssertionsPassed(Boolean assertionsPassed) { this.assertionsPassed = assertionsPassed; }

    public UUID getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(UUID environmentId) { this.environmentId = environmentId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
