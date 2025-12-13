package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.SavingsCalculatorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Spring AI Function Tool for calculating savings projections with compound interest
 * This function is automatically discoverable by Spring AI's function calling mechanism
 */
@Component("savingsCalculatorFunction")
@Description("Calculate savings projections with compound interest. Shows how much money you'll have after saving a fixed amount monthly for a specific period. Returns a detailed, conversational explanation with total contributions, interest earned, final amount, and month-by-month growth. Use this when users want to plan savings, see investment returns, or understand how their money will grow over time.")
@RequiredArgsConstructor
@Slf4j
public class SavingsCalculatorFunction implements Function<SavingsCalculatorRequest, String> {

    private static final double DEFAULT_INTEREST_RATE = 5.0; // 5% annual interest
    private static final String CURRENCY_SYMBOL = "₦";

    /**
     * Calculate savings projection with compound interest
     *
     * @param request Request containing monthlyAmount, months, and optional interestRate
     * @return Conversational explanation of savings projection
     */
    @Override
    public String apply(SavingsCalculatorRequest request) {
        log.info("Calculating savings projection: monthly={}, months={}, rate={}",
                request.getMonthlyAmount(), request.getMonths(), request.getInterestRate());

        try {
            // Validate inputs
            if (request.getMonthlyAmount() == null || request.getMonthlyAmount() <= 0) {
                return "I need a valid monthly savings amount to calculate your projection. Please provide an amount greater than zero.";
            }

            if (request.getMonths() == null || request.getMonths() <= 0) {
                return "I need a valid time period to calculate your savings. Please provide a number of months greater than zero.";
            }

            if (request.getMonths() > 600) { // 50 years max
                return "That's quite a long time horizon! I can calculate projections for up to 600 months (50 years). Please choose a shorter period.";
            }

            double monthlyAmount = request.getMonthlyAmount();
            int months = request.getMonths();
            double annualRate = request.getInterestRate() != null ? request.getInterestRate() : DEFAULT_INTEREST_RATE;

            // Calculate projection
            SavingsProjection projection = calculateProjection(monthlyAmount, months, annualRate);

            // Generate conversational response
            return formatConversationalResponse(projection, monthlyAmount, months, annualRate);

        } catch (Exception e) {
            log.error("Error calculating savings projection", e);
            return "I encountered an error while calculating your savings projection. Please check your inputs and try again.";
        }
    }

    /**
     * Calculate savings projection with compound interest
     */
    private SavingsProjection calculateProjection(double monthlyAmount, int months, double annualRate) {
        double monthlyRate = (annualRate / 100.0) / 12.0;

        List<MonthlySnapshot> snapshots = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal totalContributions = BigDecimal.ZERO;

        for (int month = 1; month <= months; month++) {
            // Add monthly contribution
            BigDecimal contribution = BigDecimal.valueOf(monthlyAmount);
            balance = balance.add(contribution);
            totalContributions = totalContributions.add(contribution);

            // Apply monthly interest
            if (monthlyRate > 0) {
                BigDecimal interest = balance.multiply(BigDecimal.valueOf(monthlyRate));
                balance = balance.add(interest);
            }

            // Store snapshot
            snapshots.add(new MonthlySnapshot(month, balance.setScale(2, RoundingMode.HALF_UP)));
        }

        BigDecimal finalAmount = balance.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = finalAmount.subtract(totalContributions).setScale(2, RoundingMode.HALF_UP);

        // Calculate effective annual yield
        double effectiveYield = calculateEffectiveYield(monthlyAmount, months, annualRate, finalAmount.doubleValue());

        return new SavingsProjection(
                totalContributions.setScale(2, RoundingMode.HALF_UP),
                totalInterest,
                finalAmount,
                effectiveYield,
                snapshots
        );
    }

    /**
     * Calculate effective annual yield
     */
    private double calculateEffectiveYield(double monthlyAmount, int months, double annualRate, double finalAmount) {
        if (months == 0 || monthlyAmount == 0) {
            return 0.0;
        }

        double totalContributed = monthlyAmount * months;
        double totalReturn = finalAmount - totalContributed;
        double years = months / 12.0;

        if (years == 0 || totalContributed == 0) {
            return 0.0;
        }

        // Effective yield = (total return / total contributed) / years * 100
        return (totalReturn / totalContributed) / years * 100.0;
    }

