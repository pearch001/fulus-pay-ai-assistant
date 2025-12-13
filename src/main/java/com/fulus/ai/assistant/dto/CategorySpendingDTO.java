package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for category-wise spending breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpendingDTO {

    private TransactionCategory category;
    private BigDecimal amount;
    private Integer transactionCount;
    private Double percentage;

    public CategorySpendingDTO(TransactionCategory category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}
