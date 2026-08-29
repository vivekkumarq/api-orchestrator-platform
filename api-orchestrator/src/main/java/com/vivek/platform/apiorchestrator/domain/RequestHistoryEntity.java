package com.vivek.platform.apiorchestrator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_history", indexes = @Index(name = "idx_history_created_at", columnList = "createdAt"))
public class RequestHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    /** The URL the user typed, still containing any {{variable}} placeholders. */
    @Column(length = 4000)
    private String url;

    /** The URL actually sent, after variable substitution and query-parameter expansion. */
    @Column(length = 4000)
    private String resolvedUrl;

    @Column(length = 10)
    private String method;

    /** JSON object. Previously this held a Java Map.toString(), which is not parseable JSON. */
    @Lob
    private String requestHeaders;

    @Lob
    private String requestBody;

    private int status;

    @Lob
    private String responseHeaders;

    /** Truncated to app.executor.max-persisted-body-chars before being stored. */
    @Lob
    private String responseBody;

    private boolean responseBodyTruncated;

    private long responseSizeBytes;

    private long responseTimeMs;

    /** Total attempts including the first, so 1 means "no retry was needed". */
    private int attempts = 1;

    /** Transport-level failure message. Null when the exchange completed, whatever the status. */
    @Column(length = 2000)
    private String errorMessage;

    /** JSON array of AssertionResult. Null when the request declared no assertions. */
    @Lob
    private String assertionResults;

    /** True only when every declared assertion passed. Null when none were declared. */
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

    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public boolean isResponseBodyTruncated() { return responseBodyTruncated; }
    public void setResponseBodyTruncated(boolean responseBodyTruncated) { this.responseBodyTruncated = responseBodyTruncated; }

    public long getResponseSizeBytes() { return responseSizeBytes; }
    public void setResponseSizeBytes(long responseSizeBytes) { this.responseSizeBytes = responseSizeBytes; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getAssertionResults() { return assertionResults; }
    public void setAssertionResults(String assertionResults) { this.assertionResults = assertionResults; }

    public Boolean getAssertionsPassed() { return assertionsPassed; }
    public void setAssertionsPassed(Boolean assertionsPassed) { this.assertionsPassed = assertionsPassed; }

    public UUID getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(UUID environmentId) { this.environmentId = environmentId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
