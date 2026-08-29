package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The original executor read the whole body with {@code bodyToMono(String.class)} and stored it
 * verbatim in a CLOB, so a large response was both an out-of-memory risk and unbounded database
 * growth. Both ceilings are configurable and are asserted here with tiny limits.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "app.executor.max-response-bytes=64",
        "app.executor.max-persisted-body-chars=16"
})
class RequestExecutorTruncationTest {

    @Autowired
    private RequestExecutorService executor;

    @Autowired
    private RequestHistoryRepository historyRepository;

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
        historyRepository.deleteAll();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("a body over the in-memory ceiling is truncated, and the true size is still reported")
    void truncatesLargeBody() {
        String payload = "x".repeat(500);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(payload));

        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("GET");
        req.setUrl(server.url("/big").toString());

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(64);
        assertThat(res.isBodyTruncated()).isTrue();
        assertThat(res.getResponseSizeBytes()).isEqualTo(500);
    }

    @Test
    @DisplayName("what reaches the database is truncated again to the persistence ceiling")
    void truncatesPersistedBody() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("y".repeat(500)));

        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("GET");
        req.setUrl(server.url("/big").toString());
        executor.execute(req);

        RequestHistoryEntity stored = historyRepository.findAll().get(0);
        assertThat(stored.getResponseBody()).hasSize(16);
        assertThat(stored.isResponseBodyTruncated()).isTrue();
        assertThat(stored.getResponseSizeBytes()).isEqualTo(500);
    }

    @Test
    @DisplayName("a body under both ceilings is stored whole and not flagged")
    void keepsSmallBodyIntact() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("short"));

        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("GET");
        req.setUrl(server.url("/small").toString());

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getBody()).isEqualTo("short");
        assertThat(res.isBodyTruncated()).isFalse();
        assertThat(historyRepository.findAll().get(0).isResponseBodyTruncated()).isFalse();
    }
}
