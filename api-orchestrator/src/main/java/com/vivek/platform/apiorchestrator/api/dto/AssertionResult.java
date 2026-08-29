package com.vivek.platform.apiorchestrator.api.dto;

/** The outcome of evaluating one {@link AssertionSpec}. */
public class AssertionResult {

    private AssertionType type;
    private String target;
    private String expected;
    private String actual;
    private boolean passed;
    private String message;

    public AssertionResult() {
    }

    public AssertionResult(AssertionType type, String target, String expected,
                           String actual, boolean passed, String message) {
        this.type = type;
        this.target = target;
        this.expected = expected;
        this.actual = actual;
        this.passed = passed;
        this.message = message;
    }

    public AssertionType getType() { return type; }
    public void setType(AssertionType type) { this.type = type; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
