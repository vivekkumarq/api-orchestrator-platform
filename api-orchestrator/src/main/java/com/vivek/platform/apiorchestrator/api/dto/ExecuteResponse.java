package com.vivek.platform.apiorchestrator.api.dto;

import java.util.Map;

public class ExecuteResponse {

    private int status;
    private Map<String, String> headers;
    private String body;
    private long responseTimeMs;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
}