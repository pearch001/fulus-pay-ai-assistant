package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for sending money to another user
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMoneyRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("The unique identifier (UUID) of the user sending the money")
    private String senderId;

    @JsonProperty(required = true)
    @JsonPropertyDescription("The recipient identifier - can be phone number (e.g., '08012345678') or full name (e.g., 'John Doe')")
    private String recipientIdentifier;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Amount to send in currency (e.g., 5000 for ₦5,000)")
    private Double amount;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Optional note or message to include with the transfer (e.g., 'Lunch money', 'Payment for rent')")
    private String note;
}
