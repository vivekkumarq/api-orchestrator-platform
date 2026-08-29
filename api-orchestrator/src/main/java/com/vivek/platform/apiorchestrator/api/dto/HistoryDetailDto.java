package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** The full history record, including the captured response. */
@Schema(description = "A history row with the captured response body, headers and assertions.")
public class HistoryDetailDto extends HistorySummaryDto {

    private Map<String, String> responseHeaders;
    private String responseBody;
    private boolean responseBodyTruncated;
    private List<AssertionResult> assertions;

    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public boolean isResponseBodyTruncated() { return responseBodyTruncated; }
    public void setResponseBodyTruncated(boolean responseBodyTruncated) { this.responseBodyTruncated = responseBodyTruncated; }

    public List<AssertionResult> getAssertions() { return assertions; }
    public void setAssertions(List<AssertionResult> assertions) { this.assertions = assertions; }
}
