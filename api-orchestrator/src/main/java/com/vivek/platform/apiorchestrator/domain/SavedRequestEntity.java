package com.vivek.platform.apiorchestrator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A request saved inside a collection.
 *
 * <p>Headers, assertions and extractions are stored as JSON text rather than as child tables:
 * they are always read and written as a whole, and keeping them opaque means the schema works
 * unchanged on both H2 and PostgreSQL.
 */
@Entity
@Table(name = "saved_request")
public class SavedRequestEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private CollectionEntity collection;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String url;

    @Column(nullable = false, length = 10)
    private String method;

    /** JSON object of header name to value. */
    @Lob
    private String headersJson;

    /** JSON object of query parameter name to value. */
    @Lob
    private String queryParamsJson;

    @Lob
    private String body;

    /** JSON array of AssertionSpec. */
    @Lob
    private String assertionsJson;

    /** JSON array of ExtractionSpec. */
    @Lob
    private String extractionsJson;

    private Integer timeoutMs;
    private Integer maxRetries;
    private Integer retryBackoffMs;

    private int sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CollectionEntity getCollection() { return collection; }
    public void setCollection(CollectionEntity collection) { this.collection = collection; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }

    public String getQueryParamsJson() { return queryParamsJson; }
    public void setQueryParamsJson(String queryParamsJson) { this.queryParamsJson = queryParamsJson; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAssertionsJson() { return assertionsJson; }
    public void setAssertionsJson(String assertionsJson) { this.assertionsJson = assertionsJson; }

    public String getExtractionsJson() { return extractionsJson; }
    public void setExtractionsJson(String extractionsJson) { this.extractionsJson = extractionsJson; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(Integer retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
