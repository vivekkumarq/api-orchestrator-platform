package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.AssertionType;
import com.vivek.platform.apiorchestrator.api.dto.CollectionDto;
import com.vivek.platform.apiorchestrator.api.dto.ExtractionSpec;
import com.vivek.platform.apiorchestrator.api.dto.PostmanImportResult;
import com.vivek.platform.apiorchestrator.api.dto.SavedRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostmanCollectionServiceTest {

    @Autowired
    private PostmanCollectionService postmanService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode load(String resource) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertThat(in).as("test resource %s", resource).isNotNull();
            return objectMapper.readTree(in);
        }
    }

    @Test
    @DisplayName("imports the repository's own sample collection, including items that carry no URL")
    void importsSampleCollection() throws IOException {
        PostmanImportResult result = postmanService.importCollection(
                load("/postman/api-orchestrator.postman_collection.json"), null);

        assertThat(result.getImportedRequests()).isEqualTo(3);
        assertThat(result.getCollection().getName()).isEqualTo("api-orchestrator");
        assertThat(result.getCollection().getRequests())
                .extracting(SavedRequestDto::getName)
                .containsExactly("normal-req", "errorRequest", "history");
        assertThat(result.getCollection().getRequests())
                .allMatch(r -> "GET".equals(r.getMethod()));
        // The sample has no url on any item; importing must not drop the request.
        assertThat(result.getCollection().getRequests().get(0).getUrl()).isEmpty();
        assertThat(result.getEnvironment()).isNull();
    }

    @Test
    @DisplayName("reads headers, raw bodies, query parameters and nested folders")
    void importsRichCollection() throws IOException {
        PostmanImportResult result = postmanService.importCollection(load("/postman/rich.postman_collection.json"), null);

        List<SavedRequestDto> requests = result.getCollection().getRequests();
        assertThat(requests).extracting(SavedRequestDto::getName)
                .containsExactly("List users", "Auth / Login");

        SavedRequestDto list = requests.get(0);
        assertThat(list.getMethod()).isEqualTo("GET");
        assertThat(list.getUrl()).isEqualTo("{{baseUrl}}/users");
        assertThat(list.getQueryParams()).containsEntry("page", "1").containsEntry("size", "20");
        assertThat(list.getHeaders()).containsEntry("Accept", "application/json");
        // The disabled header must not survive the import.
        assertThat(list.getHeaders()).doesNotContainKey("X-Disabled");

        SavedRequestDto login = requests.get(1);
        assertThat(login.getMethod()).isEqualTo("POST");
        assertThat(login.getBody()).contains("\"username\"");

        // Collection-level variables become an environment.
        assertThat(result.getEnvironment()).isNotNull();
        assertThat(result.getEnvironment().getVariables())
                .containsEntry("baseUrl", "https://api.example.com");
    }

    @Test
    @DisplayName("export produces v2.1 JSON with the schema, name and one item per request")
    void exportsPostmanV21() {
        CollectionDto dto = new CollectionDto();
        dto.setName("Exported " + System.nanoTime());
        CollectionDto created = collectionService.create(dto);

        SavedRequestDto saved = new SavedRequestDto();
        saved.setName("Get widget");
        saved.setMethod("GET");
        saved.setUrl("{{baseUrl}}/widgets/1");
        saved.setHeaders(new LinkedHashMap<>(Map.of("Accept", "application/json")));
        saved.setQueryParams(new LinkedHashMap<>(Map.of("verbose", "true")));
        collectionService.addRequest(created.getId(), saved);

        ObjectNode exported = postmanService.exportCollection(created.getId());

        assertThat(exported.path("info").path("schema").asText())
                .isEqualTo("https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        assertThat(exported.path("info").path("name").asText()).isEqualTo(dto.getName());
        assertThat(exported.path("item")).hasSize(1);

        JsonNode item = exported.path("item").get(0);
        assertThat(item.path("name").asText()).isEqualTo("Get widget");
        assertThat(item.path("request").path("method").asText()).isEqualTo("GET");
        assertThat(item.path("request").path("url").path("raw").asText())
                .isEqualTo("{{baseUrl}}/widgets/1?verbose=true");
        assertThat(item.path("request").path("header").get(0).path("key").asText()).isEqualTo("Accept");
    }

    @Test
    @DisplayName("export then import round-trips every field, assertions and extractions included")
    void roundTripsThroughExportAndImport() {
        CollectionDto dto = new CollectionDto();
        dto.setName("RoundTrip " + System.nanoTime());
        CollectionDto created = collectionService.create(dto);

        SavedRequestDto original = new SavedRequestDto();
        original.setName("Login");
        original.setMethod("POST");
        original.setUrl("{{baseUrl}}/auth/login");
        original.setHeaders(new LinkedHashMap<>(Map.of("Content-Type", "application/json")));
        original.setQueryParams(new LinkedHashMap<>(Map.of("remember", "1")));
        original.setBody("{\"username\":\"vivek\"}");
        original.setAssertions(List.of(
                new AssertionSpec(AssertionType.STATUS_EQUALS, null, "200"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.ok", "true")));
        original.setExtractions(List.of(new ExtractionSpec("authToken", "$.token", true)));
        original.setTimeoutMs(4000);
        original.setMaxRetries(2);
        original.setRetryBackoffMs(150);
        collectionService.addRequest(created.getId(), original);

        ObjectNode exported = postmanService.exportCollection(created.getId());
        PostmanImportResult reimported = postmanService.importCollection(exported, "Reimported " + System.nanoTime());

        assertThat(reimported.getImportedRequests()).isEqualTo(1);
        SavedRequestDto restored = reimported.getCollection().getRequests().get(0);

        assertThat(restored.getName()).isEqualTo("Login");
        assertThat(restored.getMethod()).isEqualTo("POST");
        assertThat(restored.getUrl()).isEqualTo("{{baseUrl}}/auth/login");
        assertThat(restored.getQueryParams()).containsEntry("remember", "1");
        assertThat(restored.getHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(restored.getBody()).isEqualTo("{\"username\":\"vivek\"}");
        assertThat(restored.getTimeoutMs()).isEqualTo(4000);
        assertThat(restored.getMaxRetries()).isEqualTo(2);
        assertThat(restored.getRetryBackoffMs()).isEqualTo(150);
        assertThat(restored.getAssertions()).hasSize(2);
        assertThat(restored.getAssertions().get(1).getType()).isEqualTo(AssertionType.JSON_PATH_EQUALS);
        assertThat(restored.getAssertions().get(1).getTarget()).isEqualTo("$.ok");
        assertThat(restored.getExtractions()).hasSize(1);
        assertThat(restored.getExtractions().get(0).getName()).isEqualTo("authToken");
        assertThat(restored.getExtractions().get(0).getJsonPath()).isEqualTo("$.token");
    }
}
