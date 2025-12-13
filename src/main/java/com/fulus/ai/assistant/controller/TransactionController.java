package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.TransactionPageResponse;
import com.fulus.ai.assistant.dto.TransactionSummaryDTO;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.service.TransactionExportService;
import com.fulus.ai.assistant.service.TransactionService;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Transaction Management
 *
 * Endpoints:
 * - GET /api/v1/transactions/{userId} - Get paginated transactions with filtering
 * - GET /api/v1/transactions/{userId}/summary - Get spending summary by category
 * - GET /api/v1/transactions/{userId}/export - Export transactions as CSV or PDF
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExportService exportService;

    /**
     * Get paginated and filtered transactions
     *
     * @param userId User ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param category Transaction category filter (optional)
     * @param fromDate Start date filter (optional)
     * @param toDate End date filter (optional)
     * @return Paginated transaction list
     */
    @GetMapping("/{userId}")
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("GET /api/v1/transactions/{} - page: {}, size: {}, category: {}, from: {}, to: {}",
                userId, page, size, category, fromDate, toDate);

        // Validate page size
        if (size > 100) {
            size = 100;
            log.warn("Page size exceeded maximum, set to 100");
        }
        if (size < 1) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }

        try {
            TransactionPageResponse response = transactionService.getTransactions(
                    userId, page, size, category, fromDate, toDate);

            log.info("Retrieved {} transactions for user: {}", response.getTransactions().size(), userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching transactions for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get transaction summary with category breakdown
     *
     * @param userId User ID
     * @param period Period: this_month, last_month, last_3_months, last_6_months, this_year, last_year, all_time, custom
     * @param fromDate Custom start date (required if period=custom)
     * @param toDate Custom end date (required if period=custom)
     * @return Transaction summary
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<?> getTransactionSummary(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "this_month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("GET /api/v1/transactions/{}/summary - period: {}, from: {}, to: {}",
                userId, period, fromDate, toDate);

        // Validate custom period
        if ("custom".equalsIgnoreCase(period) && (fromDate == null || toDate == null)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Custom period requires both fromDate and toDate parameters");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            TransactionSummaryDTO summary = transactionService.getTransactionSummary(
                    userId, period, fromDate, toDate);

            log.info("Generated summary for user: {} - Total Income: ₦{}, Total Expenses: ₦{}",
                    userId, summary.getTotalIncome(), summary.getTotalExpenses());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error generating summary for user: {}", userId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate transaction summary");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Export transactions as CSV or PDF
     *
     * @param userId User ID
     * @param format Export format: csv or pdf
     * @param fromDate Start date (optional)
     * @param toDate End date (optional)
     * @return Downloadable file
     */
    @GetMapping("/{userId}/export")
    public ResponseEntity<?> exportTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("GET /api/v1/transactions/{}/export - format: {}, from: {}, to: {}",
                userId, format, fromDate, toDate);

        // Validate format
        if (!format.equalsIgnoreCase("csv") && !format.equalsIgnoreCase("pdf")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid format. Supported formats: csv, pdf");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            byte[] exportData;
            String contentType;
            String filename = exportService.getExportFilename(format, userId, fromDate, toDate);

            if (format.equalsIgnoreCase("csv")) {
                exportData = exportService.generateCSV(userId, fromDate, toDate);
                contentType = "text/csv";
            } else {
                exportData = exportService.generatePDF(userId, fromDate, toDate);
                contentType = "application/pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(exportData.length);

            log.info("Generated {} export for user: {} - {} bytes", format.toUpperCase(), userId, exportData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);

        } catch (IOException | DocumentException e) {
            log.error("Error generating {} export for user: {}", format, userId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate export: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);

        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", userId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Unexpected error generating export for user: {}", userId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is running");
    }
}
