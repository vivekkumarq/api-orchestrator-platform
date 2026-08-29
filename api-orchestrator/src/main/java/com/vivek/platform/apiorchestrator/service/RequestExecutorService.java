package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.api.dto.AssertionResult;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.api.dto.ExtractionSpec;
import com.vivek.platform.apiorchestrator.config.AppProperties;
import com.vivek.platform.apiorchestrator.domain.EnvironmentEntity;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.exception.NotFoundException;
import com.vivek.platform.apiorchestrator.repository.EnvironmentRepository;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The execution engine.
 *
 * <p>For one call it resolves {@code {{variables}}} into the URL, headers, query parameters and
 * body; checks the resolved URL against the outbound safety policy; performs the exchange with a
 * bounded timeout and optional exponential backoff; evaluates the declared assertions; extracts
 * any chained values into the active environment; and writes a history row.
 */
@Service
public class RequestExecutorService {

    private static final Logger log = LoggerFactory.getLogger(RequestExecutorService.class);

    /** Methods for which we are willing to attach a request body. */
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RequestHistoryRepository historyRepository;
    private final EnvironmentRepository environmentRepository;
    private final WebClient webClient;
    private final AppProperties properties;
    private final VariableResolver variableResolver;
    private final UrlSafetyValidator urlSafetyValidator;
    private final AssertionEvaluator assertionEvaluator;
    private final JsonPathExtractor jsonPathExtractor;
    private final ObjectMapper objectMapper;

    public RequestExecutorService(RequestHistoryRepository historyRepository,
                                  EnvironmentRepository environmentRepository,
                                  WebClient orchestratorWebClient,
                                  AppProperties properties,
                                  VariableResolver variableResolver,
                                  UrlSafetyValidator urlSafetyValidator,
                                  AssertionEvaluator assertionEvaluator,
                                  JsonPathExtractor jsonPathExtractor,
                                  ObjectMapper objectMapper) {
        this.historyRepository = historyRepository;
        this.environmentRepository = environmentRepository;
        this.webClient = orchestratorWebClient;
        this.properties = properties;
        this.variableResolver = variableResolver;
        this.urlSafetyValidator = urlSafetyValidator;
        this.assertionEvaluator = assertionEvaluator;
        this.jsonPathExtractor = jsonPathExtractor;
        this.objectMapper = objectMapper;
    }

    public ExecuteResponse execute(ExecuteRequest req) {
        AppProperties.Executor cfg = properties.getExecutor();

        // ---- 1. Variable resolution -------------------------------------------------------
        EnvironmentEntity environment = loadEnvironment(req.getEnvironmentId());
        Map<String, String> variables = variableResolver.merge(
                environment == null ? Map.of() : environment.getVariables(),
                req.getVariables());

        String resolvedBase = variableResolver.resolve(req.getUrl(), variables);
        Map<String, String> headers = variableResolver.resolveMap(req.getHeaders(), variables);
        Map<String, String> queryParams = variableResolver.resolveMap(req.getQueryParams(), variables);
        String body = variableResolver.resolve(req.getBody(), variables);

        String fullUrl = appendQueryParams(resolvedBase, queryParams);

        // ---- 2. Safety policy -------------------------------------------------------------
        // Throws UnsafeUrlException (400) before any socket is opened.
        URI uri = urlSafetyValidator.validate(fullUrl);

        // ---- 3. Exchange ------------------------------------------------------------------
        int timeoutMs = clamp(req.getTimeoutMs(), cfg.getDefaultTimeoutMs(), 1, cfg.getMaxTimeoutMs());
        int retries = clamp(req.getMaxRetries(), 0, 0, cfg.getMaxRetries());
        int backoffMs = clamp(req.getRetryBackoffMs(), 200, 1, 30_000);
        HttpMethod method = HttpMethod.valueOf(req.getMethod().toUpperCase(Locale.ROOT));

        AtomicInteger attempts = new AtomicInteger();
        long start = System.currentTimeMillis();

        Mono<ExecuteResponse> exchange = Mono
                .defer(() -> {
                    attempts.incrementAndGet();
                    return doExchange(method, uri, headers, body, start, retries > 0);
                })
                .timeout(Duration.ofMillis(timeoutMs));

        if (retries > 0) {
            exchange = exchange.retryWhen(Retry.backoff(retries, Duration.ofMillis(backoffMs))
                    .jitter(0d)
                    .filter(RequestExecutorService::isRetryable));
        }

        ExecuteResponse response = exchange
                .onErrorResume(ex -> Mono.just(toErrorResponse(ex, uri, start)))
                // The .block() runs on the servlet worker thread, never on a Netty event loop,
                // so it cannot starve the reactive client. It is still bounded: a hung peer that
                // somehow escapes the per-attempt timeout must not pin a request thread forever.
                .block(overallBudget(timeoutMs, retries, backoffMs));

        if (response == null) {
            response = new ExecuteResponse();
            response.setStatus(0);
            response.setErrorMessage("Execution produced no result");
            response.setResponseTimeMs(System.currentTimeMillis() - start);
            response.setResolvedUrl(uri.toString());
        }
        response.setAttempts(attempts.get());
        response.setResolvedUrl(uri.toString());

        // ---- 4. Assertions ----------------------------------------------------------------
        List<AssertionResult> assertionResults = assertionEvaluator.evaluate(req.getAssertions(), response);
        if (!assertionResults.isEmpty()) {
            response.setAssertions(assertionResults);
            response.setAssertionsPassed(assertionResults.stream().allMatch(AssertionResult::isPassed));
        }

        // ---- 5. Extraction for request chaining -------------------------------------------
        response.setExtracted(applyExtractions(req.getExtractions(), response, environment));

        // ---- 6. History -------------------------------------------------------------------
        // Deliberately outside any transaction spanning the network call: holding a database
        // connection open while waiting on a third-party API is how connection pools die.
        response.setHistoryId(saveHistory(req, response, headers, body, uri).getId());

        return response;
    }

