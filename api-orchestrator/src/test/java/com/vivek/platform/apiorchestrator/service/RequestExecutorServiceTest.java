package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.AssertionType;
import com.vivek.platform.apiorchestrator.api.dto.EnvironmentDto;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.api.dto.ExtractionSpec;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.exception.UnsafeUrlException;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the execution engine against a real local HTTP server. This is the heart of the
 * application, so it is tested over a socket rather than against a mocked WebClient.
 */
@SpringBootTest
class RequestExecutorServiceTest {

    @Autowired
    private RequestExecutorService executor;

    @Autowired
    private EnvironmentService environmentService;

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

    private String url(String path) {
        return server.url(path).toString();
    }

    private ExecuteRequest request(String method, String path) {
        ExecuteRequest req = new ExecuteRequest();
        req.setMethod(method);
        req.setUrl(url(path));
        return req;
    }

    // ---- happy path -------------------------------------------------------------------

    @Test
    @DisplayName("captures status, headers, body and timing of a successful request")
    void executesSuccessfully() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Trace", "t-1")
                .setBody("{\"id\":7,\"name\":\"widget\"}"));

        ExecuteRequest req = request("GET", "/items/7");
        req.setHeaders(Map.of("Accept", "application/json"));

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getBody()).isEqualTo("{\"id\":7,\"name\":\"widget\"}");
        assertThat(res.getHeaders()).containsEntry("X-Trace", "t-1");
        assertThat(res.getResponseSizeBytes()).isEqualTo(24);
        assertThat(res.isBodyTruncated()).isFalse();
        assertThat(res.getErrorMessage()).isNull();
        assertThat(res.getAttempts()).isEqualTo(1);
        assertThat(res.getResponseTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(res.getHistoryId()).isNotNull();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/items/7");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("a GET carries no request body, not even an empty one")
    void getSendsNoBody() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        executor.execute(request("GET", "/ping"));

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getBodySize()).isZero();
        assertThat(recorded.getHeader("Content-Length")).isNull();
    }

    @Test
    @DisplayName("a POST sends its body and content type")
    void postSendsBody() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"created\":true}"));

        ExecuteRequest req = request("POST", "/items");
        req.setHeaders(Map.of("Content-Type", "application/json"));
        req.setBody("{\"name\":\"new\"}");

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(201);
        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"name\":\"new\"}");
        assertThat(recorded.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    @DisplayName("query parameters are appended and URL-encoded")
    void appendsQueryParameters() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        ExecuteRequest req = request("GET", "/search");
        req.setQueryParams(new java.util.LinkedHashMap<>(Map.of("q", "hello world")));

        executor.execute(req);

        assertThat(server.takeRequest().getPath()).isEqualTo("/search?q=hello+world");
    }

    // ---- non-2xx ----------------------------------------------------------------------

    @Test
    @DisplayName("a 404 is a result, not an error: body and status are both reported")
    void reportsNotFoundAsResult() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"error\":\"missing\"}"));

        ExecuteResponse res = executor.execute(request("GET", "/nope"));

        assertThat(res.getStatus()).isEqualTo(404);
        assertThat(res.getBody()).isEqualTo("{\"error\":\"missing\"}");
        assertThat(res.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("a 500 without retries is returned as-is")
    void reportsServerErrorAsResult() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        ExecuteResponse res = executor.execute(request("GET", "/broken"));

        assertThat(res.getStatus()).isEqualTo(500);
        assertThat(res.getBody()).isEqualTo("boom");
        assertThat(res.getAttempts()).isEqualTo(1);
    }

    // ---- timeout ----------------------------------------------------------------------

    @Test
    @DisplayName("a slow response times out and is reported as status 0 with an error message")
    void timesOut() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("late")
                .setHeadersDelay(2, TimeUnit.SECONDS));

        ExecuteRequest req = request("GET", "/slow");
        req.setTimeoutMs(250);

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isZero();
        assertThat(res.getErrorMessage()).isEqualTo("Request timed out");
        // The failure is in errorMessage, not smuggled into the body as "ERROR: ...".
        assertThat(res.getBody()).isNull();
    }

    @Test
    @DisplayName("a timed-out execution is still written to history")
    void timeoutIsRecorded() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("late")
                .setHeadersDelay(2, TimeUnit.SECONDS));

        ExecuteRequest req = request("GET", "/slow");
        req.setTimeoutMs(200);
        executor.execute(req);

        List<RequestHistoryEntity> history = historyRepository.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isZero();
        assertThat(history.get(0).getErrorMessage()).isEqualTo("Request timed out");
    }

    // ---- retry ------------------------------------------------------------------------

    @Test
    @DisplayName("retries a 5xx and reports the eventual success plus the attempt count")
    void retriesServerErrorsUntilSuccess() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("try later"));
        server.enqueue(new MockResponse().setResponseCode(503).setBody("try later"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("finally"));

        ExecuteRequest req = request("GET", "/flaky");
        req.setMaxRetries(2);
        req.setRetryBackoffMs(10);

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getBody()).isEqualTo("finally");
        assertThat(res.getAttempts()).isEqualTo(3);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("when retries are exhausted the last real response is returned, not a synthetic error")
    void returnsLastResponseWhenRetriesExhausted() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("down"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("down"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("still down"));

        ExecuteRequest req = request("GET", "/down");
        req.setMaxRetries(2);
        req.setRetryBackoffMs(10);

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(500);
        assertThat(res.getBody()).isEqualTo("still down");
        assertThat(res.getErrorMessage()).isNull();
        assertThat(res.getAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("a 4xx is not retried")
    void doesNotRetryClientErrors() {
        server.enqueue(new MockResponse().setResponseCode(400).setBody("bad"));

        ExecuteRequest req = request("GET", "/bad");
        req.setMaxRetries(3);
        req.setRetryBackoffMs(10);

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(res.getAttempts()).isEqualTo(1);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    // ---- variables and chaining -------------------------------------------------------

    @Test
    @DisplayName("resolves {{variables}} from an environment across URL, headers and body")
    void substitutesFromEnvironment() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        EnvironmentDto env = new EnvironmentDto();
        env.setName("substitution-" + System.nanoTime());
        env.setVariables(new java.util.LinkedHashMap<>(Map.of(
                "baseUrl", url("").replaceAll("/$", ""),
                "token", "s3cret",
                "who", "world")));
        EnvironmentDto created = environmentService.create(env);

        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("POST");
        req.setUrl("{{baseUrl}}/greet");
        req.setHeaders(Map.of("Authorization", "Bearer {{token}}"));
        req.setBody("{\"hello\":\"{{who}}\"}");
        req.setEnvironmentId(created.getId());

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getResolvedUrl()).endsWith("/greet").doesNotContain("{{");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/greet");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer s3cret");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    @DisplayName("inline variables win over the environment's")
    void inlineVariablesOverrideEnvironment() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        EnvironmentDto env = new EnvironmentDto();
        env.setName("override-" + System.nanoTime());
        env.setVariables(new java.util.LinkedHashMap<>(Map.of("who", "environment")));
        EnvironmentDto created = environmentService.create(env);

        ExecuteRequest req = request("POST", "/echo");
        req.setBody("{{who}}");
        req.setEnvironmentId(created.getId());
        req.setVariables(Map.of("who", "inline"));

        executor.execute(req);

        assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("inline");
    }

    @Test
    @DisplayName("extraction writes a response value into the environment for the next request")
    void extractsValueIntoEnvironmentForChaining() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"abc-123\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        EnvironmentDto env = new EnvironmentDto();
        env.setName("chain-" + System.nanoTime());
        EnvironmentDto created = environmentService.create(env);

        // Step 1: log in and capture the token.
        ExecuteRequest login = request("POST", "/login");
        login.setBody("{}");
        login.setEnvironmentId(created.getId());
        login.setExtractions(List.of(new ExtractionSpec("authToken", "$.token", true)));

        ExecuteResponse loginResponse = executor.execute(login);
        assertThat(loginResponse.getExtracted()).containsEntry("authToken", "abc-123");
        assertThat(environmentService.findById(created.getId()).getVariables())
                .containsEntry("authToken", "abc-123");

        server.takeRequest();

        // Step 2: a separate execution now resolves {{authToken}} from the environment.
        ExecuteRequest follow = request("GET", "/me");
        follow.setEnvironmentId(created.getId());
        follow.setHeaders(Map.of("Authorization", "Bearer {{authToken}}"));

        executor.execute(follow);

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer abc-123");
    }

    @Test
    @DisplayName("an extraction with persist=false is reported but not written to the environment")
    void extractionCanSkipPersistence() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"xyz\"}"));

        EnvironmentDto env = new EnvironmentDto();
        env.setName("nopersist-" + System.nanoTime());
        EnvironmentDto created = environmentService.create(env);

        ExecuteRequest req = request("GET", "/thing");
        req.setEnvironmentId(created.getId());
        req.setExtractions(List.of(new ExtractionSpec("thingId", "$.id", false)));

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getExtracted()).containsEntry("thingId", "xyz");
        assertThat(environmentService.findById(created.getId()).getVariables()).doesNotContainKey("thingId");
    }

    // ---- assertions -------------------------------------------------------------------

    @Test
    @DisplayName("assertions are evaluated against the live response and summarised")
    void evaluatesAssertions() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":7}"));

        ExecuteRequest req = request("GET", "/items/7");
        req.setAssertions(List.of(
                new AssertionSpec(AssertionType.STATUS_EQUALS, null, "200"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.id", "7"),
                new AssertionSpec(AssertionType.HEADER_PRESENT, "Content-Type", null)));

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getAssertionsPassed()).isTrue();
        assertThat(res.getAssertions()).hasSize(3).allMatch(a -> a.isPassed());
    }

    @Test
    @DisplayName("one failing assertion fails the set, and the outcome is persisted")
    void reportsFailingAssertions() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":7}"));

        ExecuteRequest req = request("GET", "/items/7");
        req.setAssertions(List.of(
                new AssertionSpec(AssertionType.STATUS_EQUALS, null, "200"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.id", "9")));

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getAssertionsPassed()).isFalse();
        assertThat(res.getAssertions().get(1).getActual()).isEqualTo("7");
        assertThat(historyRepository.findAll().get(0).getAssertionsPassed()).isFalse();
    }

    // ---- history and safety -----------------------------------------------------------

    @Test
    @DisplayName("history stores request headers as parseable JSON, not Map.toString()")
    void historyStoresHeadersAsJson() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        ExecuteRequest req = request("GET", "/x");
        req.setHeaders(Map.of("X-One", "1"));
        executor.execute(req);

        String stored = historyRepository.findAll().get(0).getRequestHeaders();
        assertThat(stored).isEqualTo("{\"X-One\":\"1\"}");
    }

    @Test
    @DisplayName("a connection refused becomes status 0 with an error message rather than a 500")
    void reportsConnectionFailure() throws IOException {
        int port = server.getPort();
        server.shutdown();

        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("GET");
        req.setUrl("http://127.0.0.1:" + port + "/gone");
        req.setTimeoutMs(1500);

        ExecuteResponse res = executor.execute(req);

        assertThat(res.getStatus()).isZero();
        assertThat(res.getErrorMessage()).isNotBlank();
    }

    @Test
    @DisplayName("the outbound policy rejects a blocked host before any socket is opened")
    void refusesBlockedHost() {
        ExecuteRequest req = new ExecuteRequest();
        req.setMethod("GET");
        req.setUrl("http://169.254.169.254/latest/meta-data/");

        assertThatThrownBy(() -> executor.execute(req)).isInstanceOf(UnsafeUrlException.class);
        assertThat(historyRepository.findAll()).isEmpty();
    }
}
