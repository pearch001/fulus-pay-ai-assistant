package com.fulus.ai.assistant.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulus.ai.assistant.dto.BudgetRequest;
import com.fulus.ai.assistant.entity.Budget;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.BudgetRepository;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring AI Function Tool for creating personalized budgets based on spending analysis
 */
@Component("budgetAssistantFunction")
@Description("Create a personalized monthly budget based on user's income and spending patterns. " +
        "Analyzes last 3 months of transaction history, applies 50/30/20 budgeting rule adapted for Nigerian context, " +
        "and provides personalized recommendations. Returns formatted budget breakdown with category allocations.")
@RequiredArgsConstructor
@Slf4j
public class BudgetAssistantFunction implements Function<BudgetRequest, String> {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CURRENCY_SYMBOL = "₦";

    // Budget style configurations
    private static final Map<String, BudgetSplit> BUDGET_STYLES = Map.of(
            "balanced", new BudgetSplit(50, 30, 20),
            "aggressive_saver", new BudgetSplit(70, 15, 15),
            "flexible", new BudgetSplit(60, 30, 10)
    );

    /**
     * Create personalized budget with spending analysis
     *
     * @param request Request containing userId, monthlyIncome, and optional preferences
     * @return Formatted budget breakdown with recommendations
     */
    @Override
    public String apply(BudgetRequest request) {
        log.info("Creating budget for user: {}, income: {}", request.getUserId(), request.getMonthlyIncome());

        try {
            // Validate inputs
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return "❌ ERROR: User ID is required to create a budget.";
            }

            if (request.getMonthlyIncome() == null || request.getMonthlyIncome() <= 0) {
                return "❌ ERROR: Monthly income must be greater than zero.";
            }

            if (request.getMonthlyIncome() < 10000) {
                return "❌ ERROR: Monthly income seems too low. Please provide your actual monthly income.";
            }

            // Get user
            UUID userId;
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                return "❌ ERROR: Invalid user ID format.";
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return "❌ ERROR: User not found.";
            }
            User user = userOpt.get();

            // Parse preferences
            BudgetPreferences preferences = parsePreferences(request.getPreferencesJson());

            // Analyze spending for last 3 months
            SpendingAnalysis analysis = analyzeSpending(userId);

            // Calculate budget allocation
            BudgetAllocation allocation = calculateBudget(
                    request.getMonthlyIncome(),
                    preferences,
                    analysis
            );

            // Save budget to database
            Budget savedBudget = saveBudget(userId, allocation);

            // Generate recommendations
            List<String> recommendations = generateRecommendations(
                    analysis,
                    allocation,
                    request.getMonthlyIncome()
            );

