package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.*;
import com.fulus.ai.assistant.enums.*;
import com.fulus.ai.assistant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for syncing offline transactions with the online system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineTransactionSyncService {

    private final UserRepository userRepository;
    private final OfflineTransactionRepository offlineTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final OfflineTransactionChainRepository offlineTransactionChainRepository;
    private final SyncConflictRepository syncConflictRepository;
    private final OfflineTransactionValidationService validationService;
    private final PaymentService paymentService;

    /**
     * Sync offline transactions for a user
     *
     * @param userId                The user ID
     * @param offlineTransactions   List of offline transactions to sync
     * @return SyncResult with details of synced/failed transactions and conflicts
     */
    @Transactional
    public SyncResult syncOfflineTransactions(String userId, List<OfflineTransaction> offlineTransactions) {
        log.info("Starting offline transaction sync for user: {}, transaction count: {}", userId, offlineTransactions.size());

        try {
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Initialize result
            SyncResult result = SyncResult.builder()
                    .userId(userId)
                    .totalTransactions(offlineTransactions.size())
                    .successCount(0)
                    .failedCount(0)
                    .conflictCount(0)
                    .syncTimestamp(LocalDateTime.now())
                    .syncedTransactions(new ArrayList<>())
                    .failedTransactions(new ArrayList<>())
                    .conflicts(new ArrayList<>())
                    .build();

            // Step 1: Retrieve or create user's chain metadata
            OfflineTransactionChain chain = getOrCreateChain(userUuid, user.getPhoneNumber());
            String lastSyncedHash = chain.getLastSyncedHash();
            log.debug("User's last synced hash: {}", lastSyncedHash);

            // Step 2: Validate transaction chain integrity
            ChainValidationResult chainValidation = validationService.validateTransactionChain(
                    userUuid, offlineTransactions);

            if (!chainValidation.isValid()) {
                log.warn("Chain validation failed for user: {}, errors: {}",
                        userId, chainValidation.getErrorCount());
                handleChainValidationFailure(chainValidation, offlineTransactions, result, chain);
                return result;
            }

            log.info("Chain validation passed for user: {}", userId);

            // Step 3: Validate each transaction payload
            List<OfflineTransaction> validTransactions = new ArrayList<>();
            for (OfflineTransaction transaction : offlineTransactions) {
                PayloadValidationResult payloadValidation = validationService.validateTransactionPayload(transaction);

                if (!payloadValidation.isValid()) {
                    log.warn("Payload validation failed for transaction: {}, errors: {}",
                            transaction.getTransactionHash(), payloadValidation.getErrors());
                    handlePayloadValidationFailure(transaction, payloadValidation, result, chain);
                } else {
                    validTransactions.add(transaction);
                }
            }

            // Step 4: Detect double spending
            DoubleSpendingReport doubleSpendingReport = validationService.detectDoubleSpending(
                    userUuid, validTransactions);

            if (doubleSpendingReport.isHasDoubleSpending()) {
                log.warn("Double spending detected for user: {}, flagged transactions: {}",
                        userId, doubleSpendingReport.getFlaggedTransactions());
                handleDoubleSpending(doubleSpendingReport, validTransactions, result, chain);
            }

            // Step 5: Check for duplicate transactions already synced
            List<OfflineTransaction> nonDuplicateTransactions = filterDuplicates(validTransactions, result, chain);

            // Step 6: Process valid transactions in chronological order
            List<OfflineTransaction> sortedTransactions = nonDuplicateTransactions.stream()
                    .sorted(Comparator.comparing(OfflineTransaction::getTimestamp))
                    .collect(Collectors.toList());

            log.info("Processing {} valid transactions for user: {}", sortedTransactions.size(), userId);

            int index = 0;
            for (OfflineTransaction transaction : sortedTransactions) {
                try {
                    processTransaction(transaction, user, result, chain, index);
                    index++;
                } catch (Exception e) {
                    log.error("Error processing transaction: {}, error: {}",
                            transaction.getTransactionHash(), e.getMessage(), e);
                    handleTransactionProcessingError(transaction, e, result, chain, index);
                    index++;
                }
            }

            // Step 7: Update chain metadata
            updateChainMetadata(chain, result, offlineTransactions);

            // Step 8: Set final balance
            result.setFinalBalance(user.getBalance());
            result.setSuccess(result.getSuccessCount() > 0);

            log.info("Sync completed for user: {}, success: {}, failed: {}, conflicts: {}",
                    userId, result.getSuccessCount(), result.getFailedCount(), result.getConflictCount());

            return result;

        } catch (Exception e) {
            log.error("Critical error during sync for user: {}, error: {}", userId, e.getMessage(), e);
            return SyncResult.failure(userId, offlineTransactions.size(),
                    "Critical sync error: " + e.getMessage());
        }
    }

    /**
     * Get or create chain metadata for user
     */
    private OfflineTransactionChain getOrCreateChain(UUID userId, String phoneNumber) {
        return offlineTransactionChainRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new chain for user: {}", userId);
                    OfflineTransactionChain newChain = new OfflineTransactionChain();
                    newChain.setUserId(userId);
                    newChain.setUserPhoneNumber(phoneNumber);
                    newChain.setGenesisHash("0000000000000000000000000000000000000000000000000000000000000000");
                    newChain.setCurrentHeadHash(newChain.getGenesisHash());
                    newChain.setLastSyncedHash(newChain.getGenesisHash());
                    newChain.setPendingCount(0);
                    newChain.setTotalTransactions(0);
                    newChain.setSyncedCount(0);
                    newChain.setFailedCount(0);
                    newChain.setConflictCount(0);
                    newChain.setChainValid(true);
                    newChain.setCreatedAt(LocalDateTime.now());
                    return offlineTransactionChainRepository.save(newChain);
                });
    }

    /**
     * Process a single valid transaction
     */
    private void processTransaction(OfflineTransaction transaction, User sender,
                                    SyncResult result, OfflineTransactionChain chain, int index) {

        log.debug("Processing transaction: {}, amount: {}, recipient: {}",
                transaction.getTransactionHash(), transaction.getAmount(),
                transaction.getRecipientPhoneNumber());

        // Find or create recipient
        User recipient = userRepository.findByPhoneNumber(transaction.getRecipientPhoneNumber())
                .orElseGet(() -> {
                    log.info("Recipient not found, creating placeholder: {}",
                            transaction.getRecipientPhoneNumber());
                    // In production, this might queue for manual review or send invitation
                    return createPlaceholderUser(transaction.getRecipientPhoneNumber());
                });

        // Check if sender has sufficient balance
        if (transaction.getSenderPhoneNumber().equals(sender.getPhoneNumber())) {
            if (sender.getBalance().compareTo(transaction.getAmount()) < 0) {
                log.warn("Insufficient balance for transaction: {}, required: {}, available: {}",
                        transaction.getTransactionHash(), transaction.getAmount(), sender.getBalance());

                createConflict(transaction, ConflictType.INSUFFICIENT_FUNDS,
                        "Insufficient balance at sync time",
                        transaction.getAmount(), sender.getBalance(), chain);

                markTransactionFailed(transaction, "Insufficient balance", result, index);
                return;
            }
        }

        // Execute transfer using PaymentService
        try {
            // Create transaction request
            String description = "Offline transaction sync: " + transaction.getTransactionHash();

            // Execute transfer
            TransactionResult transferResult = paymentService.transfer(
                    sender.getId().toString(),
                    recipient.getId().toString(),
                    transaction.getAmount().doubleValue(),
                    description
            );

            // Check if transfer was successful
            if (!transferResult.isSuccess()) {
                throw new RuntimeException("Transfer failed: " + transferResult.getMessage());
            }

            // Create a Transaction record for the offline transaction
            Transaction onlineTransaction = new Transaction();
            onlineTransaction.setUserId(sender.getId());
            onlineTransaction.setType(TransactionType.DEBIT);
            onlineTransaction.setCategory(TransactionCategory.TRANSFER);
            onlineTransaction.setAmount(transaction.getAmount());
            onlineTransaction.setDescription(description);
            onlineTransaction.setBalanceAfter(sender.getBalance());
            onlineTransaction.setReference("OFFLINE-" + transaction.getTransactionHash());
            onlineTransaction.setStatus(TransactionStatus.COMPLETED);
            onlineTransaction.setIsOffline(true);
            onlineTransaction.setOfflineTransactionId(transaction.getId());
            onlineTransaction.setSenderPhoneNumber(transaction.getSenderPhoneNumber());
            onlineTransaction.setRecipientPhoneNumber(transaction.getRecipientPhoneNumber());
            Transaction savedOnlineTransaction = transactionRepository.save(onlineTransaction);

            // Mark offline transaction as synced
            transaction.markAsSynced(savedOnlineTransaction.getId());
            transaction.setSyncedAt(LocalDateTime.now());
            offlineTransactionRepository.save(transaction);

            // Update chain
            chain.markTransactionSynced(transaction.getTransactionHash());
            chain.setCurrentHeadHash(transaction.getTransactionHash());

            // Add to result
            SyncResult.SyncedTransaction syncedTx = SyncResult.SyncedTransaction.builder()
                    .offlineTransactionId(transaction.getId())
                    .onlineTransactionId(transferResult.getTransaction().getId())
                    .transactionHash(transaction.getTransactionHash())
                    .senderPhoneNumber(transaction.getSenderPhoneNumber())
                    .recipientPhoneNumber(transaction.getRecipientPhoneNumber())
                    .amount(transaction.getAmount())
                    .offlineTimestamp(transaction.getTimestamp())
                    .syncedTimestamp(LocalDateTime.now())
                    .description(description)
                    .build();

            result.addSyncedTransaction(syncedTx);

            log.info("Transaction synced successfully: {} -> Online ID: {}",
                    transaction.getTransactionHash(), transferResult.getTransaction().getId());

        } catch (Exception e) {
            log.error("Error executing transfer for transaction: {}, error: {}",
                    transaction.getTransactionHash(), e.getMessage(), e);
            throw e; // Re-throw to be handled by caller
        }
    }

    /**
     * Create placeholder user for unknown recipients
     */
    private User createPlaceholderUser(String phoneNumber) {
        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setFullName("Pending User - " + phoneNumber);
        user.setEmail(phoneNumber + "@pending.fulus.ai");
        user.setPassword("PENDING_VERIFICATION");
        user.setBalance(BigDecimal.ZERO);
        user.setPin("0000");
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Handle chain validation failure
     */
    private void handleChainValidationFailure(ChainValidationResult chainValidation,
                                              List<OfflineTransaction> transactions,
                                              SyncResult result, OfflineTransactionChain chain) {

        log.warn("Handling chain validation failure, error count: {}", chainValidation.getErrorCount());

        for (ChainValidationResult.ChainValidationError error : chainValidation.getErrors()) {
            // Find the transaction by ID
            OfflineTransaction transaction = transactions.stream()
                    .filter(tx -> tx.getId().equals(error.getTransactionId()))
                    .findFirst()
                    .orElse(null);

            if (transaction != null) {
                // Create conflict
                ConflictType conflictType = mapErrorTypeToConflictType(error.getErrorType());
                createConflict(transaction, conflictType, error.getErrorMessage(),
                        error.getExpectedValue(), error.getActualValue(), chain);

                // Mark as failed
                markTransactionFailed(transaction, error.getErrorMessage(), result,
                        error.getTransactionIndex());
            }
        }

        chain.setChainValid(false);
        offlineTransactionChainRepository.save(chain);
    }

    /**
     * Handle payload validation failure
     */
    private void handlePayloadValidationFailure(OfflineTransaction transaction,
                                                PayloadValidationResult payloadValidation,
                                                SyncResult result, OfflineTransactionChain chain) {

        String errorMessage = String.join("; ", payloadValidation.getErrors());

        ConflictType conflictType;
        if (!payloadValidation.isSignatureValid()) {
            conflictType = ConflictType.INVALID_SIGNATURE;
        } else if (!payloadValidation.isNonceUnique()) {
            conflictType = ConflictType.NONCE_REUSED;
        } else if (!payloadValidation.isTimestampValid()) {
            conflictType = ConflictType.TIMESTAMP_INVALID;
        } else {
            conflictType = ConflictType.CHAIN_BROKEN; // Generic
        }

        createConflict(transaction, conflictType, errorMessage, null, null, chain);
        markTransactionFailed(transaction, errorMessage, result, null);
    }

    /**
     * Handle double spending detection
     */
    private void handleDoubleSpending(DoubleSpendingReport report,
                                     List<OfflineTransaction> transactions,
                                     SyncResult result, OfflineTransactionChain chain) {

        log.warn("Handling double spending, flagged count: {}", report.getFlaggedTransactions());

        for (DoubleSpendingReport.FlaggedTransaction flagged : report.getFlaggedTransactionList()) {
            // Find the transaction by ID
            OfflineTransaction transaction = transactions.stream()
                    .filter(tx -> tx.getId().equals(flagged.getTransactionId()))
                    .findFirst()
                    .orElse(null);

            if (transaction != null) {
                createConflict(transaction, ConflictType.DOUBLE_SPEND,
                        flagged.getReason(),
                        flagged.getBalanceBeforeTransaction(),
                        flagged.getBalanceAfterTransaction(), chain);

                markTransactionFailed(transaction, flagged.getReason(), result,
                        flagged.getTransactionIndex());
            }
        }
    }

    /**
     * Filter out duplicate transactions already synced
     */
    private List<OfflineTransaction> filterDuplicates(List<OfflineTransaction> transactions,
                                                      SyncResult result, OfflineTransactionChain chain) {

        List<OfflineTransaction> nonDuplicates = new ArrayList<>();

        for (OfflineTransaction transaction : transactions) {
            // Check if transaction hash already exists in synced transactions
            boolean isDuplicate = offlineTransactionRepository
                    .findByTransactionHash(transaction.getTransactionHash())
                    .map(existing -> existing.getSyncStatus() == SyncStatus.SYNCED)
                    .orElse(false);

            if (isDuplicate) {
                log.warn("Duplicate transaction detected: {}", transaction.getTransactionHash());

                createConflict(transaction, ConflictType.DOUBLE_SPEND,
                        "Transaction already synced", null, null, chain);

                markTransactionFailed(transaction, "Duplicate transaction already synced", result, null);
            } else {
                nonDuplicates.add(transaction);
            }
        }

        return nonDuplicates;
    }

    /**
     * Handle transaction processing error
     */
    private void handleTransactionProcessingError(OfflineTransaction transaction, Exception e,
                                                  SyncResult result, OfflineTransactionChain chain,
                                                  int index) {

        String errorMessage = "Processing error: " + e.getMessage();

        createConflict(transaction, ConflictType.CHAIN_BROKEN, errorMessage, null, null, chain);
        markTransactionFailed(transaction, errorMessage, result, index);
    }

    /**
     * Create sync conflict record
     */
    private void createConflict(OfflineTransaction transaction, ConflictType conflictType,
                               String description, Object expectedValue, Object actualValue,
                               OfflineTransactionChain chain) {

        SyncConflict conflict = new SyncConflict();
        conflict.setTransactionId(transaction.getId());
        conflict.setUserId(UUID.fromString(chain.getUserId().toString()));
        conflict.setConflictType(conflictType);
        conflict.setConflictDescription(description);
        conflict.setTransactionHash(transaction.getTransactionHash());
        conflict.setTransactionAmount(transaction.getAmount());
        conflict.setDetectedAt(LocalDateTime.now());
        conflict.setResolutionStatus(ResolutionStatus.UNRESOLVED);
        conflict.setAutoResolutionAttempted(false);

        if (expectedValue != null) {
            conflict.setExpectedValue(expectedValue.toString());
        }
        if (actualValue != null) {
            conflict.setActualValue(actualValue.toString());
        }

        // Set priority based on conflict type
        conflict.setPriority(getPriorityForConflictType(conflictType));

        syncConflictRepository.save(conflict);
        chain.incrementConflict();

        log.info("Conflict created: type={}, transaction={}", conflictType, transaction.getTransactionHash());
    }

    /**
     * Mark transaction as failed
     */
    private void markTransactionFailed(OfflineTransaction transaction, String errorMessage,
                                      SyncResult result, Integer index) {

        transaction.markAsFailed(errorMessage);
        offlineTransactionRepository.save(transaction);

        SyncResult.FailedTransaction failedTx = SyncResult.FailedTransaction.builder()
                .offlineTransactionId(transaction.getId())
                .transactionHash(transaction.getTransactionHash())
                .senderPhoneNumber(transaction.getSenderPhoneNumber())
                .recipientPhoneNumber(transaction.getRecipientPhoneNumber())
                .amount(transaction.getAmount())
                .failureReason(errorMessage)
                .errorDetails(transaction.getSyncError())
                .attemptedAt(LocalDateTime.now())
                .transactionIndex(index)
                .build();

        result.addFailedTransaction(failedTx);
    }

    /**
     * Update chain metadata after sync
     */
    private void updateChainMetadata(OfflineTransactionChain chain, SyncResult result,
                                    List<OfflineTransaction> transactions) {

        if (result.getSuccessCount() > 0) {
            // Find last successfully synced transaction
            OfflineTransaction lastSynced = transactions.stream()
                    .filter(tx -> tx.getSyncStatus() == SyncStatus.SYNCED)
                    .max(Comparator.comparing(OfflineTransaction::getTimestamp))
                    .orElse(null);

            if (lastSynced != null) {
                chain.setLastSyncedHash(lastSynced.getTransactionHash());
                chain.setLastSyncedAt(LocalDateTime.now());
                result.setLastSyncedHash(lastSynced.getTransactionHash());
            }
        }

        // Update pending count
        int pendingCount = offlineTransactionRepository.countByUserPhoneNumberAndSyncStatus(
                chain.getUserPhoneNumber(), SyncStatus.PENDING);
        chain.setPendingCount(pendingCount);

        // Update failed count
        chain.setFailedCount(chain.getFailedCount() + result.getFailedCount());

        chain.setUpdatedAt(LocalDateTime.now());
        offlineTransactionChainRepository.save(chain);

        log.debug("Chain metadata updated: lastHash={}, pending={}",
                chain.getLastSyncedHash(), chain.getPendingCount());
    }

    /**
     * Map error type to conflict type
     */
    private ConflictType mapErrorTypeToConflictType(String errorType) {
        return switch (errorType) {
            case "DUPLICATE_HASH", "DUPLICATE_NONCE" -> ConflictType.DOUBLE_SPEND;
            case "CHAIN_BROKEN", "INVALID_CHRONOLOGY" -> ConflictType.CHAIN_BROKEN;
            case "INVALID_HASH" -> ConflictType.INVALID_HASH;
            case "INVALID_SIGNATURE" -> ConflictType.INVALID_SIGNATURE;
            default -> ConflictType.CHAIN_BROKEN;
        };
    }

    /**
     * Get priority for conflict type
     */
    private Integer getPriorityForConflictType(ConflictType conflictType) {
        return switch (conflictType) {
            case DOUBLE_SPEND -> 1;
            case INSUFFICIENT_FUNDS -> 2;
            case INVALID_SIGNATURE -> 1;
            case NONCE_REUSED -> 1;
            case INVALID_HASH -> 2;
            case CHAIN_BROKEN -> 3;
            case TIMESTAMP_INVALID -> 4;
        };
    }

    /**
     * Get chain statistics for user
     */
    public Map<String, Object> getChainStatistics(UUID userId) {
        OfflineTransactionChain chain = offlineTransactionChainRepository.findByUserId(userId)
                .orElse(null);

        if (chain == null) {
            return Map.of("exists", false);
        }

        return Map.of(
                "exists", true,
                "totalTransactions", chain.getTotalTransactions(),
                "syncedCount", chain.getSyncedCount(),
                "failedCount", chain.getFailedCount(),
                "pendingCount", chain.getPendingCount(),
                "conflictCount", chain.getConflictCount(),
                "chainValid", chain.isChainValid(),
                "lastSyncedHash", chain.getLastSyncedHash(),
                "lastSyncedAt", chain.getLastSyncedAt()
        );
    }

    /**
     * Get unresolved conflicts for user
     */
    public List<SyncConflict> getUnresolvedConflicts(UUID userId) {
        return syncConflictRepository.findUnresolvedConflictsByUser(userId);
    }

    /**
     * Retry failed transactions
     */
    @Transactional
    public SyncResult retryFailedTransactions(String userId) {
        log.info("Retrying failed transactions for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<OfflineTransaction> failedTransactions = offlineTransactionRepository
                .findByUserPhoneNumberAndSyncStatus(user.getPhoneNumber(), SyncStatus.FAILED);

        log.info("Found {} failed transactions to retry", failedTransactions.size());

        return syncOfflineTransactions(userId, failedTransactions);
    }
}
