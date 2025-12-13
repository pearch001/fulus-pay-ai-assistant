package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for transaction summary with category breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String period;

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;

    private Integer totalTransactions;
    private Integer incomeTransactions;
    private Integer expenseTransactions;

    private List<CategorySpendingDTO> categoryBreakdown;
    private CategorySpendingDTO topCategory;
}
