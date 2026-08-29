package com.vivek.platform.apiorchestrator.exception;

/**
 * Thrown when a target URL is rejected by the outbound safety policy: an unsupported scheme, a
 * blocked host, or a private address while {@code app.security.allow-private-networks} is false.
 * Maps to 400.
 */
public class UnsafeUrlException extends RuntimeException {

    public UnsafeUrlException(String message) {
        super(message);
    }
}
