package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.platform.apiorchestrator.api.dto.AssertionResult;
import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.AssertionType;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssertionEvaluatorTest {

    private final AssertionEvaluator evaluator =
            new AssertionEvaluator(new JsonPathExtractor(new ObjectMapper()));

    private ExecuteResponse response() {
        ExecuteResponse r = new ExecuteResponse();
        r.setStatus(200);
        r.setResponseTimeMs(120);
        r.setBody("{\"id\":42,\"name\":\"widget\",\"tags\":[\"a\",\"b\"],\"nested\":{\"ok\":true}}");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Trace", "abc123");
        r.setHeaders(headers);
        return r;
    }

    @Test
    @DisplayName("STATUS_EQUALS passes on a match and fails with the actual status")
    void statusEquals() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.STATUS_EQUALS, null, "200"),
                new AssertionSpec(AssertionType.STATUS_EQUALS, null, "404")), response());

        assertThat(results.get(0).isPassed()).isTrue();
        assertThat(results.get(1).isPassed()).isFalse();
        assertThat(results.get(1).getActual()).isEqualTo("200");
    }

    @Test
    @DisplayName("RESPONSE_TIME_UNDER compares against the millisecond budget")
    void responseTimeUnder() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.RESPONSE_TIME_UNDER, null, "500"),
                new AssertionSpec(AssertionType.RESPONSE_TIME_UNDER, null, "10"),
                new AssertionSpec(AssertionType.RESPONSE_TIME_UNDER, null, "not-a-number")), response());

        assertThat(results.get(0).isPassed()).isTrue();
        assertThat(results.get(1).isPassed()).isFalse();
        assertThat(results.get(2).isPassed()).isFalse();
        assertThat(results.get(2).getMessage()).contains("must be a number");
    }

    @Test
    @DisplayName("JSON_PATH_EQUALS reads scalars, booleans and whole subtrees")
    void jsonPathEquals() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.id", "42"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.name", "widget"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.nested.ok", "true"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.tags[0]", "a"),
                new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.name", "gadget")), response());

        assertThat(results).extracting(AssertionResult::isPassed)
                .containsExactly(true, true, true, true, false);
    }

    @Test
    @DisplayName("a JSONPath that matches nothing fails rather than throwing")
    void jsonPathMissing() {
        List<AssertionResult> results = evaluator.evaluate(
                List.of(new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.nope", "x")), response());

        assertThat(results.get(0).isPassed()).isFalse();
        assertThat(results.get(0).getMessage()).contains("No value at");
    }

    @Test
    @DisplayName("a non-JSON body fails JSONPath assertions instead of erroring")
    void jsonPathOnNonJsonBody() {
        ExecuteResponse plain = response();
        plain.setBody("<html>not json</html>");

        List<AssertionResult> results = evaluator.evaluate(
                List.of(new AssertionSpec(AssertionType.JSON_PATH_EQUALS, "$.id", "42")), plain);

        assertThat(results.get(0).isPassed()).isFalse();
    }

    @Test
    @DisplayName("JSON_PATH_CONTAINS does a substring match on the rendered value")
    void jsonPathContains() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.JSON_PATH_CONTAINS, "$.name", "widg"),
                new AssertionSpec(AssertionType.JSON_PATH_CONTAINS, "$.tags", "\"b\"")), response());

        assertThat(results).extracting(AssertionResult::isPassed).containsExactly(true, true);
    }

    @Test
    @DisplayName("header assertions are case-insensitive on the header name")
    void headerAssertions() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.HEADER_PRESENT, "content-type", null),
                new AssertionSpec(AssertionType.HEADER_PRESENT, "X-Missing", null),
                new AssertionSpec(AssertionType.HEADER_EQUALS, "X-TRACE", "abc123"),
                new AssertionSpec(AssertionType.HEADER_EQUALS, "X-Trace", "other")), response());

        assertThat(results).extracting(AssertionResult::isPassed)
                .containsExactly(true, false, true, false);
    }

    @Test
    @DisplayName("BODY_CONTAINS matches the raw body text")
    void bodyContains() {
        List<AssertionResult> results = evaluator.evaluate(List.of(
                new AssertionSpec(AssertionType.BODY_CONTAINS, null, "widget"),
                new AssertionSpec(AssertionType.BODY_CONTAINS, null, "absent")), response());

        assertThat(results).extracting(AssertionResult::isPassed).containsExactly(true, false);
    }

    @Test
    @DisplayName("no assertions declared yields no results")
    void noAssertions() {
        assertThat(evaluator.evaluate(null, response())).isEmpty();
        assertThat(evaluator.evaluate(List.of(), response())).isEmpty();
    }
}
