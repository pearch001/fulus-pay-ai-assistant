package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.ChartData;
import com.fulus.ai.assistant.enums.InsightCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for generating chart data for admin insights
 * Detects when charts are needed and generates appropriate visualizations
 */
@Slf4j
@Service
public class ChartGenerationService {

    /**
     * Enum for chart types
     */
    public enum ChartType {
        LINE, BAR, PIE, DONUT, AREA
    }

    /**
     * Determines if a chart should be generated based on user message
     *
     * @param message user's message
     * @param aiResponse AI's response
     * @return true if chart should be generated
     */
    public boolean shouldGenerateChart(String message, String aiResponse) {
        // Detect chart keywords in user message
        String lower = message.toLowerCase();
        return lower.contains("show") &&
               (lower.contains("chart") || lower.contains("graph") ||
                lower.contains("trend") || lower.contains("visualize"));
    }

    /**
     * Detects the optimal chart type based on message content and platform stats
     *
     * @param message user's message
     * @param platformStats platform statistics
     * @return optimal chart type
     */
    public ChartType detectOptimalChartType(String message, Map<String, Object> platformStats) {
        String lower = message.toLowerCase();

        // Time-based analysis → Line chart
        if (containsAny(lower, "trend", "over time", "timeline", "growth", "history")) {
            return ChartType.LINE;
        }

        // Distribution/proportion → Pie chart
        if (containsAny(lower, "breakdown", "distribution", "percentage", "share", "split")) {
            return ChartType.PIE;
        }

        // Comparison → Bar chart
        if (containsAny(lower, "compare", "vs", "versus", "difference", "between")) {
            return ChartType.BAR;
        }

        // Multiple metrics over time → Area chart
        if (containsAny(lower, "multiple", "combined", "together") &&
            containsAny(lower, "trend", "over time")) {
            return ChartType.AREA;
        }

        // Default to bar chart for general queries
        return ChartType.BAR;
    }

    /**
     * Helper method to check if text contains any of the given keywords
     *
     * @param text text to search
     * @param keywords keywords to look for
     * @return true if any keyword is found
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Generates contextual charts based on insight category and query
     *
     * @param userMessage user's message
     * @param category insight category
     * @param platformStats platform statistics
     * @return list of contextual chart data
     */
    public List<ChartData> generateContextualCharts(
            String userMessage,
            InsightCategory category,
            Map<String, Object> platformStats
    ) {
        List<ChartData> charts = new ArrayList<>();
        String lower = userMessage.toLowerCase();

        switch (category) {
            case REVENUE_ANALYSIS:
                charts.add(generateRevenueChart(platformStats));
                if (lower.contains("breakdown") || lower.contains("source")) {
                    charts.add(generateRevenueSourcesChart(platformStats));
                }
                break;

            case USER_GROWTH:
                charts.add(generateUserGrowthChart(platformStats));
                if (lower.contains("active")) {
                    charts.add(generateActiveUsersChart(platformStats));
                }
                break;

            case TRANSACTION_PATTERNS:
                charts.add(generateTransactionVolumeChart(platformStats));
                if (lower.contains("success") || lower.contains("failed") || lower.contains("failure")) {
                    charts.add(generateSuccessRateChart(platformStats));
                }
                break;

            case FEE_OPTIMIZATION:
                charts.add(generateFeeRevenueChart(platformStats));
                charts.add(generateFeeBreakdownChart(platformStats));
                break;

            default:
                // General overview chart
                charts.add(generateOverviewChart(platformStats));
        }

        log.info("Generated {} contextual chart(s) for category: {}", charts.size(), category.name());
        return charts;
    }

    /**
     * Generates charts based on user message - SIMPLIFIED VERSION
     *
     * @param userMessage user's message
     * @return list of chart data
     */
    public List<ChartData> generateCharts(String userMessage) {
        List<ChartData> charts = new ArrayList<>();
        String lower = userMessage.toLowerCase();

        // Revenue charts
        if (lower.contains("revenue") || lower.contains("income") || lower.contains("earning")) {
            charts.add(generateRevenueChart());
            log.info("Generated revenue chart");
        }
        // User charts
        else if (lower.contains("user") || lower.contains("customer") || lower.contains("growth")) {
            charts.add(generateUserChart());
            log.info("Generated user chart");
        }
        // Transaction charts
        else if (lower.contains("transaction") || lower.contains("payment")) {
            charts.add(generateTransactionChart());
            log.info("Generated transaction chart");
        }
        // Default: show revenue trend for any "show/chart/trend" request
        else {
            charts.add(generateRevenueChart());
            log.info("Generated default revenue chart");
        }

        return charts;
    }

    /**
     * Generates charts based on user message context (with AI response)
     *
     * @param userMessage user's message
     * @param aiResponse AI's response
     * @return list of chart data
     */
    public List<ChartData> generateCharts(String userMessage, String aiResponse) {
        return generateCharts(userMessage);
    }

