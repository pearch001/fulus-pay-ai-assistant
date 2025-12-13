package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsCalculationResult {

    private BigDecimal principal;
    private BigDecimal monthlyContribution;
    private BigDecimal totalContributions;
    private BigDecimal totalInterestEarned;
    private BigDecimal finalBalance;
    private double annualInterestRate;
    private int months;
    private String explanation;

    /**
     * Generate a formatted explanation of the calculation
     */
    public static String generateExplanation(
            BigDecimal principal,
            BigDecimal monthlyContribution,
            BigDecimal totalContributions,
            BigDecimal totalInterestEarned,
            BigDecimal finalBalance,
            double annualInterestRate,
            int months) {

        StringBuilder sb = new StringBuilder();
        sb.append("Savings Calculation Summary:\n\n");
        sb.append(String.format("Initial Principal: ₦%.2f\n", principal));
        sb.append(String.format("Monthly Contribution: ₦%.2f\n", monthlyContribution));
        sb.append(String.format("Duration: %d months (%.1f years)\n", months, months / 12.0));
        sb.append(String.format("Annual Interest Rate: %.2f%%\n\n", annualInterestRate));

        sb.append(String.format("Total Contributions: ₦%.2f\n", totalContributions));
        sb.append(String.format("Total Interest Earned: ₦%.2f\n", totalInterestEarned));
        sb.append(String.format("Final Balance: ₦%.2f\n\n", finalBalance));

        sb.append("Formula: Compound interest calculated monthly with regular contributions.\n");
        sb.append("Each contribution earns interest from the time it's deposited.");

        return sb.toString();
    }
}
