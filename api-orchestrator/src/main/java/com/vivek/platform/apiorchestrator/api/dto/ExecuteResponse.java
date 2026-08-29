package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "The captured result of one execution.")
public class ExecuteResponse {

    @Schema(description = "HTTP status, or 0 when the exchange never completed. See errorMessage.")
    private int status;

    private Map<String, String> headers = new LinkedHashMap<>();

    private String body;

    private long responseTimeMs;

    @Schema(description = "The URL actually sent, after substitution and query expansion.")
    private String resolvedUrl;

    @Schema(description = "Size of the response body in bytes.")
    private long responseSizeBytes;

    @Schema(description = "True when the body was cut to the configured in-memory limit.")
    private boolean bodyTruncated;

    @Schema(description = "Total attempts including the first. 1 means no retry was needed.")
    private int attempts = 1;

    /**
     * Transport-level failure. The original code encoded these into the body as
     * {@code "ERROR: ..."}, which was indistinguishable from a real body that happened to
     * start with that text.
     */
    @Schema(description = "Transport failure message; null when the exchange completed.")
    private String errorMessage;

    private List<AssertionResult> assertions;

    @Schema(description = "True only when every declared assertion passed. Null when none were declared.")
    private Boolean assertionsPassed;

    @Schema(description = "Values pulled out by the request's extraction specs.")
    private Map<String, String> extracted = new LinkedHashMap<>();

    @Schema(description = "Identifier of the history row written for this execution.")
    private UUID historyId;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public String getResolvedUrl() { return resolvedUrl; }
    public void setResolvedUrl(String resolvedUrl) { this.resolvedUrl = resolvedUrl; }

    public long getResponseSizeBytes() { return responseSizeBytes; }
    public void setResponseSizeBytes(long responseSizeBytes) { this.responseSizeBytes = responseSizeBytes; }

    public boolean isBodyTruncated() { return bodyTruncated; }
    public void setBodyTruncated(boolean bodyTruncated) { this.bodyTruncated = bodyTruncated; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<AssertionResult> getAssertions() { return assertions; }
    public void setAssertions(List<AssertionResult> assertions) { this.assertions = assertions; }

    public Boolean getAssertionsPassed() { return assertionsPassed; }
    public void setAssertionsPassed(Boolean assertionsPassed) { this.assertionsPassed = assertionsPassed; }

    public Map<String, String> getExtracted() { return extracted; }
    public void setExtracted(Map<String, String> extracted) { this.extracted = extracted; }

    public UUID getHistoryId() { return historyId; }
    public void setHistoryId(UUID historyId) { this.historyId = historyId; }
}