    /**
     * Generates a line chart showing revenue trend over time
     *
     * @return ChartData for revenue line chart
     */
    private ChartData generateRevenueLineChart() {
        // Generate synthetic revenue data
        List<Map<String, Object>> data = new ArrayList<>();

        String[] months = {"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String month : months) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", month);
            point.put("revenue", ThreadLocalRandom.current().nextLong(4000000, 8000000));
            data.add(point);
        }

        return ChartData.builder()
            .type("line")
            .title("Revenue Trend - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Revenue (₦)")
                .build())
            .colors(List.of("#36A2EB"))
            .build();
    }

    /**
     * Generates a pie chart showing transaction type distribution
     *
     * @return ChartData for distribution pie chart
     */
    private ChartData generateDistributionPieChart() {
        // Generate transaction type distribution
        List<Map<String, Object>> data = new ArrayList<>();

        String[] types = {"Transfer", "Withdrawal", "Deposit", "Payment"};
        String[] colors = {"#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0"};

        for (int i = 0; i < types.length; i++) {
            Map<String, Object> segment = new HashMap<>();
            segment.put("label", types[i]);
            segment.put("value", ThreadLocalRandom.current().nextInt(15, 40));
            data.add(segment);
        }

        return ChartData.builder()
            .type("pie")
            .title("Transaction Type Distribution")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Type")
                .y("Percentage (%)")
                .build())
            .colors(List.of(colors))
            .build();
    }

    /**
     * Generates a bar chart comparing different metrics
     *
     * @return ChartData for comparison bar chart
     */
    private ChartData generateComparisonBarChart() {
        // Generate comparison data
        List<Map<String, Object>> data = new ArrayList<>();

        String[] categories = {"Q1", "Q2", "Q3", "Q4"};

        for (String category : categories) {
            Map<String, Object> bar = new HashMap<>();
            bar.put("category", category);
            bar.put("online", ThreadLocalRandom.current().nextLong(2000000, 5000000));
            bar.put("offline", ThreadLocalRandom.current().nextLong(3000000, 6000000));
            data.add(bar);
        }

        return ChartData.builder()
            .type("bar")
            .title("Quarterly Transaction Comparison")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Quarter")
                .y("Transaction Volume (₦)")
                .build())
            .colors(List.of("#FF6384", "#36A2EB"))
            .build();
    }

    // ========== Contextual Chart Generators ==========

    // ========== Simple Chart Generators (No Parameters) ==========

    /**
     * Generates revenue chart - simplified version
     */
    private ChartData generateRevenueChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = {"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", months[i]);
            point.put("revenue", 4500000 + (i * 600000)); // 4.5M to 7.5M
            data.add(point);
        }

        return ChartData.builder()
            .type("line")
            .title("Revenue Trend - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder().x("Month").y("Revenue (₦)").build())
            .colors(Arrays.asList("#10B981"))
            .build();
    }

    /**
     * Generates user growth chart - simplified version
     */
    private ChartData generateUserChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = {"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", months[i]);
            point.put("users", 15000 + (i * 2000));
            data.add(point);
        }

        return ChartData.builder()
            .type("area")
            .title("User Growth - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder().x("Month").y("Users").build())
            .colors(Arrays.asList("#3B82F6"))
            .build();
    }

    /**
     * Generates transaction status chart - simplified version
     */
    private ChartData generateTransactionChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(new HashMap<String, Object>() {{
            put("status", "Successful");
            put("count", 245000);
            put("percentage", 85);
        }});
        data.add(new HashMap<String, Object>() {{
            put("status", "Pending");
            put("count", 28000);
            put("percentage", 10);
        }});
        data.add(new HashMap<String, Object>() {{
            put("status", "Failed");
            put("count", 15000);
            put("percentage", 5);
        }});

        return ChartData.builder()
            .type("pie")
            .title("Transaction Status Distribution")
            .data(data)
            .labels(ChartData.ChartLabels.builder().x("Status").y("Count").build())
            .colors(Arrays.asList("#10B981", "#F59E0B", "#EF4444"))
            .build();
    }

    // ========== Contextual Chart Generators (With Stats Parameter) ==========

    /**
     * Generates revenue trend chart
     */
    private ChartData generateRevenueChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        String[] months = {"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        long baseRevenue = 4000000;

        for (int i = 0; i < months.length; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", months[i]);
            point.put("revenue", baseRevenue + (i * ThreadLocalRandom.current().nextLong(500000, 1000000)));
            data.add(point);
        }

        return ChartData.builder()
            .type("line")
            .title("Revenue Trend - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Revenue (₦)")
                .build())
            .colors(Arrays.asList("#10B981"))
            .build();
    }

    /**
     * Generates revenue sources breakdown chart
     */
    private ChartData generateRevenueSourcesChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        String[] sources = {"Transaction Fees", "Card Fees", "Bill Payments", "Subscriptions"};
        int[] percentages = {45, 30, 20, 5};

        for (int i = 0; i < sources.length; i++) {
            Map<String, Object> segment = new HashMap<>();
            segment.put("label", sources[i]);
            segment.put("value", percentages[i]);
            data.add(segment);
        }

        return ChartData.builder()
            .type("pie")
            .title("Revenue Sources Breakdown")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Source")
                .y("Percentage (%)")
                .build())
            .colors(Arrays.asList("#10B981", "#3B82F6", "#F59E0B", "#8B5CF6"))
            .build();
    }

    /**
     * Generates user growth chart
     */
    private ChartData generateUserGrowthChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        int baseUsers = 10000;
        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", getMonthName(i));
            point.put("users", baseUsers + (i * ThreadLocalRandom.current().nextInt(1500, 3000)));
            point.put("active", (int)((baseUsers + i * 2000) * 0.72)); // 72% active
            data.add(point);
        }

        return ChartData.builder()
            .type("area")
            .title("User Growth - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Users")
                .build())
            .colors(Arrays.asList("#3B82F6", "#10B981"))
            .build();
    }

    /**
     * Generates active users comparison chart
     */
    private ChartData generateActiveUsersChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", getMonthName(i));
            point.put("dau", ThreadLocalRandom.current().nextInt(3000, 5000));
            point.put("mau", ThreadLocalRandom.current().nextInt(10000, 15000));
            data.add(point);
        }

        return ChartData.builder()
            .type("line")
            .title("Active Users - DAU vs MAU")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Active Users")
                .build())
            .colors(Arrays.asList("#EF4444", "#3B82F6"))
            .build();
    }

    /**
     * Generates transaction volume chart
     */
    private ChartData generateTransactionVolumeChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", getMonthName(i));
            point.put("volume", ThreadLocalRandom.current().nextInt(50000, 100000));
            data.add(point);
        }

        return ChartData.builder()
            .type("bar")
            .title("Transaction Volume - Last 6 Months")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Transactions")
                .build())
            .colors(Arrays.asList("#8B5CF6"))
            .build();
    }

    /**
     * Generates success rate chart
     */
    private ChartData generateSuccessRateChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", getMonthName(i));
            point.put("success", ThreadLocalRandom.current().nextDouble(92.0, 97.0));
            point.put("failed", ThreadLocalRandom.current().nextDouble(3.0, 8.0));
            data.add(point);
        }

        return ChartData.builder()
            .type("line")
            .title("Transaction Success Rate")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Rate (%)")
                .build())
            .colors(Arrays.asList("#10B981", "#EF4444"))
            .build();
    }

    /**
     * Generates fee revenue chart
     */
    private ChartData generateFeeRevenueChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", getMonthName(i));
            point.put("revenue", ThreadLocalRandom.current().nextLong(1000000, 3000000));
            data.add(point);
        }

        return ChartData.builder()
            .type("area")
            .title("Fee Revenue Trend")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Month")
                .y("Revenue (₦)")
                .build())
            .colors(Arrays.asList("#F59E0B"))
            .build();
    }

    /**
     * Generates fee breakdown chart
     */
    private ChartData generateFeeBreakdownChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        String[] feeTypes = {"Transfer Fee", "Withdrawal Fee", "Card Fee", "Bill Payment Fee"};

        for (String feeType : feeTypes) {
            Map<String, Object> bar = new HashMap<>();
            bar.put("type", feeType);
            bar.put("revenue", ThreadLocalRandom.current().nextLong(500000, 1500000));
            data.add(bar);
        }

        return ChartData.builder()
            .type("bar")
            .title("Fee Revenue by Type")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Fee Type")
                .y("Revenue (₦)")
                .build())
            .colors(Arrays.asList("#F59E0B", "#EF4444", "#3B82F6", "#10B981"))
            .build();
    }

    /**
     * Generates overview chart for general queries
     */
    private ChartData generateOverviewChart(Map<String, Object> stats) {
        List<Map<String, Object>> data = new ArrayList<>();

        String[] metrics = {"Users", "Transactions", "Revenue", "Success Rate"};
        double[] values = {
            ThreadLocalRandom.current().nextDouble(15000, 25000),
            ThreadLocalRandom.current().nextDouble(100000, 200000),
            ThreadLocalRandom.current().nextDouble(5000000, 8000000),
            ThreadLocalRandom.current().nextDouble(92, 97)
        };

        for (int i = 0; i < metrics.length; i++) {
            Map<String, Object> bar = new HashMap<>();
            bar.put("metric", metrics[i]);
            bar.put("value", values[i]);
            data.add(bar);
        }

        return ChartData.builder()
            .type("bar")
            .title("Platform Overview")
            .data(data)
            .labels(ChartData.ChartLabels.builder()
                .x("Metric")
                .y("Value")
                .build())
            .colors(Arrays.asList("#3B82F6", "#10B981", "#F59E0B", "#8B5CF6"))
            .build();
    }

    /**
     * Helper method to get month name
     */
    private String getMonthName(int offset) {
        String[] months = {"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[Math.min(offset, months.length - 1)];
    }
}
