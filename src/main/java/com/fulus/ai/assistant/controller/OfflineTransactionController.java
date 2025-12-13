package com.fulus.ai.assistant.controller;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.OfflineTransactionChain;
import com.fulus.ai.assistant.entity.SyncConflict;
import com.fulus.ai.assistant.enums.SyncStatus;
import com.fulus.ai.assistant.repository.OfflineTransactionChainRepository;
import com.fulus.ai.assistant.repository.OfflineTransactionRepository;
import com.fulus.ai.assistant.service.OfflineTransactionSyncService;
import com.fulus.ai.assistant.service.OfflineTransactionValidationService;
import com.fulus.ai.assistant.util.RateLimiter;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for offline transaction operations
 */
@Slf4j
@RestController
@RequestMapping("/api/offline")
@RequiredArgsConstructor
@Validated
public class OfflineTransactionController {

    private final OfflineTransactionSyncService syncService;
    private final OfflineTransactionValidationService validationService;
    private final OfflineTransactionRepository offlineTransactionRepository;
    private final OfflineTransactionChainRepository chainRepository;
    private final RateLimiter rateLimiter;

    /**
     * 1. Batch sync offline transactions
     * POST /api/offline/transactions/batch-sync
     */
    @PostMapping("/transactions/batch-sync")
    public ResponseEntity<?> batchSyncTransactions(@Valid @RequestBody BatchSyncRequest request) {
        log.info("Batch sync request received for user: {}, transaction count: {}",
                request.getUserId(), request.getTransactions().size());

        try {
            // Rate limiting check
            String rateLimitKey = "batch-sync:" + request.getUserId();
            if (!rateLimiter.allowRequest(rateLimitKey, 5, 60)) { // 5 requests per minute
                log.warn("Rate limit exceeded for user: {}", request.getUserId());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before retrying.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            // Convert DTOs to entities
            List<OfflineTransaction> transactions = convertToEntities(request.getTransactions());

            // Call sync service
            SyncResult result = syncService.syncOfflineTransactions(request.getUserId(), transactions);

            log.info("Batch sync completed for user: {}, success: {}, failed: {}",
                    request.getUserId(), result.getSuccessCount(), result.getFailedCount());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_REQUEST")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error during batch sync for user: {}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Sync failed: " + e.getMessage())
                            .code("SYNC_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 2. Get pending offline transactions for user
     * GET /api/offline/transactions/pending/{userId}
     */
    @GetMapping("/transactions/pending/{userId}")
    public ResponseEntity<?> getPendingTransactions(@PathVariable @NotBlank String userId) {
        log.info("Fetching pending transactions for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);

            // Get user's chain
            OfflineTransactionChain chain = chainRepository.findByUserId(userUuid).orElse(null);

            if (chain == null) {
                return ResponseEntity.ok(PendingTransactionsResponse.builder()
                        .userId(userId)
                        .totalPending(0)
                        .chainValid(true)
                        .transactions(List.of())
                        .build());
            }

            // Get pending transactions
            List<OfflineTransaction> pending = offlineTransactionRepository
                    .findByUserPhoneNumberAndSyncStatus(chain.getUserPhoneNumber(), SyncStatus.PENDING);

            // Convert to response
            PendingTransactionsResponse response = PendingTransactionsResponse.builder()
                    .userId(userId)
                    .totalPending(pending.size())
                    .chainValid(chain.isChainValid())
                    .lastSyncedHash(chain.getLastSyncedHash())
                    .transactions(pending.stream()
                            .map(this::convertToPendingInfo)
                            .collect(Collectors.toList()))
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message("Invalid user ID format")
                            .code("INVALID_USER_ID")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error fetching pending transactions for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to fetch pending transactions")
                            .code("FETCH_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 3. Get chain status for user
     * GET /api/offline/transactions/chain-status/{userId}
     */
    @GetMapping("/transactions/chain-status/{userId}")
    public ResponseEntity<?> getChainStatus(@PathVariable @NotBlank String userId) {
        log.info("Fetching chain status for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);

            OfflineTransactionChain chain = chainRepository.findByUserId(userUuid).orElse(null);

            if (chain == null) {
                return ResponseEntity.ok(ChainStatusResponse.notFound(userId));
            }

            ChainStatusResponse response = ChainStatusResponse.builder()
                    .userId(userId)
                    .chainExists(true)
                    .chainValid(chain.isChainValid())
                    .lastSyncedHash(chain.getLastSyncedHash())
                    .lastSyncedAt(chain.getLastSyncedAt())
                    .currentHeadHash(chain.getCurrentHeadHash())
                    .totalTransactions(chain.getTotalTransactions())
                    .syncedCount(chain.getSyncedCount())
                    .pendingCount(chain.getPendingCount())
                    .failedCount(chain.getFailedCount())
                    .conflictCount(chain.getConflictCount())
                    .genesisHash(chain.getGenesisHash())
                    .createdAt(chain.getCreatedAt())
                    .updatedAt(chain.getUpdatedAt())
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message("Invalid user ID format")
                            .code("INVALID_USER_ID")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error fetching chain status for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to fetch chain status")
                            .code("FETCH_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 4. Validate chain without syncing
     * POST /api/offline/transactions/validate-chain
     */
    @PostMapping("/transactions/validate-chain")
    public ResponseEntity<?> validateChain(@Valid @RequestBody ValidateChainRequest request) {
        log.info("Chain validation request received for user: {}, transaction count: {}",
                request.getUserId(), request.getTransactions().size());

        try {
            // Rate limiting check
            String rateLimitKey = "validate-chain:" + request.getUserId();
            if (!rateLimiter.allowRequest(rateLimitKey, 10, 60)) { // 10 requests per minute
                log.warn("Rate limit exceeded for user: {}", request.getUserId());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorResponse.builder()
                                .message("Rate limit exceeded. Please wait before retrying.")
                                .code("RATE_LIMIT_EXCEEDED")
                                .timestamp(LocalDateTime.now())
                                .build());
            }

            UUID userUuid = UUID.fromString(request.getUserId());

            // Convert DTOs to entities
            List<OfflineTransaction> transactions = convertToEntities(request.getTransactions());

            // Validate chain integrity
            ChainValidationResult chainValidation = validationService.validateTransactionChain(
                    userUuid, transactions);

            // Validate each payload
            List<ValidationReportResponse.PayloadValidationSummary> payloadValidations = transactions.stream()
                    .map(tx -> {
                        PayloadValidationResult payloadResult = validationService.validateTransactionPayload(tx);
                        return ValidationReportResponse.PayloadValidationSummary.builder()
                                .transactionHash(tx.getTransactionHash())
                                .valid(payloadResult.isValid())
                                .signatureValid(payloadResult.isSignatureValid())
                                .nonceUnique(payloadResult.isNonceUnique())
                                .timestampValid(payloadResult.isTimestampValid())
                                .amountValid(payloadResult.isAmountValid())
                                .errors(payloadResult.getErrors())
                                .warnings(payloadResult.getWarnings())
                                .build();
                    })
                    .collect(Collectors.toList());

            // Check double spending
            DoubleSpendingReport doubleSpendingReport = validationService.detectDoubleSpending(
                    userUuid, transactions);

            // Build response
            ValidationReportResponse response = ValidationReportResponse.builder()
                    .userId(request.getUserId())
                    .valid(chainValidation.isValid() && payloadValidations.stream().allMatch(p -> p.isValid()))
                    .safeToSync(chainValidation.isValid() && !doubleSpendingReport.isHasDoubleSpending() &&
                            payloadValidations.stream().allMatch(p -> p.isValid()))
                    .totalTransactions(transactions.size())
                    .validTransactions(chainValidation.getValidTransactions())
                    .invalidTransactions(chainValidation.getInvalidTransactions())
                    .chainValidation(ValidationReportResponse.ChainValidationSummary.builder()
                            .valid(chainValidation.isValid())
                            .errorCount(chainValidation.getErrorCount())
                            .errors(chainValidation.getErrors().stream()
                                    .map(e -> String.format("%s: %s", e.getErrorType(), e.getErrorMessage()))
                                    .collect(Collectors.toList()))
                            .build())
                    .payloadValidations(payloadValidations)
                    .doubleSpendingCheck(ValidationReportResponse.DoubleSpendingSummary.builder()
                            .detected(doubleSpendingReport.isHasDoubleSpending())
                            .flaggedCount(doubleSpendingReport.getFlaggedTransactions())
                            .lastKnownBalance(doubleSpendingReport.getLastKnownBalance())
                            .projectedBalance(doubleSpendingReport.getProjectedBalance())
                            .flaggedTransactionHashes(doubleSpendingReport.getFlaggedTransactionList().stream()
                                    .map(f -> f.getTransactionHash())
                                    .collect(Collectors.toList()))
                            .build())
                    .build();

            response.generateRecommendation();

            log.info("Chain validation completed for user: {}, valid: {}, safeToSync: {}",
                    request.getUserId(), response.isValid(), response.isSafeToSync());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message(e.getMessage())
                            .code("INVALID_REQUEST")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error during chain validation for user: {}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Validation failed: " + e.getMessage())
                            .code("VALIDATION_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 5. Get unresolved conflicts for user
     * GET /api/offline/conflicts/{userId}
     */
    @GetMapping("/conflicts/{userId}")
    public ResponseEntity<?> getUnresolvedConflicts(@PathVariable @NotBlank String userId) {
        log.info("Fetching unresolved conflicts for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);

            List<SyncConflict> conflicts = syncService.getUnresolvedConflicts(userUuid);

            List<ConflictResponse> response = conflicts.stream()
                    .map(this::convertToConflictResponse)
                    .collect(Collectors.toList());

            log.info("Found {} unresolved conflicts for user: {}", response.size(), userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .message("Invalid user ID format")
                            .code("INVALID_USER_ID")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Error fetching conflicts for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .message("Failed to fetch conflicts")
                            .code("FETCH_ERROR")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Convert DTO to entity
     */
    private List<OfflineTransaction> convertToEntities(List<OfflineTransactionDTO> dtos) {
        return dtos.stream()
                .map(dto -> {
                    OfflineTransaction tx = new OfflineTransaction();
                    tx.setSenderPhoneNumber(dto.getSenderPhoneNumber());
                    tx.setRecipientPhoneNumber(dto.getRecipientPhoneNumber());
                    tx.setAmount(dto.getAmount());
                    tx.setTransactionHash(dto.getTransactionHash());
                    tx.setPreviousHash(dto.getPreviousHash());
                    tx.setSignatureKey(dto.getSignature());
                    tx.setNonce(dto.getNonce());
                    tx.setTimestamp(dto.getTimestamp());
                    tx.setPayload(dto.getPayload());
                    tx.setSenderOfflineBalance(dto.getSenderOfflineBalance());
                    tx.setDescription(dto.getDescription());
                    tx.setSyncStatus(SyncStatus.PENDING);
                    tx.setSyncAttempts(0);
                    tx.setCreatedAt(LocalDateTime.now());
                    return tx;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to pending info
     */
    private PendingTransactionsResponse.PendingTransactionInfo convertToPendingInfo(OfflineTransaction tx) {
        return PendingTransactionsResponse.PendingTransactionInfo.builder()
                .id(tx.getId())
                .transactionHash(tx.getTransactionHash())
                .previousHash(tx.getPreviousHash())
                .senderPhoneNumber(tx.getSenderPhoneNumber())
                .recipientPhoneNumber(tx.getRecipientPhoneNumber())
                .amount(tx.getAmount())
                .timestamp(tx.getTimestamp())
                .syncStatus(tx.getSyncStatus().name())
                .syncAttempts(tx.getSyncAttempts())
                .lastSyncAttempt(tx.getLastSyncAttempt())
                .syncError(tx.getSyncError())
                .senderOfflineBalance(tx.getSenderOfflineBalance())
                .build();
    }

    /**
     * Convert conflict entity to response
     */
    private ConflictResponse convertToConflictResponse(SyncConflict conflict) {
        ConflictResponse response = ConflictResponse.builder()
                .conflictId(conflict.getId())
                .transactionId(conflict.getTransactionId())
                .transactionHash(conflict.getTransactionHash())
                .conflictType(conflict.getConflictType().name())
                .conflictDescription(conflict.getConflictDescription())
                .transactionAmount(conflict.getTransactionAmount())
                .expectedBalance(conflict.getExpectedBalance())
                .actualBalance(conflict.getActualBalance())
                .expectedValue(conflict.getExpectedValue())
                .actualValue(conflict.getActualValue())
                .priority(conflict.getPriority())
                .resolutionStatus(conflict.getResolutionStatus().name())
                .autoResolutionAttempted(conflict.isAutoResolutionAttempted())
                .autoResolutionResult(conflict.getAutoResolutionResult())
                .detectedAt(conflict.getDetectedAt())
                .resolvedAt(conflict.getResolvedAt())
                .resolutionNotes(conflict.getResolutionNotes())
                .build();

        response.addSuggestedResolution();
        return response;
    }

    /**
     * Error response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private String code;
        private LocalDateTime timestamp;
    }
}
