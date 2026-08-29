package com.vivek.platform.apiorchestrator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VariableResolverTest {

    private final VariableResolver resolver = new VariableResolver();

    @Test
    @DisplayName("substitutes placeholders anywhere in the template")
    void substitutesPlaceholders() {
        Map<String, String> vars = Map.of("baseUrl", "https://api.example.com", "id", "42");

        assertThat(resolver.resolve("{{baseUrl}}/posts/{{id}}", vars))
                .isEqualTo("https://api.example.com/posts/42");
    }

    @Test
    @DisplayName("tolerates whitespace inside the braces")
    void tolerantOfWhitespace() {
        assertThat(resolver.resolve("{{ token }}", Map.of("token", "abc"))).isEqualTo("abc");
    }

    @Test
    @DisplayName("leaves unknown placeholders verbatim so typos stay visible")
    void leavesUnknownPlaceholders() {
        assertThat(resolver.resolve("{{baseUrl}}/x", Map.of())).isEqualTo("{{baseUrl}}/x");
        assertThat(resolver.resolve("{{a}}/{{b}}", Map.of("a", "1"))).isEqualTo("1/{{b}}");
    }

    @Test
    @DisplayName("does not expand a value that itself contains a placeholder")
    void substitutionIsSinglePass() {
        Map<String, String> vars = Map.of("outer", "{{inner}}", "inner", "boom");

        assertThat(resolver.resolve("{{outer}}", vars)).isEqualTo("{{inner}}");
    }

    @Test
    @DisplayName("treats a dollar or backslash in a value as literal text")
    void replacementIsLiteral() {
        assertThat(resolver.resolve("{{v}}", Map.of("v", "$1\\x"))).isEqualTo("$1\\x");
    }

    @Test
    @DisplayName("later layers win when merging variable maps")
    void mergePrefersLaterLayers() {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("host", "staging");
        environment.put("key", "env");
        Map<String, String> inline = Map.of("key", "inline");

        Map<String, String> merged = resolver.merge(environment, inline);

        assertThat(merged).containsEntry("host", "staging").containsEntry("key", "inline");
    }

    @Test
    @DisplayName("resolves both keys and values of a map and drops blank keys")
    void resolvesMaps() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("Authorization", "Bearer {{token}}");
        source.put("{{headerName}}", "yes");
        source.put("  ", "dropped");

        Map<String, String> resolved = resolver.resolveMap(source,
                Map.of("token", "t0k3n", "headerName", "X-Trace"));

        assertThat(resolved)
                .containsEntry("Authorization", "Bearer t0k3n")
                .containsEntry("X-Trace", "yes")
                .hasSize(2);
    }

    @Test
    @DisplayName("null and empty templates pass through untouched")
    void handlesNulls() {
        assertThat(resolver.resolve(null, Map.of("a", "b"))).isNull();
        assertThat(resolver.resolve("plain", null)).isEqualTo("plain");
        assertThat(resolver.resolveMap(null, Map.of())).isEmpty();
    }
}
