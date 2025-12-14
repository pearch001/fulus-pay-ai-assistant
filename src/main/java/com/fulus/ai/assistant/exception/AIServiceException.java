package com.fulus.ai.assistant.exception;

/**
 * Exception thrown when AI service fails to generate a response
 */
public class AIServiceException extends RuntimeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
