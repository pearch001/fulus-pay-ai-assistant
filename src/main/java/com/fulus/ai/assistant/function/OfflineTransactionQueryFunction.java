package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.ConflictExplanation;
import com.fulus.ai.assistant.dto.OfflineTransactionSummary;
import com.fulus.ai.assistant.entity.OfflineTransaction;
import com.fulus.ai.assistant.entity.SyncConflict;
import com.fulus.ai.assistant.enums.SyncStatus;
import com.fulus.ai.assistant.repository.OfflineTransactionRepository;
import com.fulus.ai.assistant.repository.SyncConflictRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spring AI Function Tool for querying offline transactions
 * Enables the AI assistant to answer questions about offline transactions and sync conflicts
 */
@Slf4j
@Component("offlineTransactionQueryFunction")
@RequiredArgsConstructor
public class OfflineTransactionQueryFunction implements Function<OfflineTransactionQueryFunction.Request, String> {

    private final OfflineTransactionRepository offlineTransactionRepository;
    private final SyncConflictRepository syncConflictRepository;
    private final UserRepository userRepository;

    /**
     * Main function entry point for Spring AI
     */
    @Override
    public String apply(Request request) {
        try {
            log.debug("AI Function called: offlineTransactionQuery - action: {}, userId: {}",
                    request.action, request.userId);

            if ("query".equalsIgnoreCase(request.action)) {
                return queryOfflineTransactions(request.userId, request.syncStatus);
            } else if ("explain".equalsIgnoreCase(request.action)) {
                return explainSyncConflict(request.userId, request.transactionId);
            } else {
                return "Unknown action: " + request.action;
            }

        } catch (Exception e) {
            log.error("Error in offline transaction query function", e);
            return "I encountered an error while retrieving your offline transaction information: " + e.getMessage();
        }
    }

    /**
     * Query offline transactions for a user
     *
     * @param userId     User ID
     * @param syncStatus Sync status filter: "pending", "synced", "failed", "all"
     * @return Plain English summary of transactions
     */
    public String queryOfflineTransactions(String userId, String syncStatus) {
        try {
            log.info("Querying offline transactions: userId={}, status={}", userId, syncStatus);

            UUID userUuid = UUID.fromString(userId);

            // Get user's phone number
            String phoneNumber = userRepository.findById(userUuid)
                    .map(user -> user.getPhoneNumber())
                    .orElse(null);

            if (phoneNumber == null) {
                return "I couldn't find your account information.";
            }

            // Get transactions based on status
            List<OfflineTransaction> transactions;
            SyncStatus status = parseSyncStatus(syncStatus);

            if (status == null || "all".equalsIgnoreCase(syncStatus)) {
                transactions = offlineTransactionRepository.getTransactionChain(phoneNumber);
            } else {
                transactions = offlineTransactionRepository
                        .findByUserPhoneNumberAndSyncStatus(phoneNumber, status);
            }

            // Build summary
            OfflineTransactionSummary summary = buildSummary(userId, syncStatus, transactions, phoneNumber);

            // Generate and return plain English summary
            return summary.generateSummary();

        } catch (Exception e) {
            log.error("Error querying offline transactions", e);
            return "I encountered an error while checking your offline transactions: " + e.getMessage();
        }
    }