    // -------------------------------------------------------------------------------------

    private Mono<ExecuteResponse> doExchange(HttpMethod method, URI uri, Map<String, String> headers,
                                             String body, long start, boolean retryServerErrors) {
        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(uri)
                .headers(h -> headers.forEach(h::add));

        // The original code always called bodyValue(""), which put a Content-Length: 0 on every
        // GET and HEAD. Some servers reject that outright.
        WebClient.RequestHeadersSpec<?> finalSpec =
                (body != null && !body.isEmpty() && BODY_METHODS.contains(method.name()))
                        ? spec.bodyValue(body)
                        : spec;

        return finalSpec.exchangeToMono(clientResponse -> {
            Map<String, String> responseHeaders =
                    new LinkedHashMap<>(clientResponse.headers().asHttpHeaders().toSingleValueMap());
            int status = clientResponse.statusCode().value();
            Charset charset = charsetOf(responseHeaders);

            return readBoundedBody(clientResponse, charset).map(captured -> {
                ExecuteResponse r = new ExecuteResponse();
                r.setStatus(status);
                r.setHeaders(responseHeaders);
                r.setBody(captured.body());
                r.setResponseSizeBytes(captured.totalBytes());
                r.setBodyTruncated(captured.truncated());
                r.setResponseTimeMs(System.currentTimeMillis() - start);
                return r;
            });
        }).flatMap(r -> (retryServerErrors && r.getStatus() >= 500)
                ? Mono.error(new RetryableResponseException(r))
                : Mono.just(r));
    }

