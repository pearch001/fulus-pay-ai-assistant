package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PIN change/reset response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinChangeResponse {

    private boolean success;
    private String message;

    public static PinChangeResponse success(String message) {
        return PinChangeResponse.builder()
                .success(true)
                .message(message)
                .build();
    }
}
