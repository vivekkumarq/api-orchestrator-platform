package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "A request to execute, before variable substitution.")
public class ExecuteRequest {

    @NotBlank
    @Size(max = 4000)
    @Schema(example = "{{baseUrl}}/posts/1")
    private String url;

    /**
     * Constrained to an explicit allowlist. Previously this was only {@code @NotNull}, so any
     * string reached {@code HttpMethod.valueOf} and an unknown one surfaced as a 500.
     */
    @NotBlank
    @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS",
            message = "must be one of GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS")
    @Schema(example = "GET")
    private String method;

    private Map<String, String> headers;

    @Schema(description = "Appended to the URL as a query string. Values are substituted too.")
    private Map<String, String> queryParams;

    private String body;

    @Min(1)
    @Max(300_000)
    @Schema(description = "Per-request timeout. Clamped to app.executor.max-timeout-ms.", example = "5000")
    private Integer timeoutMs;

    @Min(0)
    @Max(10)
    @Schema(description = "Retries after the first attempt. Clamped to app.executor.max-retries.", example = "2")
    private Integer maxRetries;

    @Min(0)
    @Max(30_000)
    @Schema(description = "Base delay for exponential backoff between retries.", example = "200")
    private Integer retryBackoffMs;

    @Schema(description = "Environment whose variables resolve {{placeholders}} in this request.")
    private UUID environmentId;

    @Schema(description = "Inline variables. These take precedence over the environment's.")
    private Map<String, String> variables;

    @Valid
    private List<AssertionSpec> assertions;

    @Valid
    private List<ExtractionSpec> extractions;

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

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(Integer retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public UUID getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(UUID environmentId) { this.environmentId = environmentId; }

    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

    public List<AssertionSpec> getAssertions() { return assertions; }
    public void setAssertions(List<AssertionSpec> assertions) { this.assertions = assertions; }

    public List<ExtractionSpec> getExtractions() { return extractions; }
    public void setExtractions(List<ExtractionSpec> extractions) { this.extractions = extractions; }
}
