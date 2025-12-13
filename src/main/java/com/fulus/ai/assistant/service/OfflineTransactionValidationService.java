package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.ChainValidationResult;
import com.fulus.ai.assistant.dto.DoubleSpendingReport;
import com.fulus.ai.assistant.dto.PayloadValidationResult;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.OfflineTransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.util.PayloadEncryptionUtil;
import com.fulus.ai.assistant.util.TransactionHashUtil;
import com.fulus.ai.assistant.util.TransactionSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating offline transactions
 *
 * Provides comprehensive validation including:
 * - Hash chain integrity
 * - Payload decryption and verification
 * - Signature verification
 * - Double-spending detection
 * - Replay attack prevention
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineTransactionValidationService {

    private final OfflineTransactionRepository offlineTransactionRepository;
    private final UserRepository userRepository;

    @Value("${offline.transaction.max-age-days:30}")
    private int maxTransactionAgeDays;

    @Value("${offline.transaction.future-tolerance-minutes:5}")
    private int futureToleranceMinutes;

    /**
     * Validate transaction chain integrity
     *
     * Verifies:
     * - Hash chain integrity (previousHash links)
     * - No duplicate hashes (replay prevention)
     * - Chronological ordering
     * - Hash calculation correctness
     *
     * @param userId User ID
     * @param transactions List of offline transactions (ordered by timestamp)
     * @return Validation result with detailed errors
     */
    public ChainValidationResult validateTransactionChain(UUID userId, List<OfflineTransaction> transactions) {
        log.info("Validating transaction chain for user: {} with {} transactions", userId, transactions.size());

        if (transactions == null || transactions.isEmpty()) {
            log.info("No transactions to validate for user: {}", userId);
            return ChainValidationResult.success(userId.toString(), 0);
        }

        // Sort by timestamp
        List<OfflineTransaction> sortedTransactions = transactions.stream()
                .sorted(Comparator.comparing(OfflineTransaction::getTimestamp))
                .collect(Collectors.toList());

        List<ChainValidationResult.ChainValidationError> errors = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();
        Set<String> seenNonces = new HashSet<>();

        // Validate each transaction
        for (int i = 0; i < sortedTransactions.size(); i++) {
            OfflineTransaction current = sortedTransactions.get(i);

            log.debug("Validating transaction {}/{}: hash={}", i + 1, sortedTransactions.size(),
                    current.getTransactionHash());

            // 1. Check for duplicate transaction hash
            if (seenHashes.contains(current.getTransactionHash())) {
                errors.add(createError(
                        current, i,
                        "DUPLICATE_HASH",
                        "Duplicate transaction hash detected - possible replay attack",
                        null,
                        current.getTransactionHash()
                ));
                log.warn("SECURITY ALERT: Duplicate hash detected for user {} at index {}: {}",
                        userId, i, current.getTransactionHash());
            } else {
                seenHashes.add(current.getTransactionHash());
            }

            // 2. Check for duplicate nonce
            if (seenNonces.contains(current.getNonce())) {
                errors.add(createError(
                        current, i,
                        "DUPLICATE_NONCE",
                        "Duplicate nonce detected - replay attack",
                        null,
                        current.getNonce()
                ));
                log.warn("SECURITY ALERT: Duplicate nonce detected for user {} at index {}: {}",
                        userId, i, current.getNonce());
            } else {
                seenNonces.add(current.getNonce());
            }

            // 3. Verify hash chain link
            if (i > 0) {
                OfflineTransaction previous = sortedTransactions.get(i - 1);

                if (!current.getPreviousHash().equals(previous.getTransactionHash())) {
                    errors.add(createError(
                            current, i,
                            "CHAIN_BROKEN",
                            "Previous hash does not match previous transaction hash",
                            previous.getTransactionHash(),
                            current.getPreviousHash()
                    ));
                    log.error("Chain broken for user {} at index {}: expected={}, actual={}",
                            userId, i, previous.getTransactionHash(), current.getPreviousHash());
                }
            } else {
                // First transaction should link to genesis hash
                String genesisHash = TransactionHashUtil.getGenesisHash();
                if (!current.getPreviousHash().equals(genesisHash)) {
                    errors.add(createError(
                            current, i,
                            "INVALID_GENESIS",
                            "First transaction must link to genesis hash",
                            genesisHash,
                            current.getPreviousHash()
                    ));
                    log.error("Invalid genesis hash for user {} at index {}: expected={}, actual={}",
                            userId, i, genesisHash, current.getPreviousHash());
                }
            }

            // 4. Verify hash calculation
            String calculatedHash = TransactionHashUtil.generateTransactionHash(
                    current.getSenderPhoneNumber(),
                    current.getRecipientPhoneNumber(),
                    current.getAmount().toString(),
                    current.getTimestamp(),
                    current.getNonce(),
                    current.getPreviousHash()
            );

            if (!calculatedHash.equals(current.getTransactionHash())) {
                errors.add(createError(
                        current, i,
                        "INVALID_HASH",
                        "Transaction hash does not match calculated hash",
                        calculatedHash,
                        current.getTransactionHash()
                ));
                log.error("Invalid hash for user {} at index {}: expected={}, actual={}",
                        userId, i, calculatedHash, current.getTransactionHash());
            }

            // 5. Verify chronological ordering
            if (i > 0) {
                OfflineTransaction previous = sortedTransactions.get(i - 1);
                if (current.getTimestamp().isBefore(previous.getTimestamp())) {
                    errors.add(createError(
                            current, i,
                            "INVALID_CHRONOLOGY",
                            "Transaction timestamp is before previous transaction",
                            previous.getTimestamp().toString(),
                            current.getTimestamp().toString()
                    ));
                    log.error("Invalid chronology for user {} at index {}: current={}, previous={}",
                            userId, i, current.getTimestamp(), previous.getTimestamp());
                }
            }
        }

        // Build result
        if (errors.isEmpty()) {
            log.info("Chain validation SUCCESS for user {}: {} transactions validated", userId, transactions.size());
            return ChainValidationResult.success(userId.toString(), transactions.size());
        } else {
            log.warn("Chain validation FAILED for user {}: {} errors found in {} transactions",
                    userId, errors.size(), transactions.size());
            return ChainValidationResult.failure(userId.toString(), transactions.size(), errors);
        }
    }

    /**
     * Validate individual transaction payload
     *
     * Verifies:
     * - Payload decryption
     * - Signature verification
     * - Nonce uniqueness (database check)
     * - Amount validation (> 0)
     * - Timestamp validation (not too old, not in future)
     *
     * @param transaction Offline transaction to validate
     * @return Validation result with specific errors
     */
    public PayloadValidationResult validateTransactionPayload(OfflineTransaction transaction) {
        log.info("Validating payload for transaction: {} (hash: {})",
                transaction.getId(), transaction.getTransactionHash());

        PayloadValidationResult result = PayloadValidationResult.builder()
                .transactionId(transaction.getId())
                .transactionHash(transaction.getTransactionHash())
                .valid(true)
                .build();

        // 1. Validate amount
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Amount must be greater than zero");
            result.setAmountValid(false);
            log.error("Invalid amount for transaction {}: {}", transaction.getId(), transaction.getAmount());
        } else {
            result.setAmountValid(true);
        }

        // 2. Validate timestamp
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxAge = now.minusDays(maxTransactionAgeDays);
        LocalDateTime maxFuture = now.plusMinutes(futureToleranceMinutes);

        if (transaction.getTimestamp().isBefore(maxAge)) {
            result.addError(String.format("Transaction is too old (older than %d days)", maxTransactionAgeDays));
            result.setTimestampValid(false);
            log.warn("Transaction {} timestamp too old: {} (max age: {})",
                    transaction.getId(), transaction.getTimestamp(), maxAge);
        } else if (transaction.getTimestamp().isAfter(maxFuture)) {
            result.addError(String.format("Transaction timestamp is in the future (tolerance: %d minutes)",
                    futureToleranceMinutes));
            result.setTimestampValid(false);
            log.error("Transaction {} timestamp in future: {} (max future: {})",
                    transaction.getId(), transaction.getTimestamp(), maxFuture);
        } else {
            result.setTimestampValid(true);

            // Add warning if transaction is old (but within tolerance)
            long daysSinceCreation = ChronoUnit.DAYS.between(transaction.getTimestamp(), now);
            if (daysSinceCreation > 7) {
                result.addWarning(String.format("Transaction is %d days old", daysSinceCreation));
            }
        }

        // 3. Check nonce uniqueness (database check)
        boolean nonceExists = offlineTransactionRepository.existsByNonce(transaction.getNonce());
        if (nonceExists) {
            result.addError("Nonce already used - replay attack detected");
            result.setNonceUnique(false);
            log.error("SECURITY ALERT: Nonce reuse detected for transaction {}: {}",
                    transaction.getId(), transaction.getNonce());
        } else {
            result.setNonceUnique(true);
        }

        // 4. Decrypt and validate payload
        try {
            // For validation, we need the encryption key
            // In production, this would be derived from user's key
            // For now, we'll just check if payload is not empty
            if (transaction.getPayload() == null || transaction.getPayload().isEmpty()) {
                result.addError("Payload is empty");
                log.error("Empty payload for transaction {}", transaction.getId());
            } else {
                result.setDecryptedPayload("Payload present (not decrypted in validation)");
                // Note: Actual decryption would happen during sync with proper keys
            }
        } catch (Exception e) {
            result.addError("Failed to process payload: " + e.getMessage());
            log.error("Payload processing error for transaction {}: {}",
                    transaction.getId(), e.getMessage(), e);
        }

        // 5. Verify signature
        try {
            // Get user to derive secret key
            Optional<User> userOpt = userRepository.findByPhoneNumber(transaction.getSenderPhoneNumber());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Derive secret key (mock implementation)
                // In production, use user's public key for verification
                String secretKey = TransactionSignatureUtil.generateUserSecretKey(
                        user.getPhoneNumber(),
                        user.getPin() // This is already hashed
                );

                // Verify signature
                boolean signatureValid = TransactionSignatureUtil.verifySignature(
                        transaction.getTransactionHash(),
                        transaction.getSignatureKey(),
                        secretKey
                );

                result.setSignatureValid(signatureValid);

                if (!signatureValid) {
                    result.addError("Invalid signature - transaction may be forged");
                    log.error("SECURITY ALERT: Invalid signature for transaction {} (sender: {})",
                            transaction.getId(), transaction.getSenderPhoneNumber());
                } else {
                    log.debug("Signature verified for transaction {}", transaction.getId());
                }
            } else {
                result.addError("Sender not found: " + transaction.getSenderPhoneNumber());
                result.setSignatureValid(false);
                log.error("Sender not found for transaction {}: {}",
                        transaction.getId(), transaction.getSenderPhoneNumber());
            }
        } catch (Exception e) {
            result.addError("Signature verification failed: " + e.getMessage());
            result.setSignatureValid(false);
            log.error("Signature verification error for transaction {}: {}",
                    transaction.getId(), e.getMessage(), e);
        }

        // Log result
        if (result.isValid()) {
            log.info("Payload validation SUCCESS for transaction {}", transaction.getId());
        } else {
            log.warn("Payload validation FAILED for transaction {}: {} errors",
                    transaction.getId(), result.getErrors().size());
        }

        return result;
    }

    /**
     * Detect double-spending in offline transactions
     *
     * Calculates cumulative debits and compares against last known balance.
     * Flags transactions that would cause negative balance.
     *
     * @param userId User ID
     * @param pendingTransactions List of pending offline transactions
     * @return Report with flagged transactions
     */
    public DoubleSpendingReport detectDoubleSpending(UUID userId, List<OfflineTransaction> pendingTransactions) {
        log.info("Detecting double-spending for user {} with {} pending transactions",
                userId, pendingTransactions.size());

        // Get user's current balance
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        BigDecimal currentBalance = user.getBalance();
        log.debug("User {} current balance: ₦{}", userId, currentBalance);

        // Sort transactions by timestamp
        List<OfflineTransaction> sortedTransactions = pendingTransactions.stream()
                .sorted(Comparator.comparing(OfflineTransaction::getTimestamp))
                .collect(Collectors.toList());

        // Initialize report
        DoubleSpendingReport report = DoubleSpendingReport.builder()
                .userId(userId.toString())
                .lastKnownBalance(currentBalance)
                .totalDebits(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .totalTransactions(sortedTransactions.size())
                .flaggedTransactions(0)
                .hasDoubleSpending(false)
                .flaggedTransactionList(new ArrayList<>())
                .build();

        // Track running balance
        BigDecimal runningBalance = currentBalance;

        // Process each transaction
        for (int i = 0; i < sortedTransactions.size(); i++) {
            OfflineTransaction tx = sortedTransactions.get(i);

            // Determine transaction type (debit or credit)
            boolean isDebit = tx.getSenderPhoneNumber().equals(user.getPhoneNumber());
            BigDecimal amount = tx.getAmount();

            log.debug("Processing transaction {}/{}: hash={}, amount=₦{}, type={}",
                    i + 1, sortedTransactions.size(), tx.getTransactionHash(), amount,
                    isDebit ? "DEBIT" : "CREDIT");

            // Update totals
            if (isDebit) {
                report.setTotalDebits(report.getTotalDebits().add(amount));
                runningBalance = runningBalance.subtract(amount);

                // Check if balance goes negative
                if (runningBalance.compareTo(BigDecimal.ZERO) < 0) {
                    DoubleSpendingReport.FlaggedTransaction flagged = DoubleSpendingReport.FlaggedTransaction.builder()
                            .transactionId(tx.getId())
                            .transactionHash(tx.getTransactionHash())
                            .amount(amount)
                            .balanceBeforeTransaction(runningBalance.add(amount)) // Restore balance before this TX
                            .balanceAfterTransaction(runningBalance)
                            .reason(String.format("Insufficient funds: balance would be ₦%s after transaction", runningBalance))
                            .transactionIndex(i)
                            .build();

                    report.addFlaggedTransaction(flagged);

                    log.warn("DOUBLE-SPENDING DETECTED for user {} at transaction {}: " +
                            "balance before=₦{}, amount=₦{}, balance after=₦{}",
                            userId, i, flagged.getBalanceBeforeTransaction(), amount, runningBalance);
                }
            } else {
                report.setTotalCredits(report.getTotalCredits().add(amount));
                runningBalance = runningBalance.add(amount);
            }
        }

        // Calculate projected balance
        report.calculateProjectedBalance();

        // Log summary
        if (report.isHasDoubleSpending()) {
            log.error("Double-spending DETECTED for user {}: {} flagged transactions, " +
                    "total debits=₦{}, total credits=₦{}, projected balance=₦{}",
                    userId, report.getFlaggedTransactions(),
                    report.getTotalDebits(), report.getTotalCredits(), report.getProjectedBalance());
        } else {
            log.info("Double-spending check PASSED for user {}: " +
                    "total debits=₦{}, total credits=₦{}, projected balance=₦{}",
                    userId, report.getTotalDebits(), report.getTotalCredits(), report.getProjectedBalance());
        }

        return report;
    }

    /**
     * Comprehensive validation combining all checks
     *
     * @param userId User ID
     * @param transactions List of offline transactions
     * @return Map of validation results
     */
    public Map<String, Object> validateAllTransactions(UUID userId, List<OfflineTransaction> transactions) {
        log.info("Running comprehensive validation for user {} with {} transactions", userId, transactions.size());

        Map<String, Object> results = new HashMap<>();

        // 1. Validate chain
        ChainValidationResult chainResult = validateTransactionChain(userId, transactions);
        results.put("chainValidation", chainResult);

        // 2. Validate each payload
        List<PayloadValidationResult> payloadResults = new ArrayList<>();
        for (OfflineTransaction tx : transactions) {
            PayloadValidationResult payloadResult = validateTransactionPayload(tx);
            payloadResults.add(payloadResult);
        }
        results.put("payloadValidation", payloadResults);

        // 3. Detect double-spending
        DoubleSpendingReport doubleSpendingReport = detectDoubleSpending(userId, transactions);
        results.put("doubleSpendingReport", doubleSpendingReport);

        // 4. Overall result
        boolean overallValid = chainResult.isValid() &&
                               payloadResults.stream().allMatch(PayloadValidationResult::isValid) &&
                               !doubleSpendingReport.isHasDoubleSpending();

        results.put("overallValid", overallValid);
        results.put("totalTransactions", transactions.size());
        results.put("validTransactions", chainResult.getValidTransactions());

        log.info("Comprehensive validation complete for user {}: overallValid={}, " +
                "chainValid={}, payloadErrors={}, doubleSpending={}",
                userId, overallValid, chainResult.isValid(),
                payloadResults.stream().filter(p -> !p.isValid()).count(),
                doubleSpendingReport.isHasDoubleSpending());

        return results;
    }

    /**
     * Helper method to create chain validation error
     */
    private ChainValidationResult.ChainValidationError createError(
            OfflineTransaction tx, int index, String type, String message,
            String expected, String actual) {

        return ChainValidationResult.ChainValidationError.builder()
                .transactionId(tx.getId())
                .transactionHash(tx.getTransactionHash())
                .transactionIndex(index)
                .errorType(type)
                .errorMessage(message)
                .expectedValue(expected)
                .actualValue(actual)
                .build();
    }
}
