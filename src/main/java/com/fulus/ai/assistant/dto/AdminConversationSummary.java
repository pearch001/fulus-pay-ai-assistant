package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary DTO for Admin conversation
 * Used for listing conversations and overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversationSummary {

    /**
     * Unique conversation identifier
     */
    @NotNull
    private UUID conversationId;

    /**
     * Total number of messages in the conversation
     */
    private Integer messageCount;

    /**
     * Timestamp of the last message in the conversation
     */
    private LocalDateTime lastMessageAt;

    /**
     * Optional subject or topic of the conversation
     */
    private String subject;

    /**
     * Whether the conversation is currently active
     */
    private Boolean isActive;

    /**
     * Total tokens used in this conversation
     */
    private Long totalTokens;

    /**
     * When the conversation was created
     */
    private LocalDateTime createdAt;

    /**
     * When the conversation was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * Preview of the first or last message (optional)
     */
    private String messagePreview;
}

