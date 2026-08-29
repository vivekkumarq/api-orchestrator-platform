package com.vivek.platform.apiorchestrator.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {{name}}} placeholders using a flat variable map.
 *
 * <p>Substitution is single-pass: a value that itself contains {@code {{...}}} is not expanded
 * again. That is deliberate — it keeps resolution terminating and predictable, and rules out a
 * self-referential variable spinning forever.
 *
 * <p>An unknown placeholder is left verbatim rather than replaced with an empty string, so a
 * typo shows up in the resolved URL instead of silently producing a wrong request.
 */
@Component
public class VariableResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.\\-]+)\\s*}}");

    /** Later maps win, so callers pass environment variables first and inline overrides after. */
    @SafeVarargs
    public final Map<String, String> merge(Map<String, String>... layers) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (Map<String, String> layer : layers) {
            if (layer != null) {
                layer.forEach((k, v) -> {
                    if (k != null) {
                        merged.put(k, v == null ? "" : v);
                    }
                });
            }
        }
        return merged;
    }

    public String resolve(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty() || variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.containsKey(key) ? variables.get(key) : matcher.group(0);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Resolves both keys and values of a map, dropping entries whose key resolves to blank. */
    public Map<String, String> resolveMap(Map<String, String> source, Map<String, String> variables) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (source == null) {
            return resolved;
        }
        source.forEach((key, value) -> {
            String resolvedKey = resolve(key, variables);
            if (resolvedKey != null && !resolvedKey.isBlank()) {
                resolved.put(resolvedKey, resolve(value == null ? "" : value, variables));
            }
        });
        return resolved;
    }
}
