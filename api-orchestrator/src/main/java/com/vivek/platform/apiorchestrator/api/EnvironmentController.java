package com.vivek.platform.apiorchestrator.api;

import com.vivek.platform.apiorchestrator.api.dto.EnvironmentDto;
import com.vivek.platform.apiorchestrator.service.EnvironmentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Environments", description = "Named variable sets used to resolve {{placeholders}}")
@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService service;

    public EnvironmentController(EnvironmentService service) {
        this.service = service;
    }

    @Operation(summary = "List environments")
    @GetMapping
    public List<EnvironmentDto> list() {
        return service.findAll();
    }

    @Operation(summary = "Read one environment")
    @GetMapping("/{id}")
    public EnvironmentDto get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "Create an environment")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnvironmentDto create(@RequestBody @Valid EnvironmentDto dto) {
        return service.create(dto);
    }

    @Operation(summary = "Replace an environment's name and variables")
    @PutMapping("/{id}")
    public EnvironmentDto update(@PathVariable UUID id, @RequestBody @Valid EnvironmentDto dto) {
        return service.update(id, dto);
    }

    @Operation(summary = "Set a single variable",
            description = "Body is {\"value\": \"...\"}. Useful for tweaking one value without "
                    + "sending the whole environment back.")
    @PutMapping("/{id}/variables/{key}")
    public EnvironmentDto setVariable(@PathVariable UUID id, @PathVariable String key,
                                      @RequestBody Map<String, String> body) {
        return service.setVariable(id, key, body.get("value"));
    }

    @Operation(summary = "Delete an environment")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
