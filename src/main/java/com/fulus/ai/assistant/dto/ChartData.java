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
     * Type of chart: "line", "bar", "pie", "doughnut", "area", etc.
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
     * - For line/bar: {"values": [10, 20, 30], "datasets": [...]}
     * - For pie: {"segments": [{"label": "A", "value": 10}, ...]}
     */
    @NotNull(message = "Chart data cannot be null")
    private Map<String, Object> data;

    /**
     * Labels for X-axis or segments
     * Example: ["Jan", "Feb", "Mar"] or ["Category A", "Category B"]
     */
    private List<String> labels;

    /**
     * Optional description or subtitle
     */
    private String description;

    /**
     * Optional color scheme for the chart
     * Example: ["#FF6384", "#36A2EB", "#FFCE56"]
     */
    private List<String> colors;
}

