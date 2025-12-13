package com.fulus.ai.assistant.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse natural language time periods into date ranges
 */
@Slf4j
public class TimePeriodParser {

    @Data
    @AllArgsConstructor
    public static class DateRange {
        private LocalDateTime start;
        private LocalDateTime end;
        private String description;
    }

    /**
     * Parse time period string into date range
     *
     * @param timePeriod Natural language time period (e.g., "this month", "last 7 days")
     * @return DateRange with start and end dates
     */
    public static DateRange parse(String timePeriod) {
        if (timePeriod == null || timePeriod.trim().isEmpty()) {
            // Default to all time (very far past to now)
            return new DateRange(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now(),
                    "All time"
            );
        }

        String period = timePeriod.toLowerCase().trim();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        log.debug("Parsing time period: {}", period);

        // Today
        if (period.matches("today")) {
            return new DateRange(
                    today.atStartOfDay(),
                    now,
                    "Today"
            );
        }

        // Yesterday
        if (period.matches("yesterday")) {
            LocalDate yesterday = today.minusDays(1);
            return new DateRange(
                    yesterday.atStartOfDay(),
                    yesterday.atTime(LocalTime.MAX),
                    "Yesterday"
            );
        }

        // This week
        if (period.matches("this week")) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            return new DateRange(
                    weekStart.atStartOfDay(),
                    now,
                    "This week"
            );
        }

        // Last week
        if (period.matches("last week")) {
            LocalDate lastWeekStart = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
            return new DateRange(
                    lastWeekStart.atStartOfDay(),
                    lastWeekEnd.atTime(LocalTime.MAX),
                    "Last week"
            );
        }

        // This month
        if (period.matches("this month")) {
            LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
            return new DateRange(
                    monthStart.atStartOfDay(),
                    now,
                    "This month"
            );
        }

        // Last month
        if (period.matches("last month")) {
            LocalDate lastMonthStart = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            LocalDate lastMonthEnd = lastMonthStart.with(TemporalAdjusters.lastDayOfMonth());
            return new DateRange(
                    lastMonthStart.atStartOfDay(),
                    lastMonthEnd.atTime(LocalTime.MAX),
                    "Last month"
            );
        }

        // This year
        if (period.matches("this year")) {
            LocalDate yearStart = today.with(TemporalAdjusters.firstDayOfYear());
            return new DateRange(
                    yearStart.atStartOfDay(),
                    now,
                    "This year"
            );
        }

        // Last year
        if (period.matches("last year")) {
            LocalDate lastYearStart = today.minusYears(1).with(TemporalAdjusters.firstDayOfYear());
            LocalDate lastYearEnd = lastYearStart.with(TemporalAdjusters.lastDayOfYear());
            return new DateRange(
                    lastYearStart.atStartOfDay(),
                    lastYearEnd.atTime(LocalTime.MAX),
                    "Last year"
            );
        }

        // Last N days/weeks/months pattern
        Pattern lastNPattern = Pattern.compile("last\\s+(\\d+)\\s+(day|days|week|weeks|month|months)");
        Matcher matcher = lastNPattern.matcher(period);

        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            LocalDateTime start;
            String description;

            if (unit.startsWith("day")) {
                start = now.minusDays(count);
                description = String.format("Last %d %s", count, count == 1 ? "day" : "days");
            } else if (unit.startsWith("week")) {
                start = now.minusWeeks(count);
                description = String.format("Last %d %s", count, count == 1 ? "week" : "weeks");
            } else { // month
                start = now.minusMonths(count);
                description = String.format("Last %d %s", count, count == 1 ? "month" : "months");
            }

            return new DateRange(start, now, description);
        }

        // If no match, default to all time
        log.warn("Could not parse time period '{}', defaulting to all time", timePeriod);
        return new DateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                now,
                "All time"
        );
    }

    /**
     * Get common time period presets
     */
    public static String[] getCommonPeriods() {
        return new String[]{
                "today",
                "yesterday",
                "this week",
                "last week",
                "this month",
                "last month",
                "last 7 days",
                "last 30 days",
                "last 3 months",
                "this year",
                "last year"
        };
    }
}
