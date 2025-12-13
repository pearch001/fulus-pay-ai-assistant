package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for querying transactions
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionQueryRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("The unique identifier of the user whose transactions to query")
    private String userId;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Filter by transaction category: BILL_PAYMENT, TRANSFER, SAVINGS, WITHDRAWAL, DEPOSIT, AIRTIME, DATA, SHOPPING, FOOD, TRANSPORT, ENTERTAINMENT, UTILITIES, HEALTHCARE, EDUCATION, OTHER. Leave null for all categories.")
    private String category;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Time period for the query. Examples: 'this month', 'last month', 'this week', 'last week', 'today', 'yesterday', 'last 7 days', 'last 30 days', 'last 3 months', 'this year'. Leave null for all time.")
    private String timePeriod;
}
