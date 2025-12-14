package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI chat interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {


    @NotBlank(message = "Message cannot be empty")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;

    /**
     * Whether to use conversation memory (default: true)
     */
    @Builder.Default
    private Boolean useMemory = true;

    /**
     * Optional: Include conversation history in response for debugging
     */
    private Boolean includeHistory;
}
