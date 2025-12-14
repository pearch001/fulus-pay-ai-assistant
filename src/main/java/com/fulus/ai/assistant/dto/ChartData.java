package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for chart data visualization (for future use)
 * Supports different chart types like line, bar, pie charts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {

    /**
     * Type of chart: "line", "bar", "pie", "donut", "area"
     */
    @NotBlank(message = "Chart type cannot be empty")
    private String type;

    /**
     * Title of the chart
     */
    @NotBlank(message = "Chart title cannot be empty")
    private String title;

    /**
     * Chart data - flexible structure to support various chart types
     * Examples:
     * - For line/bar: [{"label": "A", "value": 10}, {"label": "B", "value": 20}]
     * - For pie: [{"label": "Category A", "value": 10}, {"label": "Category B", "value": 20}]
     */
    @NotNull(message = "Chart data cannot be null")
    private List<Map<String, Object>> data;

    /**
     * Axis labels for the chart
     */
    private ChartLabels labels;

    /**
     * Optional color scheme for the chart
     * Example: ["#FF6384", "#36A2EB", "#FFCE56"]
     */
    private List<String> colors;

    /**
     * Chart axis labels
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartLabels {
        /**
         * X-axis label
         */
        private String x;

        /**
         * Y-axis label
         */
        private String y;
    }
}

