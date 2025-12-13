package com.fulus.ai.assistant.exception;

public class OpenAIServiceException extends RuntimeException {

    public OpenAIServiceException(String message) {
        super(message);
    }

    public OpenAIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
