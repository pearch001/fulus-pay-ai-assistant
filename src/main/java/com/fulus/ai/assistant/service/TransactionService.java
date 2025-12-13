package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.CategorySpendingDTO;
import com.fulus.ai.assistant.dto.TransactionDTO;
import com.fulus.ai.assistant.dto.TransactionPageResponse;
import com.fulus.ai.assistant.dto.TransactionSummaryDTO;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for transaction operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Get paginated and filtered transactions
     */
    public TransactionPageResponse getTransactions(
            UUID userId,
            int page,
            int size,
            TransactionCategory category,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.info("Fetching transactions for user: {}, page: {}, size: {}, category: {}, from: {}, to: {}",
                userId, page, size, category, fromDate, toDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findFilteredTransactions(
                userId, category, fromDate, toDate, pageable);

        List<TransactionDTO> transactionDTOs = transactionPage.getContent().stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());

        return TransactionPageResponse.builder()
                .transactions(transactionDTOs)
                .currentPage(transactionPage.getNumber())
                .totalPages(transactionPage.getTotalPages())
                .totalElements(transactionPage.getTotalElements())
                .pageSize(transactionPage.getSize())
                .hasNext(transactionPage.hasNext())
                .hasPrevious(transactionPage.hasPrevious())
                .build();
    }

    /**
     * Get transaction summary for a period
     */
    public TransactionSummaryDTO getTransactionSummary(UUID userId, String period, LocalDateTime customFrom, LocalDateTime customTo) {
        log.info("Generating transaction summary for user: {}, period: {}", userId, period);

        // Determine date range based on period
        LocalDateTime startDate;
        LocalDateTime endDate;
        String periodLabel;

        if ("custom".equalsIgnoreCase(period) && customFrom != null && customTo != null) {
            startDate = customFrom;
            endDate = customTo;
            periodLabel = "Custom Period";
        } else {
            LocalDateTime[] dateRange = getPeriodDateRange(period);
            startDate = dateRange[0];
            endDate = dateRange[1];
            periodLabel = formatPeriodLabel(period);
        }

        // Get income and expense totals
        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                userId, TransactionType.CREDIT, startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                userId, TransactionType.DEBIT, startDate, endDate);

        // Handle null values
        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;

        BigDecimal netAmount = totalIncome.subtract(totalExpenses);

        // Get transaction counts
        Long incomeCount = transactionRepository.countByUserAndTypeAndDateRange(
                userId, TransactionType.CREDIT, startDate, endDate);
        Long expenseCount = transactionRepository.countByUserAndTypeAndDateRange(
                userId, TransactionType.DEBIT, startDate, endDate);

        incomeCount = incomeCount != null ? incomeCount : 0L;
        expenseCount = expenseCount != null ? expenseCount : 0L;

        // Get category breakdown with counts
        List<Object[]> categoryData = transactionRepository.findCategorySpendingWithCount(
                userId, startDate, endDate);

        List<CategorySpendingDTO> categoryBreakdown = new ArrayList<>();
        CategorySpendingDTO topCategory = null;
        BigDecimal maxAmount = BigDecimal.ZERO;

        for (Object[] data : categoryData) {
            TransactionCategory category = (TransactionCategory) data[0];
            BigDecimal amount = (BigDecimal) data[1];
            Long count = (Long) data[2];

            // Calculate percentage
            double percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            CategorySpendingDTO categoryDTO = CategorySpendingDTO.builder()
                    .category(category)
                    .amount(amount)
                    .transactionCount(count.intValue())
                    .percentage(percentage)
                    .build();

            categoryBreakdown.add(categoryDTO);

            // Track top category
            if (amount.compareTo(maxAmount) > 0) {
                maxAmount = amount;
                topCategory = categoryDTO;
            }
        }

        return TransactionSummaryDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period(periodLabel)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netAmount(netAmount)
                .totalTransactions((int) (incomeCount + expenseCount))
                .incomeTransactions(incomeCount.intValue())
                .expenseTransactions(expenseCount.intValue())
                .categoryBreakdown(categoryBreakdown)
                .topCategory(topCategory)
                .build();
    }

    /**
     * Get date range for period
     */
    private LocalDateTime[] getPeriodDateRange(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate = now;

        switch (period.toLowerCase()) {
            case "this_month":
                YearMonth thisMonth = YearMonth.now();
                startDate = thisMonth.atDay(1).atStartOfDay();
                endDate = thisMonth.atEndOfMonth().atTime(23, 59, 59);
                break;

            case "last_month":
                YearMonth lastMonth = YearMonth.now().minusMonths(1);
                startDate = lastMonth.atDay(1).atStartOfDay();
                endDate = lastMonth.atEndOfMonth().atTime(23, 59, 59);
                break;

            case "last_3_months":
                startDate = now.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                break;

            case "last_6_months":
                startDate = now.minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                break;

            case "this_year":
                startDate = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
                break;

            case "last_year":
                startDate = now.minusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
                endDate = now.minusYears(1).withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59);
                break;

            case "all_time":
                startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
                break;

            default:
                // Default to this month
                YearMonth currentMonth = YearMonth.now();
                startDate = currentMonth.atDay(1).atStartOfDay();
                endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * Format period label for display
     */
    private String formatPeriodLabel(String period) {
        switch (period.toLowerCase()) {
            case "this_month":
                return "This Month";
            case "last_month":
                return "Last Month";
            case "last_3_months":
                return "Last 3 Months";
            case "last_6_months":
                return "Last 6 Months";
            case "this_year":
                return "This Year";
            case "last_year":
                return "Last Year";
            case "all_time":
                return "All Time";
            default:
                return "This Month";
        }
    }
}
