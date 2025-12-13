package com.fulus.ai.assistant.exception;

public class ChatMemoryException extends RuntimeException {

    public ChatMemoryException(String message) {
        super(message);
    }

    public ChatMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
