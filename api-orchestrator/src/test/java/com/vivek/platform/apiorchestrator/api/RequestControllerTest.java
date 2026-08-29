package com.vivek.platform.apiorchestrator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestHistoryRepository historyRepository;

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        historyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("executes over HTTP and records the result in a paged history")
    void executeAndReadHistory() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":1}"));

        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "method", "GET",
                "url", server.url("/items/1").toString(),
                "assertions", java.util.List.of(java.util.Map.of("type", "STATUS_EQUALS", "expected", "200"))));

        MvcResult executed = mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body").value("{\"id\":1}"))
                .andExpect(jsonPath("$.assertionsPassed").value(true))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.historyId").exists())
                .andReturn();

        UUID historyId = UUID.fromString(objectMapper
                .readTree(executed.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("historyId").asText());

        // The list is paged and omits response bodies.
        mockMvc.perform(get("/api/requests/history").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value(200))
                .andExpect(jsonPath("$.content[0].requestHeaders").exists())
                .andExpect(jsonPath("$.content[0].responseBody").doesNotExist());

        // The detail endpoint carries the captured response.
        mockMvc.perform(get("/api/requests/history/{id}", historyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBody").value("{\"id\":1}"))
                .andExpect(jsonPath("$.responseHeaders['Content-Type']")
                        .value(org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.assertions[0].passed").value(true));

        mockMvc.perform(delete("/api/requests/history")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/requests/history"))
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("rejects an unsupported HTTP method with 400 rather than failing with a 500")
    void rejectsUnknownMethod() throws Exception {
        mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"FETCH\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("method")));
    }

    @Test
    @DisplayName("rejects a blank URL")
    void rejectsBlankUrl() throws Exception {
        mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects a URL the outbound policy refuses, with a 400 explaining why")
    void rejectsBlockedUrl() throws Exception {
        mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"http://169.254.169.254/latest/meta-data/\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("URL refused by outbound policy"));
    }

    @Test
    @DisplayName("rejects a scheme outside the allowlist")
    void rejectsBadScheme() throws Exception {
        mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"file:///etc/passwd\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404s when the named environment does not exist")
    void unknownEnvironment() throws Exception {
        String payload = "{\"method\":\"GET\",\"url\":\"https://example.com\",\"environmentId\":\""
                + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/requests/execute")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404s for an unknown history id")
    void unknownHistoryEntry() throws Exception {
        mockMvc.perform(get("/api/requests/history/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
