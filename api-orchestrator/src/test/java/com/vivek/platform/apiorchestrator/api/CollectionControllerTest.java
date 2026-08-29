package com.vivek.platform.apiorchestrator.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.repository.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollectionRepository collectionRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAll();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private UUID createCollection(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(body(result).path("id").asText());
    }

    @Test
    @DisplayName("creates, reads, updates and deletes a collection")
    void collectionCrud() throws Exception {
        UUID id = createCollection("Smoke tests");

        mockMvc.perform(get("/api/collections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Smoke tests"))
                .andExpect(jsonPath("$.requests").isArray());

        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(put("/api/collections/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"description\":\"now described\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.description").value("now described"));

        mockMvc.perform(delete("/api/collections/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/collections/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("rejects a collection with no name")
    void rejectsInvalidCollection() throws Exception {
        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    @DisplayName("returns 404 for an unknown collection id")
    void unknownCollection() throws Exception {
        mockMvc.perform(get("/api/collections/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    @DisplayName("adds, reads, updates and removes a saved request")
    void savedRequestCrud() throws Exception {
        UUID collectionId = createCollection("Users API");

        String payload = """
                {
                  "name": "Get user",
                  "method": "GET",
                  "url": "{{baseUrl}}/users/1",
                  "headers": {"Accept": "application/json"},
                  "queryParams": {"expand": "profile"},
                  "assertions": [{"type": "STATUS_EQUALS", "expected": "200"}],
                  "extractions": [{"name": "userId", "jsonPath": "$.id", "persist": true}],
                  "timeoutMs": 3000
                }
                """;

        MvcResult created = mockMvc.perform(post("/api/collections/{id}/requests", collectionId)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Get user"))
                .andExpect(jsonPath("$.assertions[0].type").value("STATUS_EQUALS"))
                .andExpect(jsonPath("$.extractions[0].jsonPath").value("$.id"))
                .andReturn();
        UUID requestId = UUID.fromString(body(created).path("id").asText());

        mockMvc.perform(get("/api/collections/{id}/requests/{rid}", collectionId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryParams.expand").value("profile"))
                .andExpect(jsonPath("$.timeoutMs").value(3000));

        mockMvc.perform(get("/api/collections/{id}", collectionId))
                .andExpect(jsonPath("$.requests", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(put("/api/collections/{id}/requests/{rid}", collectionId, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Get user v2\",\"method\":\"POST\",\"url\":\"{{baseUrl}}/users\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Get user v2"))
                .andExpect(jsonPath("$.method").value("POST"));

        mockMvc.perform(delete("/api/collections/{id}/requests/{rid}", collectionId, requestId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/collections/{id}", collectionId))
                .andExpect(jsonPath("$.requests", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("rejects a saved request with an unsupported method")
    void rejectsBadMethod() throws Exception {
        UUID collectionId = createCollection("Bad method");

        mockMvc.perform(post("/api/collections/{id}/requests", collectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"method\":\"TRACE\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("will not reach a request through the wrong collection")
    void requestScopedToItsCollection() throws Exception {
        UUID first = createCollection("First");
        UUID second = createCollection("Second");

        MvcResult created = mockMvc.perform(post("/api/collections/{id}/requests", first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"r\",\"method\":\"GET\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID requestId = UUID.fromString(body(created).path("id").asText());

        mockMvc.perform(get("/api/collections/{id}/requests/{rid}", second, requestId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("imports a Postman collection over HTTP and exports it back")
    void importAndExportOverHttp() throws Exception {
        String collection = """
                {
                  "info": {"name": "Imported over HTTP",
                           "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
                  "item": [
                    {"name": "Ping",
                     "request": {"method": "GET", "header": [],
                                 "url": {"raw": "https://example.com/ping"}}}
                  ],
                  "variable": [{"key": "baseUrl", "value": "https://example.com"}]
                }
                """;

        MvcResult imported = mockMvc.perform(post("/api/collections/import")
                        .contentType(MediaType.APPLICATION_JSON).content(collection))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedRequests").value(1))
                .andExpect(jsonPath("$.collection.name").value("Imported over HTTP"))
                .andExpect(jsonPath("$.environment.variables.baseUrl").value("https://example.com"))
                .andReturn();

        UUID collectionId = UUID.fromString(body(imported).path("collection").path("id").asText());

        mockMvc.perform(get("/api/collections/{id}/export", collectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.schema")
                        .value("https://schema.getpostman.com/json/collection/v2.1.0/collection.json"))
                .andExpect(jsonPath("$.item[0].name").value("Ping"))
                .andExpect(jsonPath("$.item[0].request.url.raw").value("https://example.com/ping"));
    }

    @Test
    @DisplayName("import rejects a body that is not a JSON object")
    void importRejectsNonObject() throws Exception {
        mockMvc.perform(post("/api/collections/import")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
    }
}
