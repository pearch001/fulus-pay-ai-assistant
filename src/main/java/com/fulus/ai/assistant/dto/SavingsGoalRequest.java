package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalRequest {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be greater than 0")
    private Double targetAmount;

    @NotNull(message = "Monthly contribution is required")
    @Positive(message = "Monthly contribution must be greater than 0")
    private Double monthlyContribution;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 600, message = "Duration cannot exceed 600 months (50 years)")
    private Integer durationMonths;

    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    private Double annualInterestRate; // Optional, defaults to 5%
}
