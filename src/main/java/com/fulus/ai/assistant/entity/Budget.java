package com.fulus.ai.assistant.entity;

import com.fulus.ai.assistant.converter.CategoryMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budget_user_id", columnList = "userId"),
    @Index(name = "idx_budget_month", columnList = "month"),
    @Index(name = "idx_budget_user_month", columnList = "userId, month", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 7)
    private String month; // Stored as "YYYY-MM" format

    @Convert(converter = CategoryMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, BigDecimal> categories = new HashMap<>();

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Helper methods to work with YearMonth
    public YearMonth getMonthAsYearMonth() {
        return YearMonth.parse(this.month);
    }

    public void setMonthFromYearMonth(YearMonth yearMonth) {
        this.month = yearMonth.toString();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
