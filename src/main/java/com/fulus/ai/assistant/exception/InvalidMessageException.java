package com.fulus.ai.assistant.exception;

/**
 * Exception thrown when message validation fails
 */
public class InvalidMessageException extends RuntimeException {

    private final String field;
    private final String rejectedValue;

    public InvalidMessageException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
    }

    public InvalidMessageException(String message, String field, String rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public String getRejectedValue() {
        return rejectedValue;
    }
}

