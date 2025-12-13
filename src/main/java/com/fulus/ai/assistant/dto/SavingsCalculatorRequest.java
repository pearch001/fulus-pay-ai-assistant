package com.fulus.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for calculating savings projections
 * Used by Spring AI function calling mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsCalculatorRequest {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Monthly savings amount in currency (e.g., 10000 for ₦10,000 per month)")
    private Double monthlyAmount;

    @JsonProperty(required = true)
    @JsonPropertyDescription("Number of months to save for (e.g., 12 for 1 year, 24 for 2 years)")
    private Integer months;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Annual interest rate as a percentage (e.g., 5.0 for 5% per year). Defaults to 5.0 if not provided. This is the yearly interest rate that will be compounded monthly.")
    private Double interestRate;
}
