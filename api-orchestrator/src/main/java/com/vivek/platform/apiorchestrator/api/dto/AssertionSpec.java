package com.vivek.platform.apiorchestrator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** One expectation declared against the response of a request. */
@Schema(description = "An expectation evaluated against the response after execution.")
public class AssertionSpec {

    @NotNull
    @Schema(example = "STATUS_EQUALS")
    private AssertionType type;

    @Schema(description = "JSONPath or header name, depending on the type.", example = "$.id")
    private String target;

    @Schema(description = "The expected value. Ignored by HEADER_PRESENT.", example = "200")
    private String expected;

    public AssertionSpec() {
    }

    public AssertionSpec(AssertionType type, String target, String expected) {
        this.type = type;
        this.target = target;
        this.expected = expected;
    }

    public AssertionType getType() { return type; }
    public void setType(AssertionType type) { this.type = type; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
}
