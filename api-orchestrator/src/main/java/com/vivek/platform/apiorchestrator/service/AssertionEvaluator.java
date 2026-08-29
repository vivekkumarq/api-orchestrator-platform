package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.AssertionResult;
import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Evaluates a request's declared expectations against the response it actually got. */
@Component
public class AssertionEvaluator {

    private final JsonPathExtractor jsonPath;

    public AssertionEvaluator(JsonPathExtractor jsonPath) {
        this.jsonPath = jsonPath;
    }

    public List<AssertionResult> evaluate(List<AssertionSpec> specs, ExecuteResponse response) {
        List<AssertionResult> results = new ArrayList<>();
        if (specs == null || specs.isEmpty()) {
            return results;
        }
        for (AssertionSpec spec : specs) {
            results.add(evaluateOne(spec, response));
        }
        return results;
    }

    private AssertionResult evaluateOne(AssertionSpec spec, ExecuteResponse response) {
        if (spec == null || spec.getType() == null) {
            return new AssertionResult(null, null, null, null, false, "Assertion has no type");
        }
        return switch (spec.getType()) {
            case STATUS_EQUALS -> {
                String actual = String.valueOf(response.getStatus());
                boolean passed = actual.equals(trim(spec.getExpected()));
                yield result(spec, actual, passed,
                        passed ? "Status is " + actual : "Expected status " + spec.getExpected() + " but got " + actual);
            }
            case RESPONSE_TIME_UNDER -> {
                String actual = response.getResponseTimeMs() + "ms";
                Long budget = parseLong(spec.getExpected());
                if (budget == null) {
                    yield result(spec, actual, false, "Expected value must be a number of milliseconds");
                }
                boolean passed = response.getResponseTimeMs() < budget;
                yield result(spec, actual, passed,
                        passed ? "Responded in " + actual : "Took " + actual + ", budget was " + budget + "ms");
            }
            case JSON_PATH_EQUALS -> {
                Optional<String> actual = jsonPath.read(response.getBody(), spec.getTarget());
                boolean passed = actual.isPresent() && actual.get().equals(trim(spec.getExpected()));
                yield result(spec, actual.orElse(null), passed, actual.isEmpty()
                        ? "No value at " + spec.getTarget()
                        : (passed ? "Matches" : "Expected " + spec.getExpected() + " but got " + actual.get()));
            }
            case JSON_PATH_CONTAINS -> {
                Optional<String> actual = jsonPath.read(response.getBody(), spec.getTarget());
                String expected = spec.getExpected() == null ? "" : spec.getExpected();
                boolean passed = actual.isPresent() && actual.get().contains(expected);
                yield result(spec, actual.orElse(null), passed, actual.isEmpty()
                        ? "No value at " + spec.getTarget()
                        : (passed ? "Contains expected text" : "Value at " + spec.getTarget() + " does not contain " + expected));
            }
            case HEADER_PRESENT -> {
                Optional<String> actual = header(response, spec.getTarget());
                boolean passed = actual.isPresent();
                yield result(spec, actual.orElse(null), passed,
                        passed ? "Header present" : "Header " + spec.getTarget() + " is absent");
            }
            case HEADER_EQUALS -> {
                Optional<String> actual = header(response, spec.getTarget());
                boolean passed = actual.isPresent() && actual.get().equals(trim(spec.getExpected()));
                yield result(spec, actual.orElse(null), passed, actual.isEmpty()
                        ? "Header " + spec.getTarget() + " is absent"
                        : (passed ? "Matches" : "Expected " + spec.getExpected() + " but got " + actual.get()));
            }
            case BODY_CONTAINS -> {
                String body = response.getBody() == null ? "" : response.getBody();
                String expected = spec.getExpected() == null ? "" : spec.getExpected();
                boolean passed = body.contains(expected);
                yield result(spec, null, passed,
                        passed ? "Body contains expected text" : "Body does not contain " + expected);
            }
        };
    }

    /** Header lookup is case-insensitive, as HTTP header names are. */
    private Optional<String> header(ExecuteResponse response, String name) {
        if (name == null || response.getHeaders() == null) {
            return Optional.empty();
        }
        return response.getHeaders().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private AssertionResult result(AssertionSpec spec, String actual, boolean passed, String message) {
        return new AssertionResult(spec.getType(), spec.getTarget(), spec.getExpected(), actual, passed, message);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(trim(value));
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