            // Format and return response
            return formatBudgetResponse(
                    user,
                    request.getMonthlyIncome(),
                    allocation,
                    analysis,
                    recommendations,
                    savedBudget
            );

        } catch (Exception e) {
            log.error("Error creating budget", e);
            return "❌ BUDGET CREATION FAILED: An unexpected error occurred. " +
                    "Please try again or contact support.";
        }
    }

    /**
     * Parse user preferences from JSON string
     */
    private BudgetPreferences parsePreferences(String preferencesJson) {
        BudgetPreferences prefs = new BudgetPreferences();

        if (preferencesJson == null || preferencesJson.trim().isEmpty()) {
            return prefs; // Return defaults
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(preferencesJson);

            // Parse style
            if (jsonNode.has("style")) {
                String style = jsonNode.get("style").asText().toLowerCase();
                prefs.style = BUDGET_STYLES.getOrDefault(style, BUDGET_STYLES.get("balanced"));
            }

            // Parse custom savings goal
            if (jsonNode.has("savingsGoal")) {
                int savingsGoal = jsonNode.get("savingsGoal").asInt();
                if (savingsGoal >= 10 && savingsGoal <= 50) {
                    prefs.customSavingsPercentage = savingsGoal;
                }
            }

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse preferences JSON, using defaults: {}", e.getMessage());
        }

        return prefs;
    }

    /**
     * Analyze spending for last 3 months
     */
    private SpendingAnalysis analyzeSpending(UUID userId) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndCreatedAtBetween(userId, threeMonthsAgo, now);

        SpendingAnalysis analysis = new SpendingAnalysis();

        // Filter only debit transactions
        List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .collect(Collectors.toList());

        // Group by category and sum
        Map<TransactionCategory, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())
                ));

        analysis.categorySpending = categoryTotals;
        analysis.totalSpent = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
        analysis.averageMonthlySpending = analysis.totalSpent / 3.0;
        analysis.transactionCount = expenses.size();

        // Calculate category percentages
        if (analysis.totalSpent > 0) {
            analysis.categoryPercentages = categoryTotals.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> (e.getValue() / analysis.totalSpent) * 100
                    ));
        }

        return analysis;
    }

    /**
     * Calculate budget allocation based on income, preferences, and spending analysis
     */
    private BudgetAllocation calculateBudget(
            double monthlyIncome,
            BudgetPreferences preferences,
            SpendingAnalysis analysis) {

        BudgetSplit split = preferences.style;

        // Apply custom savings percentage if specified
        if (preferences.customSavingsPercentage > 0) {
            int savings = preferences.customSavingsPercentage;
            int remaining = 100 - savings;
            split = new BudgetSplit(
                    (int) (remaining * 0.6), // 60% of remaining for needs
                    (int) (remaining * 0.4), // 40% of remaining for wants
                    savings
            );
        }

        BudgetAllocation allocation = new BudgetAllocation();
        allocation.totalIncome = monthlyIncome;

        // Calculate main categories
        allocation.needsAmount = (monthlyIncome * split.needs) / 100.0;
        allocation.wantsAmount = (monthlyIncome * split.wants) / 100.0;
        allocation.savingsAmount = (monthlyIncome * split.savings) / 100.0;

        allocation.needsPercentage = split.needs;
        allocation.wantsPercentage = split.wants;
        allocation.savingsPercentage = split.savings;

        // Allocate specific categories based on analysis
        allocation.categoryAllocations = allocateCategories(
                allocation.needsAmount,
                allocation.wantsAmount,
                analysis
        );

        return allocation;
    }

    /**
     * Allocate budget to specific categories
     */
    private Map<TransactionCategory, Double> allocateCategories(
            double needsAmount,
            double wantsAmount,
            SpendingAnalysis analysis) {

        Map<TransactionCategory, Double> allocations = new HashMap<>();

        // Needs categories (Nigerian context)
        Set<TransactionCategory> needsCategories = Set.of(
                TransactionCategory.BILL_PAYMENT,
                TransactionCategory.FOOD,
                TransactionCategory.TRANSPORT,
                TransactionCategory.UTILITIES,
                TransactionCategory.HEALTHCARE
        );

        // Wants categories
        Set<TransactionCategory> wantsCategories = Set.of(
                TransactionCategory.ENTERTAINMENT,
                TransactionCategory.SHOPPING
        );

        // Allocate needs based on historical spending ratios
        allocateByCategory(needsAmount, needsCategories, analysis, allocations);

        // Allocate wants based on historical spending ratios
        allocateByCategory(wantsAmount, wantsCategories, analysis, allocations);

        return allocations;
    }

    /**
     * Helper to allocate amount across categories based on historical ratios
     */
    private void allocateByCategory(
            double totalAmount,
            Set<TransactionCategory> categories,
            SpendingAnalysis analysis,
            Map<TransactionCategory, Double> allocations) {

        // Calculate total historical spending for these categories
        double totalHistorical = categories.stream()
                .mapToDouble(cat -> analysis.categorySpending.getOrDefault(cat, 0.0))
                .sum();

        if (totalHistorical > 0) {
            // Allocate proportionally based on historical spending
            for (TransactionCategory category : categories) {
                double historicalAmount = analysis.categorySpending.getOrDefault(category, 0.0);
                double ratio = historicalAmount / totalHistorical;
                allocations.put(category, totalAmount * ratio);
            }
        } else {
            // No historical data, distribute evenly
            double amountPerCategory = totalAmount / categories.size();
            for (TransactionCategory category : categories) {
                allocations.put(category, amountPerCategory);
            }
        }
    }

    /**
     * Generate personalized recommendations
     */
    private List<String> generateRecommendations(
            SpendingAnalysis analysis,
            BudgetAllocation allocation,
            double monthlyIncome) {

        List<String> recommendations = new ArrayList<>();

        // Compare average spending to income
        if (analysis.averageMonthlySpending > monthlyIncome * 0.95) {
            recommendations.add("⚠️ Your average spending (₦" + String.format("%,.0f", analysis.averageMonthlySpending) +
                    ") is very close to or exceeds your income. Focus on reducing discretionary expenses.");
        }

        // Check if user is saving
        double currentSavingsRate = 0;
        if (analysis.totalSpent > 0 && monthlyIncome > analysis.averageMonthlySpending) {
            currentSavingsRate = ((monthlyIncome - analysis.averageMonthlySpending) / monthlyIncome) * 100;
        }

        if (currentSavingsRate < 10) {
            recommendations.add("💡 Try to save at least " + allocation.savingsPercentage +
                    "% of your income (₦" + String.format("%,.0f", allocation.savingsAmount) +
                    "). Start with automatic transfers to a savings account.");
        } else if (currentSavingsRate >= 20) {
            recommendations.add("🌟 Excellent! You're already saving well. Consider investing some savings for better returns.");
        }

        // Category-specific recommendations
        for (Map.Entry<TransactionCategory, Double> entry : analysis.categorySpending.entrySet()) {
            TransactionCategory category = entry.getKey();
            double spent = entry.getValue() / 3.0; // Average per month
            double allocated = allocation.categoryAllocations.getOrDefault(category, 0.0);

            if (spent > allocated * 1.2) { // Spending 20% more than allocated
                recommendations.add("💰 Consider reducing your " + category.name().toLowerCase().replace("_", " ") +
                        " expenses. You're currently spending ₦" + String.format("%,.0f", spent) +
                        " but should aim for ₦" + String.format("%,.0f", allocated) + ".");
            }
        }

        // Nigerian-specific recommendations
        if (allocation.categoryAllocations.getOrDefault(TransactionCategory.BILL_PAYMENT, 0.0) > monthlyIncome * 0.25) {
            recommendations.add("📊 Bills take up a large portion of your budget. Look for ways to reduce utility costs " +
                    "(e.g., energy-efficient appliances, prepaid plans).");
        }

        if (allocation.categoryAllocations.getOrDefault(TransactionCategory.TRANSPORT, 0.0) > monthlyIncome * 0.15) {
            recommendations.add("🚗 Transportation costs are high. Consider carpooling, using public transport, " +
                    "or planning trips to reduce fuel expenses.");
        }

        // Add general advice
        recommendations.add("✅ Review your budget weekly and adjust as needed.");
        recommendations.add("📱 Track all expenses using Fulus Pay to stay within your budget.");

        return recommendations;
    }

    /**
     * Save budget to database
     */
    private Budget saveBudget(UUID userId, BudgetAllocation allocation) {
        YearMonth currentMonth = YearMonth.now();

        // Check if budget already exists for this month
        Optional<Budget> existingBudget = budgetRepository
                .findByUserIdAndMonth(userId, currentMonth.toString());

        Budget budget;
        if (existingBudget.isPresent()) {
            budget = existingBudget.get();
            log.info("Updating existing budget for user: {}, month: {}", userId, currentMonth);
        } else {
            budget = new Budget();
            budget.setUserId(userId);
            budget.setMonth(currentMonth.toString());
        }

        // Convert allocations to map for JSON storage
        Map<String, BigDecimal> categoryMap = allocation.categoryAllocations.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> BigDecimal.valueOf(e.getValue())
                ));

        // Add main categories
        categoryMap.put("SAVINGS", BigDecimal.valueOf(allocation.savingsAmount));
        categoryMap.put("TOTAL_NEEDS", BigDecimal.valueOf(allocation.needsAmount));
        categoryMap.put("TOTAL_WANTS", BigDecimal.valueOf(allocation.wantsAmount));

        budget.setCategories(categoryMap);
        budget.setUpdatedAt(LocalDateTime.now());

        return budgetRepository.save(budget);
    }

    /**
     * Format budget response
     */
    private String formatBudgetResponse(
            User user,
            double monthlyIncome,
            BudgetAllocation allocation,
            SpendingAnalysis analysis,
            List<String> recommendations,
            Budget savedBudget) {

        StringBuilder response = new StringBuilder();

        response.append("💰 PERSONALIZED BUDGET PLAN\n");
        response.append("══════════════════════════════════════════════════════════\n\n");

        response.append(String.format("Hi %s! Here's your budget for %s\n",
                user.getName(), YearMonth.now().toString()));
        response.append(String.format("Monthly Income: %s%,.2f\n\n", CURRENCY_SYMBOL, monthlyIncome));

        // Main budget breakdown
        response.append("📊 BUDGET BREAKDOWN (50/30/20 Rule)\n");
        response.append("──────────────────────────────────────────────────────────\n");
        response.append(String.format("Needs (%d%%):        %s%,.2f\n",
                allocation.needsPercentage, CURRENCY_SYMBOL, allocation.needsAmount));
        response.append(String.format("Wants (%d%%):        %s%,.2f\n",
                allocation.wantsPercentage, CURRENCY_SYMBOL, allocation.wantsAmount));
        response.append(String.format("Savings (%d%%):      %s%,.2f 💎\n\n",
                allocation.savingsPercentage, CURRENCY_SYMBOL, allocation.savingsAmount));

        // Category breakdown
        response.append("📋 CATEGORY ALLOCATIONS\n");
        response.append("──────────────────────────────────────────────────────────\n");

        // Sort categories by amount
        List<Map.Entry<TransactionCategory, Double>> sortedCategories = allocation.categoryAllocations
                .entrySet()
                .stream()
                .sorted(Map.Entry.<TransactionCategory, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<TransactionCategory, Double> entry : sortedCategories) {
            String categoryName = formatCategoryName(entry.getKey());
            double amount = entry.getValue();
            double percentage = (amount / monthlyIncome) * 100;

            response.append(String.format("  • %-20s %s%,10.2f  (%,.1f%%)\n",
                    categoryName, CURRENCY_SYMBOL, amount, percentage));
        }

        // Spending analysis
        if (analysis.transactionCount > 0) {
            response.append("\n📈 YOUR SPENDING ANALYSIS (Last 3 Months)\n");
            response.append("──────────────────────────────────────────────────────────\n");
            response.append(String.format("Total Transactions: %d\n", analysis.transactionCount));
            response.append(String.format("Total Spent:        %s%,.2f\n", CURRENCY_SYMBOL, analysis.totalSpent));
            response.append(String.format("Avg Monthly:        %s%,.2f\n\n",
                    CURRENCY_SYMBOL, analysis.averageMonthlySpending));

            // Show top spending categories
            response.append("Top Spending Categories:\n");
            analysis.categorySpending.entrySet().stream()
                    .sorted(Map.Entry.<TransactionCategory, Double>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> {
                        String categoryName = formatCategoryName(e.getKey());
                        double monthlyAvg = e.getValue() / 3.0;
                        double percentage = analysis.categoryPercentages.getOrDefault(e.getKey(), 0.0);
                        response.append(String.format("  • %-20s %s%,10.2f/month  (%,.1f%% of spending)\n",
                                categoryName, CURRENCY_SYMBOL, monthlyAvg, percentage));
                    });
        }

        // Recommendations
        response.append("\n💡 PERSONALIZED RECOMMENDATIONS\n");
        response.append("──────────────────────────────────────────────────────────\n");
        for (int i = 0; i < recommendations.size(); i++) {
            response.append(String.format("%d. %s\n", i + 1, recommendations.get(i)));
        }

        response.append("\n✅ Budget saved successfully for " + savedBudget.getMonth() + "!\n");
        response.append("Track your spending and stay within budget to achieve your financial goals. 🎯");

        return response.toString();
    }

    /**
     * Format category name for display
     */
    private String formatCategoryName(TransactionCategory category) {
        String name = category.name().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    // Helper classes

    private static class BudgetPreferences {
        BudgetSplit style = BUDGET_STYLES.get("balanced"); // Default
        int customSavingsPercentage = 0; // 0 means use style default
    }

    private static class BudgetSplit {
        final int needs;
        final int wants;
        final int savings;

        BudgetSplit(int needs, int wants, int savings) {
            this.needs = needs;
            this.wants = wants;
            this.savings = savings;
        }
    }

    private static class SpendingAnalysis {
        Map<TransactionCategory, Double> categorySpending = new HashMap<>();
        Map<TransactionCategory, Double> categoryPercentages = new HashMap<>();
        double totalSpent = 0;
        double averageMonthlySpending = 0;
        int transactionCount = 0;
    }

    private static class BudgetAllocation {
        double totalIncome;
        double needsAmount;
        double wantsAmount;
        double savingsAmount;
        int needsPercentage;
        int wantsPercentage;
        int savingsPercentage;
        Map<TransactionCategory, Double> categoryAllocations = new HashMap<>();
    }
}
