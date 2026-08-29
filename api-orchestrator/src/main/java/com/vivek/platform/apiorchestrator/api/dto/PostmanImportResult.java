package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** What an import produced: the new collection, plus an environment if the file declared variables. */
@Schema(description = "Result of importing a Postman v2.1 collection.")
public class PostmanImportResult {

    private CollectionDto collection;

    @Schema(description = "Created from the collection's variable list; null when it declared none.")
    private EnvironmentDto environment;

    private int importedRequests;

    public PostmanImportResult() {
    }

    public PostmanImportResult(CollectionDto collection, EnvironmentDto environment, int importedRequests) {
        this.collection = collection;
        this.environment = environment;
        this.importedRequests = importedRequests;
    }

    public CollectionDto getCollection() { return collection; }
    public void setCollection(CollectionDto collection) { this.collection = collection; }

    public EnvironmentDto getEnvironment() { return environment; }
    public void setEnvironment(EnvironmentDto environment) { this.environment = environment; }

    public int getImportedRequests() { return importedRequests; }
    public void setImportedRequests(int importedRequests) { this.importedRequests = importedRequests; }
}
