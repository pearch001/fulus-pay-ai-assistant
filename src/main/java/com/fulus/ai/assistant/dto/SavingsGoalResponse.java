package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalResponse {

    private boolean success;
    private String message;
    private UUID savingsAccountId;
    private BigDecimal targetAmount;
    private BigDecimal monthlyContribution;
    private BigDecimal projectedFinalBalance;
    private BigDecimal projectedInterest;
    private double interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private int durationMonths;
    private String calculationExplanation;

    public static SavingsGoalResponse success(
            UUID savingsAccountId,
            BigDecimal targetAmount,
            BigDecimal monthlyContribution,
            BigDecimal projectedFinalBalance,
            BigDecimal projectedInterest,
            double interestRate,
            LocalDate startDate,
            LocalDate maturityDate,
            int durationMonths,
            String calculationExplanation) {

        return SavingsGoalResponse.builder()
                .success(true)
                .message("Savings goal created successfully")
                .savingsAccountId(savingsAccountId)
                .targetAmount(targetAmount)
                .monthlyContribution(monthlyContribution)
                .projectedFinalBalance(projectedFinalBalance)
                .projectedInterest(projectedInterest)
                .interestRate(interestRate)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .durationMonths(durationMonths)
                .calculationExplanation(calculationExplanation)
                .build();
    }

    public static SavingsGoalResponse failure(String message) {
        return SavingsGoalResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
