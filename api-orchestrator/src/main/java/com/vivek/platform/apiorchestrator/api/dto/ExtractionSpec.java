package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Pulls a value out of the response and binds it to a variable name, so a later request can
 * refer to it as {@code {{name}}}. This is what makes request chaining work.
 */
@Schema(description = "Extracts a response value into a variable for use by a later request.")
public class ExtractionSpec {

    @NotBlank
    @Schema(description = "Variable name to bind. Referenced later as {{name}}.", example = "authToken")
    private String name;

    @NotBlank
    @Schema(description = "JSONPath into the response body.", example = "$.token")
    private String jsonPath;

    /**
     * When true (the default) and the execution named an environment, the extracted value is
     * written back into that environment so it survives beyond this single response.
     */
    @Schema(description = "Persist the value into the active environment.", example = "true")
    private boolean persist = true;

    public ExtractionSpec() {
    }

    public ExtractionSpec(String name, String jsonPath, boolean persist) {
        this.name = name;
        this.jsonPath = jsonPath;
        this.persist = persist;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJsonPath() { return jsonPath; }
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }

    public boolean isPersist() { return persist; }
    public void setPersist(boolean persist) { this.persist = persist; }
}
