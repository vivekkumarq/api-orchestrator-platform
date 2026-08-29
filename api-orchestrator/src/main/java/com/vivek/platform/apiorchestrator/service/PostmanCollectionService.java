package com.vivek.platform.apiorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vivek.platform.apiorchestrator.api.dto.AssertionSpec;
import com.vivek.platform.apiorchestrator.api.dto.CollectionDto;
import com.vivek.platform.apiorchestrator.api.dto.EnvironmentDto;
import com.vivek.platform.apiorchestrator.api.dto.ExtractionSpec;
import com.vivek.platform.apiorchestrator.api.dto.PostmanImportResult;
import com.vivek.platform.apiorchestrator.api.dto.SavedRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Converts between Postman v2.1 collection JSON and this application's collections.
 *
 * <p>Postman's schema is broader than what an execution engine needs, so the mapping is
 * deliberately partial and the gaps are documented rather than faked:
 *
 * <ul>
 *   <li>Folders are flattened. A request inside a folder keeps the folder in its name
 *       ("Auth / Login"), because this application has a flat collection model.</li>
 *   <li>Postman {@code event} / {@code pm.test} scripts are <em>not</em> translated into
 *       assertions. Executing arbitrary JavaScript is out of scope.</li>
 *   <li>Assertions and extractions, which have no Postman equivalent, are carried in a
 *       non-standard {@code _apiOrchestrator} object on each item. Postman ignores unknown
 *       keys, and reading it back gives a lossless round-trip through this application.</li>
 *   <li>Only {@code raw} and {@code urlencoded} body modes are read; file and formdata bodies
 *       are skipped.</li>
 * </ul>
 */
@Service
public class PostmanCollectionService {

    private static final String SCHEMA_V21 =
            "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";
    private static final String EXTENSION_KEY = "_apiOrchestrator";

    private final CollectionService collectionService;
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper;

    public PostmanCollectionService(CollectionService collectionService,
                                    EnvironmentService environmentService,
                                    ObjectMapper objectMapper) {
        this.collectionService = collectionService;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
    }

    // ---- import -----------------------------------------------------------------------

