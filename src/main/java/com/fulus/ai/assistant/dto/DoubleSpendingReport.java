package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for double-spending detection report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleSpendingReport {

    private String userId;
    private BigDecimal lastKnownBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal projectedBalance;
    private boolean hasDoubleSpending;
    private Integer totalTransactions;
    private Integer flaggedTransactions;

    @Builder.Default
    private List<FlaggedTransaction> flaggedTransactionList = new ArrayList<>();

    /**
     * Add flagged transaction
     */
    public void addFlaggedTransaction(FlaggedTransaction transaction) {
        this.flaggedTransactionList.add(transaction);
        this.hasDoubleSpending = true;
        this.flaggedTransactions = this.flaggedTransactionList.size();
    }

    /**
     * Calculate projected balance
     */
    public void calculateProjectedBalance() {
        this.projectedBalance = lastKnownBalance
                .add(totalCredits != null ? totalCredits : BigDecimal.ZERO)
                .subtract(totalDebits != null ? totalDebits : BigDecimal.ZERO);
    }

    /**
     * Factory method for no double-spending
     */
    public static DoubleSpendingReport clean(String userId, BigDecimal balance, BigDecimal debits, BigDecimal credits, int txCount) {
        DoubleSpendingReport report = DoubleSpendingReport.builder()
                .userId(userId)
                .lastKnownBalance(balance)
                .totalDebits(debits)
                .totalCredits(credits)
                .hasDoubleSpending(false)
                .totalTransactions(txCount)
                .flaggedTransactions(0)
                .flaggedTransactionList(new ArrayList<>())
                .build();
        report.calculateProjectedBalance();
        return report;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlaggedTransaction {
        private UUID transactionId;
        private String transactionHash;
        private BigDecimal amount;
        private BigDecimal balanceBeforeTransaction;
        private BigDecimal balanceAfterTransaction;
        private String reason;
        private Integer transactionIndex;
    }
}
