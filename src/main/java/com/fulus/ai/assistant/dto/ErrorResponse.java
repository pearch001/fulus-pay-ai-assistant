package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response for API errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private boolean success;
    private String error;
    private String message;
    private String code;
    private LocalDateTime timestamp;
    private String path;
    private Integer status;

    public ErrorResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.error = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
        this.error = message;
        this.timestamp = LocalDateTime.now();
    }
}

