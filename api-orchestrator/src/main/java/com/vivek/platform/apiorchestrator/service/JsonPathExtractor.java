package com.vivek.platform.apiorchestrator.service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads a value out of a JSON response body by JSONPath. Shared by assertion evaluation and by
 * the extraction specs that feed request chaining.
 */
@Component
public class JsonPathExtractor {

    private final Configuration configuration;
    private final ObjectMapper objectMapper;

    public JsonPathExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper))
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    /**
     * @return the value at {@code path}, rendered as a string. Empty when the body is not JSON,
     *         the path does not match, or the value is null. Objects and arrays come back as
     *         compact JSON so they can still be compared or stored as a variable.
     */
    public Optional<String> read(String body, String path) {
        if (body == null || body.isBlank() || path == null || path.isBlank()) {
            return Optional.empty();
        }
        Object value;
        try {
            value = JsonPath.using(configuration).parse(body).read(path);
        } catch (PathNotFoundException | IllegalArgumentException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            // Covers InvalidJsonException and malformed-path errors: the body simply is not
            // something we can read, which is a failed assertion rather than a server error.
            return Optional.empty();
        }
        return render(value);
    }

    private Optional<String> render(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String s) {
            return Optional.of(s);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return Optional.of(String.valueOf(value));
        }
        try {
            return Optional.of(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            return Optional.of(String.valueOf(value));
        }
    }
}