    /**
     * Explain a sync conflict in plain English
     *
     * @param userId        User ID
     * @param transactionId Transaction ID with conflict
     * @return Plain English explanation with resolution steps
     */
    public String explainSyncConflict(String userId, String transactionId) {
        try {
            log.info("Explaining sync conflict: userId={}, transactionId={}", userId, transactionId);

            UUID userUuid = UUID.fromString(userId);
            UUID txUuid = UUID.fromString(transactionId);

            // Find conflict for this transaction
            List<SyncConflict> conflicts = syncConflictRepository
                    .findByTransactionIdOrderByDetectedAtDesc(txUuid);

            if (conflicts.isEmpty()) {
                // Check if transaction exists
                OfflineTransaction transaction = offlineTransactionRepository.findById(txUuid)
                        .orElse(null);

                if (transaction == null) {
                    return "I couldn't find that transaction.";
                }

                if (transaction.getSyncStatus() == SyncStatus.SYNCED) {
                    return String.format(
                            "This transaction of ₦%s was successfully synced on %s. There are no conflicts.",
                            transaction.getAmount(),
                            transaction.getSyncedAt()
                    );
                }

                if (transaction.getSyncStatus() == SyncStatus.PENDING) {
                    return String.format(
                            "This transaction of ₦%s is still pending sync. It hasn't encountered any conflicts yet.",
                            transaction.getAmount()
                    );
                }

                if (transaction.getSyncStatus() == SyncStatus.FAILED) {
                    String errorMsg = transaction.getSyncError() != null ?
                            transaction.getSyncError() : "Unknown error";
                    return String.format(
                            "This transaction of ₦%s failed to sync with the following error: %s",
                            transaction.getAmount(),
                            errorMsg
                    );
                }

                return "This transaction doesn't have any recorded conflicts.";
            }

            // Get the most recent conflict
            SyncConflict conflict = conflicts.get(0);

            // Generate explanation
            ConflictExplanation explanation = ConflictExplanation.fromConflict(conflict);

            // Build response
            StringBuilder response = new StringBuilder();
            response.append(explanation.getExplanation());
            response.append("\n\n");

            // Add resolution steps
            if (!explanation.getResolutionSteps().isEmpty()) {
                response.append("Here's what you can do:\n");
                int step = 1;
                for (String resolutionStep : explanation.getResolutionSteps()) {
                    response.append(step++).append(". ").append(resolutionStep).append("\n");
                }
            }

            return response.toString().trim();

        } catch (Exception e) {
            log.error("Error explaining sync conflict", e);
            return "I encountered an error while explaining the sync conflict: " + e.getMessage();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Parse sync status string to enum
     */
    private SyncStatus parseSyncStatus(String syncStatus) {
        if (syncStatus == null || "all".equalsIgnoreCase(syncStatus)) {
            return null;
        }

        try {
            return SyncStatus.valueOf(syncStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sync status: {}", syncStatus);
            return null;
        }
    }

    /**
     * Build transaction summary
     */
    private OfflineTransactionSummary buildSummary(String userId, String syncStatus,
                                                   List<OfflineTransaction> transactions,
                                                   String phoneNumber) {

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        LocalDateTime oldestPending = null;
        LocalDateTime newest = null;

        for (OfflineTransaction tx : transactions) {
            totalAmount = totalAmount.add(tx.getAmount());

            // Check if debit or credit
            if (tx.getSenderPhoneNumber().equals(phoneNumber)) {
                totalDebits = totalDebits.add(tx.getAmount());
            } else {
                totalCredits = totalCredits.add(tx.getAmount());
            }

            // Track oldest pending
            if (tx.getSyncStatus() == SyncStatus.PENDING &&
                    (oldestPending == null || tx.getTimestamp().isBefore(oldestPending))) {
                oldestPending = tx.getTimestamp();
            }

            // Track newest
            if (newest == null || tx.getTimestamp().isAfter(newest)) {
                newest = tx.getTimestamp();
            }
        }

        // Count by status
        long pendingCount = transactions.stream()
                .filter(tx -> tx.getSyncStatus() == SyncStatus.PENDING)
                .count();

        long syncedCount = transactions.stream()
                .filter(tx -> tx.getSyncStatus() == SyncStatus.SYNCED)
                .count();

        long failedCount = transactions.stream()
                .filter(tx -> tx.getSyncStatus() == SyncStatus.FAILED)
                .count();

        return OfflineTransactionSummary.builder()
                .userId(userId)
                .syncStatus(syncStatus)
                .transactionCount(transactions.size())
                .totalAmount(totalAmount)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .oldestPendingDate(oldestPending)
                .newestTransactionDate(newest)
                .pendingCount((int) pendingCount)
                .syncedCount((int) syncedCount)
                .failedCount((int) failedCount)
                .build();
    }

    /**
     * Request object for Spring AI function calling
     */
    public static class Request {
        public String action; // "query" or "explain"
        public String userId;
        public String syncStatus; // For query: "pending", "synced", "failed", "all"
        public String transactionId; // For explain
    }
}
