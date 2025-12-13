package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardActionResponse {

    private boolean success;
    private String message;
    private Object data;

    public static CardActionResponse success(String message) {
        return CardActionResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static CardActionResponse success(String message, Object data) {
        return CardActionResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static CardActionResponse failure(String message) {
        return CardActionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
