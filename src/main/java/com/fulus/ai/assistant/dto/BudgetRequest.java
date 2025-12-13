package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for creating a personalized budget
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("The unique identifier (UUID) of the user for whom to create the budget")
    private String userId;

    @JsonProperty(required = true)
    @JsonPropertyDescription("User's monthly income in currency (e.g., 150000 for ₦150,000 per month)")
    private Double monthlyIncome;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Optional JSON string with user preferences for budget allocation. " +
            "Supported preferences: " +
            "{'savingsGoal': 25, 'priorities': ['savings', 'bills', 'food']}, " +
            "{'style': 'aggressive_saver'} for 70/15/15 split, " +
            "{'style': 'balanced'} for 50/30/20 split (default), " +
            "{'style': 'flexible'} for 60/30/10 split")
    private String preferencesJson;
}