    /**
     * Format conversational response
     */
    private String formatConversationalResponse(SavingsProjection projection, double monthlyAmount, int months, double annualRate) {
        StringBuilder response = new StringBuilder();

        // Opening statement
        response.append("💰 Savings Projection Calculator\n");
        response.append("════════════════════════════════════════\n\n");

        // Scenario description
        int years = months / 12;
        int remainingMonths = months % 12;
        String timeDescription;
        if (years > 0 && remainingMonths > 0) {
            timeDescription = String.format("%d %s and %d %s",
                    years, years == 1 ? "year" : "years",
                    remainingMonths, remainingMonths == 1 ? "month" : "months");
        } else if (years > 0) {
            timeDescription = String.format("%d %s", years, years == 1 ? "year" : "years");
        } else {
            timeDescription = String.format("%d %s", months, months == 1 ? "month" : "months");
        }

        response.append(String.format("If you save %s%,.2f every month for %s at %,.2f%% annual interest, here's what happens:\n\n",
                CURRENCY_SYMBOL, monthlyAmount, timeDescription, annualRate));

        // Key results
        response.append("📊 THE RESULTS\n");
        response.append("────────────────────────────────────────\n");
        response.append(String.format("Your Total Contributions:  %s%,.2f\n",
                CURRENCY_SYMBOL, projection.totalContributions));
        response.append(String.format("Interest You'll Earn:      %s%,.2f 💸\n",
                CURRENCY_SYMBOL, projection.totalInterest));
        response.append(String.format("Your Final Amount:         %s%,.2f ✨\n\n",
                CURRENCY_SYMBOL, projection.finalAmount));

        // Effective yield
        response.append(String.format("Effective Annual Yield:    %.2f%%\n\n",
                projection.effectiveYield));

        // Explanation
        response.append("💡 WHAT THIS MEANS\n");
        response.append("────────────────────────────────────────\n");

        double percentageGain = (projection.totalInterest.doubleValue() / projection.totalContributions.doubleValue()) * 100;
        response.append(String.format("Your money grows by %.1f%% thanks to compound interest! ", percentageGain));
        response.append("That means your money earns interest, and then that interest earns interest too.\n\n");

        // Month-by-month growth (sample)
        response.append("📈 MONTH-BY-MONTH GROWTH\n");
        response.append("────────────────────────────────────────\n");

        // Show first 3 months, middle month, and last 3 months
        List<MonthlySnapshot> snapshots = projection.snapshots;
        int snapshotCount = snapshots.size();

        if (snapshotCount <= 7) {
            // Show all if 7 or fewer
            for (MonthlySnapshot snapshot : snapshots) {
                response.append(formatSnapshot(snapshot));
            }
        } else {
            // Show first 3
            for (int i = 0; i < 3; i++) {
                response.append(formatSnapshot(snapshots.get(i)));
            }

            response.append("    ...\n");

            // Show middle
            MonthlySnapshot middle = snapshots.get(snapshotCount / 2);
            response.append(formatSnapshot(middle));

            response.append("    ...\n");

            // Show last 3
            for (int i = snapshotCount - 3; i < snapshotCount; i++) {
                response.append(formatSnapshot(snapshots.get(i)));
            }
        }

        // Closing advice
        response.append("\n✅ BOTTOM LINE\n");
        response.append("────────────────────────────────────────\n");

        if (projection.totalInterest.doubleValue() > 1000) {
            response.append(String.format("You'll earn an extra %s%,.2f just by being patient and consistent with your savings. ",
                    CURRENCY_SYMBOL, projection.totalInterest));
        } else {
            response.append(String.format("You'll earn %s%,.2f in interest. ",
                    CURRENCY_SYMBOL, projection.totalInterest));
        }

        if (annualRate < 3) {
            response.append("Consider looking for higher interest rates to maximize your returns!");
        } else if (annualRate >= 3 && annualRate < 6) {
            response.append("That's a decent return on your savings!");
        } else {
            response.append("That's a great return on your savings!");
        }

        response.append("\n\nStart saving today and watch your money grow! 🌱");

        return response.toString();
    }

    /**
     * Format a single month snapshot
     */
    private String formatSnapshot(MonthlySnapshot snapshot) {
        return String.format("Month %3d: %s%,10.2f\n",
                snapshot.month,
                CURRENCY_SYMBOL,
                snapshot.balance);
    }

    /**
     * Data class for savings projection results
     */
    private static class SavingsProjection {
        BigDecimal totalContributions;
        BigDecimal totalInterest;
        BigDecimal finalAmount;
        double effectiveYield;
        List<MonthlySnapshot> snapshots;

        SavingsProjection(BigDecimal totalContributions, BigDecimal totalInterest,
                         BigDecimal finalAmount, double effectiveYield,
                         List<MonthlySnapshot> snapshots) {
            this.totalContributions = totalContributions;
            this.totalInterest = totalInterest;
            this.finalAmount = finalAmount;
            this.effectiveYield = effectiveYield;
            this.snapshots = snapshots;
        }
    }

    /**
     * Data class for monthly snapshot
     */
    private static class MonthlySnapshot {
        int month;
        BigDecimal balance;

        MonthlySnapshot(int month, BigDecimal balance) {
            this.month = month;
            this.balance = balance;
        }
    }
}
