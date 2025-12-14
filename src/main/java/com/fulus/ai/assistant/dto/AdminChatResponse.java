package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Admin business insights chat interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatResponse {

    /**
     * The AI assistant's response message
     */
    private String message;

    /**
     * The conversation ID for this chat session
     */
    private UUID conversationId;

    /**
     * Timestamp when the response was generated
     */
    private LocalDateTime timestamp;

    /**
     * Time taken to process and generate the response (in milliseconds)
     */
    private Long processingTimeMs;

    /**
     * Optional charts data for visualization (for future use)
     */
    private List<ChartData> charts;

    /**
     * Sequence number of this message in the conversation
     */
    private Integer sequenceNumber;

    /**
     * Estimated token count for this interaction
     */
    private Integer tokenCount;
}

