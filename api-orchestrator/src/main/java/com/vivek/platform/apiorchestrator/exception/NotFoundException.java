package com.vivek.platform.apiorchestrator.exception;

/** Thrown when a collection, saved request or environment does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
