package com.vivek.platform.apiorchestrator.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A request stored inside a collection. */
public class SavedRequestDto {

    private UUID id;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 4000)
    private String url;

    @NotBlank
    @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS",
            message = "must be one of GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS")
    private String method;

    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;

    @Valid
    private List<AssertionSpec> assertions;

    @Valid
    private List<ExtractionSpec> extractions;

    private Integer timeoutMs;
    private Integer maxRetries;
    private Integer retryBackoffMs;
    private int sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public List<AssertionSpec> getAssertions() { return assertions; }
    public void setAssertions(List<AssertionSpec> assertions) { this.assertions = assertions; }

    public List<ExtractionSpec> getExtractions() { return extractions; }
    public void setExtractions(List<ExtractionSpec> extractions) { this.extractions = extractions; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(Integer retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
