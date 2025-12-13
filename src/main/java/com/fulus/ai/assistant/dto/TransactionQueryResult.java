package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result of transaction query with formatted summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionQueryResult {

    private boolean success;
    private String message;
    private String userId;
    private String timePeriod;
    private Integer transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Map<String, CategorySummary> categoryBreakdown;
    private String formattedSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String category;
        private Integer count;
        private BigDecimal amount;
    }

    /**
     * Generate a human-readable formatted summary
     */
    public static String formatSummary(TransactionQueryResult result) {
        if (!result.success) {
            return result.message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Transaction Summary").append("\n");
        sb.append("==================").append("\n\n");

        sb.append(String.format("Period: %s%n", result.timePeriod != null ? result.timePeriod : "All time"));
        sb.append(String.format("Total Transactions: %d%n", result.transactionCount));
        sb.append(String.format("Total Income: ₦%.2f%n", result.totalIncome));
        sb.append(String.format("Total Expenses: ₦%.2f%n", result.totalExpenses));
        sb.append(String.format("Net Amount: ₦%.2f%n%n", result.totalAmount));

        if (result.categoryBreakdown != null && !result.categoryBreakdown.isEmpty()) {
            sb.append("Breakdown by Category:").append("\n");
            sb.append("----------------------").append("\n");

            result.categoryBreakdown.values().stream()
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .forEach(summary -> {
                        sb.append(String.format("  • %s: ₦%.2f (%d transactions)%n",
                                summary.getCategory(),
                                summary.getAmount(),
                                summary.getCount()));
                    });
        }

        return sb.toString();
    }

    public static TransactionQueryResult failure(String message) {
        return TransactionQueryResult.builder()
                .success(false)
                .message(message)
                .formattedSummary(message)
                .build();
    }
}
