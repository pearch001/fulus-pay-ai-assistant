package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.TransactionQueryRequest;
import com.fulus.ai.assistant.dto.TransactionQueryResult;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.util.TimePeriodParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring AI Function Tool for querying user transactions
 * This function is automatically discoverable by Spring AI's function calling mechanism
 */
@Component("transactionQueryFunction")
@Description("Query and analyze user transactions. Returns a detailed summary including total amount, transaction count, income vs expenses, and breakdown by category. Use this when users ask about their spending, transactions, or financial activity.")
@RequiredArgsConstructor
@Slf4j
public class TransactionQueryFunction implements Function<TransactionQueryRequest, String> {

    private final TransactionRepository transactionRepository;

    /**
     * Query transactions based on user ID, category, and time period
     *
     * @param request Request containing userId, category filter, and time period
     * @return Formatted transaction summary as a String
     */
    @Override
    public String apply(TransactionQueryRequest request) {
        log.info("Executing transaction query: userId={}, category={}, timePeriod={}",
                request.getUserId(), request.getCategory(), request.getTimePeriod());

        try {
            // Validate user ID
            UUID userId = UUID.fromString(request.getUserId());

            // Parse time period
            TimePeriodParser.DateRange dateRange = TimePeriodParser.parse(request.getTimePeriod());
            log.debug("Parsed date range: {} to {}", dateRange.getStart(), dateRange.getEnd());

            // Query transactions
            List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
                    userId,
                    dateRange.getStart(),
                    dateRange.getEnd()
            );

            // Filter by category if specified
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                try {
                    TransactionCategory categoryFilter = TransactionCategory.valueOf(
                            request.getCategory().toUpperCase()
                    );
                    transactions = transactions.stream()
                            .filter(t -> t.getCategory() == categoryFilter)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category filter: {}", request.getCategory());
                    return TransactionQueryResult.failure(
                            "Invalid category: " + request.getCategory()
                    ).getFormattedSummary();
                }
            }

            // Build result
            TransactionQueryResult result = buildResult(
                    userId.toString(),
                    transactions,
                    dateRange.getDescription()
            );

            return result.getFormattedSummary();

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID: {}", request.getUserId());
            return TransactionQueryResult.failure("Invalid user ID format").getFormattedSummary();
        } catch (Exception e) {
            log.error("Error querying transactions", e);
            return TransactionQueryResult.failure(
                    "Error querying transactions: " + e.getMessage()
            ).getFormattedSummary();
        }
    }

    /**
     * Build comprehensive transaction result with analytics
     */
    private TransactionQueryResult buildResult(String userId, List<Transaction> transactions, String timePeriod) {
        if (transactions.isEmpty()) {
            TransactionQueryResult result = TransactionQueryResult.builder()
                    .success(true)
                    .message("No transactions found")
                    .userId(userId)
                    .timePeriod(timePeriod)
                    .transactionCount(0)
                    .totalAmount(BigDecimal.ZERO)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO)
                    .categoryBreakdown(new HashMap<>())
                    .build();

            result.setFormattedSummary(TransactionQueryResult.formatSummary(result));
            return result;
        }

        // Calculate totals
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = totalIncome.subtract(totalExpenses);

        // Group by category
        Map<String, TransactionQueryResult.CategorySummary> categoryBreakdown = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().name(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> TransactionQueryResult.CategorySummary.builder()
                                        .category(list.get(0).getCategory().name())
                                        .count(list.size())
                                        .amount(list.stream()
                                                .map(Transaction::getAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                                        .build()
                        )
                ));

        TransactionQueryResult result = TransactionQueryResult.builder()
                .success(true)
                .message("Query successful")
                .userId(userId)
                .timePeriod(timePeriod)
                .transactionCount(transactions.size())
                .totalAmount(netAmount)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .categoryBreakdown(categoryBreakdown)
                .build();

        // Generate formatted summary
        result.setFormattedSummary(TransactionQueryResult.formatSummary(result));

        return result;
    }
}