    /**
     * Reads the body but stops copying once {@code max-response-bytes} has been captured, while
     * still draining and releasing the remaining buffers. The original {@code bodyToMono(String)}
     * had no ceiling at all, so a multi-gigabyte response was an out-of-memory error.
     */
    private Mono<CapturedBody> readBoundedBody(
            org.springframework.web.reactive.function.client.ClientResponse clientResponse, Charset charset) {
        int max = properties.getExecutor().getMaxResponseBytes();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        AtomicLong total = new AtomicLong();

        return clientResponse.bodyToFlux(DataBuffer.class)
                .doOnNext(buffer -> {
                    try {
                        int readable = buffer.readableByteCount();
                        total.addAndGet(readable);
                        int room = max - sink.size();
                        if (room > 0) {
                            int n = Math.min(room, readable);
                            byte[] chunk = new byte[n];
                            buffer.read(chunk, 0, n);
                            sink.write(chunk, 0, n);
                        }
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .then(Mono.fromSupplier(() -> new CapturedBody(
                        sink.toString(charset), total.get(), total.get() > sink.size())));
    }

    private record CapturedBody(String body, long totalBytes, boolean truncated) {
    }

    private static boolean isRetryable(Throwable t) {
        return t instanceof RetryableResponseException
                || t instanceof TimeoutException
                || t instanceof java.io.IOException
                || t instanceof org.springframework.web.reactive.function.client.WebClientRequestException;
    }

    /**
     * Turns a terminal failure into a reportable response. A retry-exhausted 5xx is unwrapped
     * back into the real response the server sent; anything else becomes status 0 plus an
     * {@code errorMessage}. The old code stuffed {@code "ERROR: " + message} into the body,
     * which a caller could not tell apart from a genuine body.
     */
    private ExecuteResponse toErrorResponse(Throwable ex, URI uri, long start) {
        Throwable cause = Exceptions.isRetryExhausted(ex) && ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof RetryableResponseException retryable) {
            return retryable.getResponse();
        }
        ExecuteResponse err = new ExecuteResponse();
        err.setStatus(0);
        err.setHeaders(new LinkedHashMap<>());
        err.setBody(null);
        err.setResolvedUrl(uri.toString());
        err.setResponseTimeMs(System.currentTimeMillis() - start);
        err.setErrorMessage(cause instanceof TimeoutException
                ? "Request timed out"
                : cause.getClass().getSimpleName() + ": " + cause.getMessage());
        log.debug("Execution of {} failed", uri, cause);
        return err;
    }

    private Map<String, String> applyExtractions(List<ExtractionSpec> specs, ExecuteResponse response,
                                                 EnvironmentEntity environment) {
        Map<String, String> extracted = new LinkedHashMap<>();
        if (specs == null || specs.isEmpty()) {
            return extracted;
        }
        boolean environmentTouched = false;
        for (ExtractionSpec spec : specs) {
            if (spec == null || spec.getName() == null || spec.getName().isBlank()) {
                continue;
            }
            Optional<String> value = jsonPathExtractor.read(response.getBody(), spec.getJsonPath());
            if (value.isEmpty()) {
                continue;
            }
            extracted.put(spec.getName(), value.get());
            if (spec.isPersist() && environment != null) {
                environment.getVariables().put(spec.getName(), value.get());
                environmentTouched = true;
            }
        }
        if (environmentTouched) {
            environment.setUpdatedAt(Instant.now());
            environmentRepository.save(environment);
        }
        return extracted;
    }

    private RequestHistoryEntity saveHistory(ExecuteRequest req, ExecuteResponse res,
                                             Map<String, String> resolvedHeaders, String resolvedBody, URI uri) {
        int maxChars = properties.getExecutor().getMaxPersistedBodyChars();
        String storedBody = truncate(res.getBody(), maxChars);

        RequestHistoryEntity h = new RequestHistoryEntity();
        h.setUrl(truncate(req.getUrl(), 4000));
        h.setResolvedUrl(truncate(uri.toString(), 4000));
        h.setMethod(req.getMethod());
        // Real JSON, not Map.toString(). The UI parses this when replaying a history entry.
        h.setRequestHeaders(toJson(resolvedHeaders));
        h.setRequestBody(truncate(resolvedBody, maxChars));
        h.setStatus(res.getStatus());
        h.setResponseHeaders(toJson(res.getHeaders()));
        h.setResponseBody(storedBody);
        h.setResponseBodyTruncated(res.isBodyTruncated()
                || (res.getBody() != null && res.getBody().length() > maxChars));
        h.setResponseSizeBytes(res.getResponseSizeBytes());
        h.setResponseTimeMs(res.getResponseTimeMs());
        h.setAttempts(res.getAttempts());
        h.setErrorMessage(truncate(res.getErrorMessage(), 2000));
        h.setAssertionsPassed(res.getAssertionsPassed());
        h.setAssertionResults(res.getAssertions() == null ? null : toJson(res.getAssertions()));
        h.setEnvironmentId(req.getEnvironmentId());
        h.setCreatedAt(Instant.now());
        return historyRepository.save(h);
    }

    private EnvironmentEntity loadEnvironment(java.util.UUID environmentId) {
        if (environmentId == null) {
            return null;
        }
        return environmentRepository.findById(environmentId)
                .orElseThrow(() -> new NotFoundException("Environment " + environmentId + " not found"));
    }

    static String appendQueryParams(String url, Map<String, String> queryParams) {
        if (url == null || queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? '&' : '?');
        List<String> pairs = new ArrayList<>();
        queryParams.forEach((k, v) -> pairs.add(URLEncoder.encode(k, StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8)));
        sb.append(String.join("&", pairs));
        return sb.toString();
    }

    private static Charset charsetOf(Map<String, String> headers) {
        String contentType = headers.entrySet().stream()
                .filter(e -> "content-type".equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
        int idx = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (idx < 0) {
            return StandardCharsets.UTF_8;
        }
        String name = contentType.substring(idx + "charset=".length()).split(";")[0].trim().replace("\"", "");
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }

    /** Overall ceiling for the blocking wait: every attempt plus every backoff, plus slack. */
    private static Duration overallBudget(int timeoutMs, int retries, int backoffMs) {
        long backoffTotal = 0;
        for (int i = 0; i < retries; i++) {
            backoffTotal += (long) backoffMs << i;
        }
        return Duration.ofMillis((long) timeoutMs * (retries + 1) + backoffTotal + 5_000);
    }

    private static int clamp(Integer requested, int fallback, int min, int max) {
        int value = requested == null ? fallback : requested;
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise {} for history", value.getClass().getSimpleName(), e);
            return null;
        }
    }
}
