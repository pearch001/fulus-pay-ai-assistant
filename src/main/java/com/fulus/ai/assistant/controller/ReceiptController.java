package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.service.ReceiptPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for receipt PDF generation
 */
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Receipts", description = "Receipt PDF generation API")
@SecurityRequirement(name = "Bearer Authentication")
public class ReceiptController {

    private final ReceiptPdfService receiptPdfService;

    /**
     * Download receipt PDF for a transaction
     */
    @GetMapping("/{transactionId}/download")
    @Operation(summary = "Download receipt PDF", description = "Generate and download PDF receipt for a transaction")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable UUID transactionId) {
        log.info("GET /api/v1/receipts/{}/download - Receipt download request", transactionId);

        try {
            byte[] pdfBytes = receiptPdfService.generateReceipt(transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "receipt-" + transactionId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("Receipt generation failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error generating receipt PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * View receipt PDF inline (in browser)
     */
    @GetMapping("/{transactionId}/view")
    @Operation(summary = "View receipt PDF", description = "View PDF receipt inline in browser")
    public ResponseEntity<byte[]> viewReceipt(@PathVariable UUID transactionId) {
        log.info("GET /api/v1/receipts/{}/view - Receipt view request", transactionId);

        try {
            byte[] pdfBytes = receiptPdfService.generateReceipt(transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "receipt-" + transactionId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("Receipt generation failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error generating receipt PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
