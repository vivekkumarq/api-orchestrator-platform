package com.vivek.platform.apiorchestrator.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vivek.platform.apiorchestrator.api.dto.CollectionDto;
import com.vivek.platform.apiorchestrator.api.dto.PostmanImportResult;
import com.vivek.platform.apiorchestrator.api.dto.SavedRequestDto;
import com.vivek.platform.apiorchestrator.service.CollectionService;
import com.vivek.platform.apiorchestrator.service.PostmanCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Collections", description = "Named groups of saved requests, and Postman import/export")
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final PostmanCollectionService postmanService;

    public CollectionController(CollectionService collectionService,
                                PostmanCollectionService postmanService) {
        this.collectionService = collectionService;
        this.postmanService = postmanService;
    }

    @Operation(summary = "List all collections with their saved requests")
    @GetMapping
    public List<CollectionDto> list() {
        return collectionService.findAll();
    }

    @Operation(summary = "Read one collection")
    @GetMapping("/{id}")
    public CollectionDto get(@PathVariable UUID id) {
        return collectionService.findById(id);
    }

    @Operation(summary = "Create a collection")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionDto create(@RequestBody @Valid CollectionDto dto) {
        return collectionService.create(dto);
    }

    @Operation(summary = "Rename or re-describe a collection")
    @PutMapping("/{id}")
    public CollectionDto update(@PathVariable UUID id, @RequestBody @Valid CollectionDto dto) {
        return collectionService.update(id, dto);
    }

    @Operation(summary = "Delete a collection and every request in it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        collectionService.delete(id);
    }

    // ---- saved requests ---------------------------------------------------------------

    @Operation(summary = "Add a request to a collection")
    @PostMapping("/{id}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public SavedRequestDto addRequest(@PathVariable UUID id, @RequestBody @Valid SavedRequestDto dto) {
        return collectionService.addRequest(id, dto);
    }

    @Operation(summary = "Read one saved request")
    @GetMapping("/{id}/requests/{requestId}")
    public SavedRequestDto getRequest(@PathVariable UUID id, @PathVariable UUID requestId) {
        return collectionService.findRequest(id, requestId);
    }

    @Operation(summary = "Update a saved request")
    @PutMapping("/{id}/requests/{requestId}")
    public SavedRequestDto updateRequest(@PathVariable UUID id, @PathVariable UUID requestId,
                                         @RequestBody @Valid SavedRequestDto dto) {
        return collectionService.updateRequest(id, requestId, dto);
    }

    @Operation(summary = "Remove a request from a collection")
    @DeleteMapping("/{id}/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequest(@PathVariable UUID id, @PathVariable UUID requestId) {
        collectionService.deleteRequest(id, requestId);
    }

    // ---- Postman ----------------------------------------------------------------------

    @Operation(summary = "Import a Postman v2.1 collection",
            description = "Folders are flattened into the request name. Collection variables become "
                    + "a new environment. Postman test scripts are not translated into assertions.")
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public PostmanImportResult importCollection(@RequestBody JsonNode collection,
                                                @RequestParam(required = false) String name) {
        return postmanService.importCollection(collection, name);
    }

    @Operation(summary = "Export a collection as Postman v2.1 JSON")
    @GetMapping("/{id}/export")
    public ObjectNode export(@PathVariable UUID id) {
        return postmanService.exportCollection(id);
    }
}
