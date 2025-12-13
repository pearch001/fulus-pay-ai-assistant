package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.service.QRCodeService;
import com.fulus.ai.assistant.util.RateLimiter;
import com.fulus.ai.assistant.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for QR code generation and validation for offline payments
 */
@Slf4j
@RestController
@RequestMapping("/api/offline/qr")
@RequiredArgsConstructor
@Validated
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final RateLimiter rateLimiter;

    /**
     * 3. Generate payment QR code
     * POST /api/offline/qr/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQRCode(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody GenerateQRRequest request) {
        try {
            // Get authenticated user ID
            UUID userUuid = getUserId(userDetails);
            if (userUuid == null) {
                log.warn("Unauthorized QR code generation attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }
            String userId = userUuid.toString();

            log.info("QR code generation request from user: {}, amount: {}", userId, request.getAmount());

            // Rate limiting check
            String rateLimitKey = "qr-generate:" + userId;
            if (!rateLimiter.allowRequest(rateLimitKey, 20, 60)) { // 20 requests per minute
                log.warn("Rate limit exceeded for QR generation: {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before generating more QR codes.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Validate amount
            if (request.getAmount().doubleValue() <= 0) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                                .message("Amount must be greater than zero")
                                .code("INVALID_AMOUNT")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Generate QR code
            int qrSize = request.getQrSize() != null ? request.getQrSize() : 300;
            QRCodeResponse response = qrCodeService.generatePaymentQRCode(
                    userId,
                    request.getAmount().doubleValue(),
                    request.getNote(),
                    qrSize
            );

            log.info("QR code generated successfully for user: {}, requestId: {}",
                    userId, response.getPaymentRequestId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_REQUEST")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (IllegalStateException e) {
            log.error("Service error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("PRECONDITION_FAILED")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error generating QR code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to generate QR code: " + e.getMessage())
                            .code("GENERATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 4. Validate payment QR code
     * POST /api/offline/qr/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateQRCode(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody ValidateQRRequest request) {
        try {
            // Get authenticated user ID (optional for validation)
            UUID userUuid = null;
            try {
                if (userDetails != null) {
                    userUuid = getUserId(userDetails);
                }
            } catch (IllegalStateException ex) {
                // If principal isn't our UserPrincipal, treat as anonymous for validation
                log.debug("Non-standard principal provided for QR validation", ex);
                userUuid = null;
            }

            String userIdForLog = userUuid != null ? userUuid.toString() : "anonymous";
            log.info("QR code validation request from user: {}, data length: {}",
                    userIdForLog, request.getQrData().length());

            // Rate limiting check (more lenient for validation)
            if (userUuid != null) {
                String rateLimitKey = "qr-validate:" + userUuid.toString();
                if (!rateLimiter.allowRequest(rateLimitKey, 30, 60)) { // 30 requests per minute
                    log.warn("Rate limit exceeded for QR validation: {}", userUuid);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(ErrorResponse.builder()
                                    .message("Rate limit exceeded. Please wait before validating more QR codes.")
                                    .code("RATE_LIMIT_EXCEEDED")
                                    .timestamp(LocalDateTime.now())
                                    .build());
                }
            }

            // Parse and validate QR code
            ParsedPaymentDetails parsedDetails = qrCodeService.parsePaymentQRCode(request.getQrData());

            if (!parsedDetails.isValid()) {
                log.warn("Invalid QR code validation: {}", parsedDetails.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(parsedDetails);
            }

            log.info("QR code validated successfully: paymentRequestId={}, amount={}",
                    parsedDetails.getPaymentRequestId(), parsedDetails.getAmount());

            return ResponseEntity.ok(parsedDetails);

        } catch (Exception e) {
            log.error("Error validating QR code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to validate QR code: " + e.getMessage())
                            .code("VALIDATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Get QR code statistics (admin/debug endpoint)
     * GET /api/offline/qr/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get authenticated user ID
            UUID userUuid = getUserId(userDetails);
            if (userUuid == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.debug("QR code statistics request from user: {}", userUuid);

            var stats = qrCodeService.getStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching QR code statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to fetch statistics")
                            .code("STATS_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Cleanup expired QR codes (admin endpoint)
     * POST /api/offline/qr/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupExpiredQRCodes(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get authenticated user ID
            UUID userUuid = getUserId(userDetails);
            if (userUuid == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .message("Authentication required")
                                .code("UNAUTHORIZED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            log.info("QR code cleanup request from user: {}", userUuid);

            int cleanedCount = qrCodeService.cleanupExpiredRequests();

            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "cleanedCount", cleanedCount,
                    "message", "Expired QR codes cleaned up successfully"
            ));

        } catch (Exception e) {
            log.error("Error cleaning up expired QR codes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to cleanup expired QR codes")
                            .code("CLEANUP_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Helper method to extract user ID from UserDetails
     */
    private UUID getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal) {
            return ((UserPrincipal) userDetails).getId();
        }
        throw new IllegalStateException("UserDetails is not an instance of UserPrincipal");
    }

    /**
     * Error response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private String code;
        private LocalDateTime timestamp;
    }
}
