package com.vivek.platform.apiorchestrator.api.dto;

/** The kinds of expectation a request can declare about its response. */
public enum AssertionType {

    /** {@code expected} is the exact status code, e.g. "200". */
    STATUS_EQUALS,

    /** {@code expected} is a millisecond budget; passes when the response was faster. */
    RESPONSE_TIME_UNDER,

    /** {@code target} is a JSONPath; passes when the value at that path equals {@code expected}. */
    JSON_PATH_EQUALS,

    /** {@code target} is a JSONPath; passes when the value at that path contains {@code expected}. */
    JSON_PATH_CONTAINS,

    /** {@code target} is a header name; passes when the response carries it. Case-insensitive. */
    HEADER_PRESENT,

    /** {@code target} is a header name; passes when its value equals {@code expected}. */
    HEADER_EQUALS,

    /** Passes when the raw response body contains {@code expected}. */
    BODY_CONTAINS
}
