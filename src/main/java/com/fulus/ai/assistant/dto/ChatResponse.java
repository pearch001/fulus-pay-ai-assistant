package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for AI chat interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Whether the request was successful
     */
    private boolean success;

    /**
     * Status message (e.g., "Response generated successfully" or error description)
     */
    private String message;

    /**
     * The AI assistant's response
     */
    private String response;

    /**
     * The conversation ID (typically the user ID)
     */
    private String conversationId;

    /**
     * Number of messages in the conversation history
     */
    private Integer messageCount;

    /**
     * Timestamp when response was generated
     */
    private LocalDateTime timestamp;

    /**
     * Optional: Conversation history for debugging
     */
    private List<ConversationMessage> history;

    /**
     * Nested class for conversation history
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private String role; // "user" or "assistant" or "system"
        private String content;
        private LocalDateTime timestamp;
    }

    /**
     * Factory method for success response
     */
    public static ChatResponse success(String response, String conversationId, Integer messageCount) {
        return ChatResponse.builder()
                .success(true)
                .message("Response generated successfully")
                .response(response)
                .conversationId(conversationId)
                .messageCount(messageCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for failure response
     */
    public static ChatResponse failure(String errorMessage) {
        return ChatResponse.builder()
                .success(false)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
