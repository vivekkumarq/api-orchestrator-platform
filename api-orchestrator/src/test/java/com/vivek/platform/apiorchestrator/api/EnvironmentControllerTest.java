package com.vivek.platform.apiorchestrator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.repository.EnvironmentRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnvironmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @BeforeEach
    void clean() {
        environmentRepository.deleteAll();
    }

    private UUID create(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/environments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("id").asText());
    }

    @Test
    @DisplayName("creates, lists, updates and deletes an environment with its variables")
    void environmentCrud() throws Exception {
        UUID id = create("""
                {"name":"Local","variables":{"baseUrl":"http://localhost:3000","token":"dev"}}
                """);

        mockMvc.perform(get("/api/environments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Local"))
                .andExpect(jsonPath("$.variables.baseUrl").value("http://localhost:3000"))
                .andExpect(jsonPath("$.variables.token").value("dev"));

        mockMvc.perform(get("/api/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        // A full update replaces the variable map rather than merging into it.
        mockMvc.perform(put("/api/environments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Staging\",\"variables\":{\"baseUrl\":\"https://staging.example.com\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Staging"))
                .andExpect(jsonPath("$.variables.baseUrl").value("https://staging.example.com"))
                .andExpect(jsonPath("$.variables.token").doesNotExist());

        mockMvc.perform(delete("/api/environments/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/environments/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sets a single variable without resending the whole environment")
    void setsSingleVariable() throws Exception {
        UUID id = create("{\"name\":\"Single\",\"variables\":{\"a\":\"1\"}}");

        mockMvc.perform(put("/api/environments/{id}/variables/{key}", id, "b")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables.a").value("1"))
                .andExpect(jsonPath("$.variables.b").value("2"));
    }

    @Test
    @DisplayName("drops blank variable keys instead of storing them")
    void dropsBlankKeys() throws Exception {
        UUID id = create("{\"name\":\"Blanks\",\"variables\":{\"good\":\"yes\",\"  \":\"no\"}}");

        mockMvc.perform(get("/api/environments/{id}", id))
                .andExpect(jsonPath("$.variables.good").value("yes"))
                .andExpect(jsonPath("$.variables", org.hamcrest.Matchers.aMapWithSize(1)));
    }

    @Test
    @DisplayName("rejects an environment with no name and 404s on an unknown id")
    void validatesAndReportsMissing() throws Exception {
        mockMvc.perform(post("/api/environments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/environments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
