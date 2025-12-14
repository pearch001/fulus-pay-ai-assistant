package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.validation.SafeMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for Admin business insights chat interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(min = 2, max = 2000, message = "Message must be 2-2000 characters")
    @Pattern(regexp = "^[^<>]*$", message = "Message contains invalid characters")
    @SafeMessage
    private String message;

    /**
     * Optional conversation ID to continue existing conversation
     * If null, a new conversation will be created
     */
    private UUID conversationId;

    /**
     * Whether to include charts in the response (for future chart generation)
     * Default: false
     */
    @Builder.Default
    private Boolean includeCharts = false;
}
