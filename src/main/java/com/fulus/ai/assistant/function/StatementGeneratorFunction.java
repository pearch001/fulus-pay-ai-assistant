package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.StatementGenerateRequest;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.util.TimePeriodParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring AI Function Tool for generating account statements
 * This function is automatically discoverable by Spring AI's function calling mechanism
 */
@Component("statementGeneratorFunction")
@Description("Generate a detailed account statement for a user showing all transactions in a formatted table with date, description, amount, and running balance. Includes summary with opening balance, total credits, total debits, and closing balance. Use this when users request their account statement, transaction history, or want to see all their transactions.")
@RequiredArgsConstructor
@Slf4j
public class StatementGeneratorFunction implements Function<StatementGenerateRequest, String> {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String CURRENCY_SYMBOL = "₦";

    /**
     * Generate account statement with transactions formatted as a table
     *
     * @param request Request containing userId, transactionType, and period
     * @return Formatted statement as a String
     */
    @Override
    public String apply(StatementGenerateRequest request) {
        log.info("Generating statement: userId={}, type={}, period={}",
                request.getUserId(), request.getTransactionType(), request.getPeriod());

        try {
            // Validate and get user
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

            // Parse time period
            TimePeriodParser.DateRange dateRange = TimePeriodParser.parse(request.getPeriod());
            log.debug("Statement date range: {} to {}", dateRange.getStart(), dateRange.getEnd());

            // Query transactions
            List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
                    userId,
                    dateRange.getStart(),
                    dateRange.getEnd()
            );

            // Filter by transaction type if specified
            String transactionType = request.getTransactionType() != null ?
                    request.getTransactionType().toLowerCase().trim() : "all";

            if (!transactionType.equals("all")) {
                TransactionType typeFilter = parseTransactionType(transactionType);
                transactions = transactions.stream()
                        .filter(t -> t.getType() == typeFilter)
                        .collect(Collectors.toList());
            }

            // Sort by date ascending
            transactions.sort(Comparator.comparing(Transaction::getCreatedAt));

            // Generate statement
            return formatStatement(user, transactions, dateRange.getDescription(), transactionType);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            log.error("Error generating statement", e);
            return "Error generating statement: " + e.getMessage();
        }
    }

    /**
     * Format statement as a text table with summary
     */
    private String formatStatement(User user, List<Transaction> transactions, String period, String type) {
        StringBuilder statement = new StringBuilder();

        // Header
        statement.append("═".repeat(90)).append("\n");
        statement.append("                         ACCOUNT STATEMENT\n");
        statement.append("═".repeat(90)).append("\n\n");

        statement.append(String.format("Account Holder: %s%n", user.getName()));
        statement.append(String.format("Account ID: %s%n", user.getId()));
        statement.append(String.format("Period: %s%n", period));
        statement.append(String.format("Transaction Type: %s%n", type.toUpperCase()));
        statement.append(String.format("Generated: %s%n%n",
                java.time.LocalDateTime.now().format(DATE_FORMATTER)));

        if (transactions.isEmpty()) {
            statement.append("No transactions found for the specified period.\n\n");
            statement.append(String.format("Current Balance: %s%.2f%n", CURRENCY_SYMBOL, user.getBalance()));
            statement.append("═".repeat(90)).append("\n");
            return statement.toString();
        }

        // Calculate opening balance from first transaction
        Transaction firstTransaction = transactions.get(0);
        BigDecimal openingBalance;
        if (firstTransaction.getType() == TransactionType.DEBIT) {
            openingBalance = firstTransaction.getBalanceAfter().add(firstTransaction.getAmount());
        } else {
            openingBalance = firstTransaction.getBalanceAfter().subtract(firstTransaction.getAmount());
        }

        statement.append(String.format("Opening Balance: %s%.2f%n%n", CURRENCY_SYMBOL, openingBalance));

        // Table header
        statement.append("─".repeat(90)).append("\n");
        statement.append(String.format("%-20s | %-30s | %-12s | %-15s%n",
                "Date", "Description", "Amount", "Balance"));
        statement.append("─".repeat(90)).append("\n");

        // Calculate totals
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        // Transaction rows
        for (Transaction txn : transactions) {
            String date = txn.getCreatedAt().format(DATE_FORMATTER);
            String description = truncateString(txn.getDescription(), 30);
            String amount = formatAmount(txn);
            String balance = String.format("%s%.2f", CURRENCY_SYMBOL, txn.getBalanceAfter());

            statement.append(String.format("%-20s | %-30s | %-12s | %-15s%n",
                    date, description, amount, balance));

            // Update totals
            if (txn.getType() == TransactionType.CREDIT) {
                totalCredits = totalCredits.add(txn.getAmount());
            } else {
                totalDebits = totalDebits.add(txn.getAmount());
            }
        }

        statement.append("─".repeat(90)).append("\n\n");

        // Summary
        Transaction lastTransaction = transactions.get(transactions.size() - 1);
        BigDecimal closingBalance = lastTransaction.getBalanceAfter();

        statement.append("SUMMARY\n");
        statement.append("─".repeat(50)).append("\n");
        statement.append(String.format("Opening Balance:    %s%.2f%n", CURRENCY_SYMBOL, openingBalance));
        statement.append(String.format("Total Credits:      %s%.2f (+)%n", CURRENCY_SYMBOL, totalCredits));
        statement.append(String.format("Total Debits:       %s%.2f (-)%n", CURRENCY_SYMBOL, totalDebits));
        statement.append("─".repeat(50)).append("\n");
        statement.append(String.format("Closing Balance:    %s%.2f%n", CURRENCY_SYMBOL, closingBalance));
        statement.append(String.format("Current Balance:    %s%.2f%n", CURRENCY_SYMBOL, user.getBalance()));
        statement.append("\n");
        statement.append(String.format("Total Transactions: %d%n", transactions.size()));

        statement.append("\n");
        statement.append("═".repeat(90)).append("\n");

        return statement.toString();
    }

    /**
     * Format transaction amount with +/- prefix
     */
    private String formatAmount(Transaction txn) {
        String prefix = txn.getType() == TransactionType.CREDIT ? "+" : "-";
        return String.format("%s%s%.2f", prefix, CURRENCY_SYMBOL, txn.getAmount());
    }

    /**
     * Truncate string to specified length with ellipsis
     */
    private String truncateString(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Parse transaction type from string
     */
    private TransactionType parseTransactionType(String type) {
        return switch (type.toLowerCase()) {
            case "credit" -> TransactionType.CREDIT;
            case "debit" -> TransactionType.DEBIT;
            default -> throw new IllegalArgumentException(
                    "Invalid transaction type: " + type + ". Must be 'credit', 'debit', or 'all'");
        };
    }
}