    @Transactional
    public PostmanImportResult importCollection(JsonNode root, String nameOverride) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Body must be a Postman collection JSON object");
        }
        JsonNode info = root.path("info");
        String name = nameOverride != null && !nameOverride.isBlank()
                ? nameOverride.trim()
                : text(info, "name", "Imported collection");

        CollectionDto collectionDto = new CollectionDto();
        collectionDto.setName(name);
        collectionDto.setDescription(text(info, "description", null));
        CollectionDto created = collectionService.create(collectionDto);

        List<SavedRequestDto> flattened = new ArrayList<>();
        collectItems(root.path("item"), "", flattened);
        for (int i = 0; i < flattened.size(); i++) {
            SavedRequestDto dto = flattened.get(i);
            dto.setSortOrder(i);
            collectionService.addRequest(created.getId(), dto);
        }

        EnvironmentDto environment = importVariables(root.path("variable"), name);

        return new PostmanImportResult(
                collectionService.findById(created.getId()), environment, flattened.size());
    }

    private void collectItems(JsonNode items, String prefix, List<SavedRequestDto> out) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            String itemName = text(item, "name", "Untitled");
            if (item.has("item")) {
                // A folder: recurse, carrying the folder name into the child names.
                collectItems(item.path("item"), prefix.isEmpty() ? itemName : prefix + " / " + itemName, out);
            } else {
                out.add(toSavedRequest(item, prefix.isEmpty() ? itemName : prefix + " / " + itemName));
            }
        }
    }

    private SavedRequestDto toSavedRequest(JsonNode item, String name) {
        JsonNode request = item.path("request");
        SavedRequestDto dto = new SavedRequestDto();
        dto.setName(name);
        dto.setMethod(normaliseMethod(text(request, "method", "GET")));

        UrlParts url = readUrl(request.path("url"));
        dto.setUrl(url.raw());
        dto.setQueryParams(url.query());
        dto.setHeaders(readHeaders(request.path("header")));
        dto.setBody(readBody(request.path("body")));

        JsonNode extension = item.path(EXTENSION_KEY);
        if (extension.isObject()) {
            dto.setAssertions(convert(extension.path("assertions"), AssertionSpec[].class));
            dto.setExtractions(convert(extension.path("extractions"), ExtractionSpec[].class));
            if (extension.hasNonNull("timeoutMs")) {
                dto.setTimeoutMs(extension.get("timeoutMs").asInt());
            }
            if (extension.hasNonNull("maxRetries")) {
                dto.setMaxRetries(extension.get("maxRetries").asInt());
            }
            if (extension.hasNonNull("retryBackoffMs")) {
                dto.setRetryBackoffMs(extension.get("retryBackoffMs").asInt());
            }
        }
        return dto;
    }

    /** Postman URLs are either a plain string or an object with {@code raw} plus parsed parts. */
    private UrlParts readUrl(JsonNode url) {
        if (url.isTextual()) {
            return new UrlParts(stripQuery(url.asText()), queryFromRaw(url.asText()));
        }
        if (url.isObject()) {
            String raw = text(url, "raw", "");
            Map<String, String> query = new LinkedHashMap<>();
            JsonNode queryNode = url.path("query");
            if (queryNode.isArray()) {
                for (JsonNode q : queryNode) {
                    if (q.path("disabled").asBoolean(false)) {
                        continue;
                    }
                    String key = text(q, "key", null);
                    if (key != null && !key.isBlank()) {
                        query.put(key, text(q, "value", ""));
                    }
                }
            }
            if (query.isEmpty()) {
                query = queryFromRaw(raw);
            }
            return new UrlParts(stripQuery(raw), query);
        }
        // The sample collection at the repo root has items with no url at all. Import them
        // rather than failing the whole file; the empty URL is visible in the UI.
        return new UrlParts("", new LinkedHashMap<>());
    }

    private static String stripQuery(String raw) {
        int idx = raw.indexOf('?');
        return idx < 0 ? raw : raw.substring(0, idx);
    }

    private static Map<String, String> queryFromRaw(String raw) {
        Map<String, String> query = new LinkedHashMap<>();
        int idx = raw.indexOf('?');
        if (idx < 0 || idx == raw.length() - 1) {
            return query;
        }
        for (String pair : raw.substring(idx + 1).split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                query.put(pair, "");
            } else {
                query.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return query;
    }

    private Map<String, String> readHeaders(JsonNode headers) {
        Map<String, String> map = new LinkedHashMap<>();
        if (headers.isArray()) {
            for (JsonNode header : headers) {
                if (header.path("disabled").asBoolean(false)) {
                    continue;
                }
                String key = text(header, "key", null);
                if (key != null && !key.isBlank()) {
                    map.put(key, text(header, "value", ""));
                }
            }
        }
        return map;
    }

    private String readBody(JsonNode body) {
        if (!body.isObject()) {
            return null;
        }
        String mode = text(body, "mode", "");
        if ("raw".equals(mode)) {
            return body.path("raw").isTextual() ? body.path("raw").asText() : null;
        }
        if ("urlencoded".equals(mode) && body.path("urlencoded").isArray()) {
            List<String> pairs = new ArrayList<>();
            for (JsonNode entry : body.path("urlencoded")) {
                if (entry.path("disabled").asBoolean(false)) {
                    continue;
                }
                pairs.add(text(entry, "key", "") + "=" + text(entry, "value", ""));
            }
            return pairs.isEmpty() ? null : String.join("&", pairs);
        }
        return null;
    }

    private EnvironmentDto importVariables(JsonNode variables, String collectionName) {
        if (!variables.isArray() || variables.isEmpty()) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (JsonNode variable : variables) {
            String key = text(variable, "key", null);
            if (key != null && !key.isBlank()) {
                values.put(key, text(variable, "value", ""));
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        EnvironmentDto dto = new EnvironmentDto();
        dto.setName(collectionName + " variables");
        dto.setVariables(values);
        return environmentService.create(dto);
    }

    // ---- export -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ObjectNode exportCollection(UUID collectionId) {
        CollectionDto collection = collectionService.findById(collectionId);

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode info = root.putObject("info");
        info.put("_postman_id", collection.getId() == null ? UUID.randomUUID().toString()
                : collection.getId().toString());
        info.put("name", collection.getName());
        info.put("schema", SCHEMA_V21);
        if (collection.getDescription() != null) {
            info.put("description", collection.getDescription());
        }

        ArrayNode items = root.putArray("item");
        for (SavedRequestDto saved : collection.getRequests()) {
            items.add(toPostmanItem(saved));
        }
        return root;
    }

    private ObjectNode toPostmanItem(SavedRequestDto saved) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("name", saved.getName());

        ObjectNode request = item.putObject("request");
        request.put("method", saved.getMethod());

        ArrayNode headers = request.putArray("header");
        if (saved.getHeaders() != null) {
            saved.getHeaders().forEach((k, v) -> {
                ObjectNode header = headers.addObject();
                header.put("key", k);
                header.put("value", v);
                header.put("type", "text");
            });
        }

        ObjectNode url = request.putObject("url");
        url.put("raw", rawUrl(saved));
        ArrayNode query = url.putArray("query");
        if (saved.getQueryParams() != null) {
            saved.getQueryParams().forEach((k, v) -> {
                ObjectNode param = query.addObject();
                param.put("key", k);
                param.put("value", v);
            });
        }

        if (saved.getBody() != null && !saved.getBody().isEmpty()) {
            ObjectNode body = request.putObject("body");
            body.put("mode", "raw");
            body.put("raw", saved.getBody());
        }

        item.putArray("response");

        // Non-standard block so an export/import cycle through this application is lossless.
        ObjectNode extension = objectMapper.createObjectNode();
        boolean any = false;
        if (saved.getAssertions() != null && !saved.getAssertions().isEmpty()) {
            extension.set("assertions", objectMapper.valueToTree(saved.getAssertions()));
            any = true;
        }
        if (saved.getExtractions() != null && !saved.getExtractions().isEmpty()) {
            extension.set("extractions", objectMapper.valueToTree(saved.getExtractions()));
            any = true;
        }
        if (saved.getTimeoutMs() != null) {
            extension.put("timeoutMs", saved.getTimeoutMs());
            any = true;
        }
        if (saved.getMaxRetries() != null) {
            extension.put("maxRetries", saved.getMaxRetries());
            any = true;
        }
        if (saved.getRetryBackoffMs() != null) {
            extension.put("retryBackoffMs", saved.getRetryBackoffMs());
            any = true;
        }
        if (any) {
            item.set(EXTENSION_KEY, extension);
        }
        return item;
    }

    private static String rawUrl(SavedRequestDto saved) {
        String base = saved.getUrl() == null ? "" : saved.getUrl();
        Map<String, String> query = saved.getQueryParams();
        if (query == null || query.isEmpty()) {
            return base;
        }
        List<String> pairs = new ArrayList<>();
        query.forEach((k, v) -> pairs.add(k + "=" + (v == null ? "" : v)));
        return base + (base.contains("?") ? "&" : "?") + String.join("&", pairs);
    }

    // ---- small helpers ----------------------------------------------------------------

    private static String normaliseMethod(String method) {
        String upper = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> upper;
            default -> "GET";
        };
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : fallback;
    }

    private <T> List<T> convert(JsonNode node, Class<T[]> arrayType) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.treeToValue(node, arrayType));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private record UrlParts(String raw, Map<String, String> query) {
    }
}
