package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;

/**
 * Carries a completed-but-retryable response (a 5xx) through Reactor's retry operator.
 *
 * <p>A 5xx is not an error as far as the caller is concerned — it is a result worth reporting —
 * but it is worth retrying. Wrapping it in an exception lets {@code retryWhen} see it, and the
 * terminal error handler unwraps it again so the last response is returned rather than a
 * synthetic failure.
 */
class RetryableResponseException extends RuntimeException {

    private final transient ExecuteResponse response;

    RetryableResponseException(ExecuteResponse response) {
        super("Retryable status " + response.getStatus(), null, false, false);
        this.response = response;
    }

    ExecuteResponse getResponse() {
        return response;
    }
}
