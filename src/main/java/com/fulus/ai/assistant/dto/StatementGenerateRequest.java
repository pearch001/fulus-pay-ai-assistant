package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for generating account statements
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementGenerateRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("The unique identifier of the user whose statement to generate")
    private String userId;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Type of transactions to include: 'credit' for incoming money, 'debit' for outgoing money, 'all' for both types. Defaults to 'all'.")
    private String transactionType;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Time period for the statement. Examples: 'this month', 'last month', 'this week', 'last week', 'today', 'yesterday', 'last 7 days', 'last 30 days', 'last 3 months', 'this year'. Leave null for all time.")
    private String period;
}
