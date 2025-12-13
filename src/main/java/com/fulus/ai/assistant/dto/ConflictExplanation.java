package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plain English explanation of sync conflict for AI assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictExplanation {

    private UUID conflictId;
    private UUID transactionId;
    private String transactionHash;
    private String conflictType;
    private BigDecimal transactionAmount;
    private LocalDateTime detectedAt;
    private String explanation; // Plain English explanation
    private String technicalDetails; // Technical details for context

    @Builder.Default
    private List<String> resolutionSteps = new ArrayList<>();

    /**
     * Generate plain English explanation based on conflict type
     */
    public static ConflictExplanation fromConflict(com.fulus.ai.assistant.entity.SyncConflict conflict) {
        ConflictExplanation explanation = new ConflictExplanation();
        explanation.setConflictId(conflict.getId());
        explanation.setTransactionId(conflict.getTransactionId());
        explanation.setTransactionHash(conflict.getTransactionHash());
        explanation.setConflictType(conflict.getConflictType().name());
        explanation.setTransactionAmount(conflict.getTransactionAmount());
        explanation.setDetectedAt(conflict.getDetectedAt());

        // Generate plain English explanation
        String plainExplanation = generateExplanation(conflict);
        explanation.setExplanation(plainExplanation);

        // Set technical details
        explanation.setTechnicalDetails(conflict.getConflictDescription());

        // Generate resolution steps
        List<String> steps = generateResolutionSteps(conflict);
        explanation.setResolutionSteps(steps);

        return explanation;
    }

    /**
     * Generate plain English explanation
     */
    private static String generateExplanation(com.fulus.ai.assistant.entity.SyncConflict conflict) {
        switch (conflict.getConflictType()) {
            case DOUBLE_SPEND:
                return String.format(
                        "Your offline payment of ₦%s couldn't sync because it looks like you tried to spend the same money twice. " +
                                "This happens when you make multiple offline payments without syncing, and you don't have enough balance to cover all of them.",
                        conflict.getTransactionAmount()
                );

            case INSUFFICIENT_FUNDS:
                return String.format(
                        "Your offline payment of ₦%s failed because you don't have enough money in your account. " +
                                "Your balance at the time of sync was ₦%s, but you needed ₦%s.",
                        conflict.getTransactionAmount(),
                        conflict.getActualBalance(),
                        conflict.getExpectedBalance()
                );

            case INVALID_SIGNATURE:
                return "Your offline payment couldn't be verified because the digital signature doesn't match. " +
                        "This could mean the payment data was corrupted or tampered with. For your security, we rejected this transaction.";

            case NONCE_REUSED:
                return "Your offline payment was rejected because it appears to be a duplicate (replay attack detected). " +
                        "The unique identifier (nonce) for this payment has already been used. This protects you from processing the same payment twice.";

            case INVALID_HASH:
                return "Your offline payment couldn't sync because the transaction hash doesn't match the data. " +
                        "This could mean the payment data was corrupted during storage or transmission.";

            case CHAIN_BROKEN:
                return "Your offline payment couldn't sync because it doesn't properly link to your previous transactions. " +
                        "This is part of our security system that ensures all your offline payments are in the correct order.";

            case TIMESTAMP_INVALID:
                return String.format(
                        "Your offline payment of ₦%s couldn't sync because the timestamp is invalid. " +
                                "The payment might be too old or the time on your device might be incorrect.",
                        conflict.getTransactionAmount()
                );

            default:
                return String.format(
                        "Your offline payment of ₦%s encountered a sync issue: %s",
                        conflict.getTransactionAmount(),
                        conflict.getConflictDescription()
                );
        }
    }

    /**
     * Generate resolution steps
     */
    private static List<String> generateResolutionSteps(com.fulus.ai.assistant.entity.SyncConflict conflict) {
        List<String> steps = new ArrayList<>();

        switch (conflict.getConflictType()) {
            case DOUBLE_SPEND:
                steps.add("Check your account balance to ensure you have enough funds");
                steps.add("Review all your pending offline transactions");
                steps.add("Consider canceling or reducing some offline payments");
                steps.add("Add money to your account if needed");
                break;

            case INSUFFICIENT_FUNDS:
                steps.add("Add money to your account to cover this payment");
                steps.add("Once you have sufficient balance, the transaction will be retried automatically");
                steps.add("Alternatively, you can cancel this transaction");
                break;

            case INVALID_SIGNATURE:
                steps.add("This transaction cannot be recovered as it may have been tampered with");
                steps.add("Contact support if you believe this is an error");
                steps.add("Consider re-creating the payment if the recipient didn't receive it");
                break;

            case NONCE_REUSED:
                steps.add("This is a duplicate transaction that was already processed");
                steps.add("Check your transaction history to confirm if the original payment went through");
                steps.add("No action needed - this protects you from paying twice");
                break;

            case INVALID_HASH:
                steps.add("This transaction data appears to be corrupted");
                steps.add("Contact support with the transaction hash for investigation");
                steps.add("You may need to re-create this payment");
                break;

            case CHAIN_BROKEN:
                steps.add("Try syncing your older transactions first");
                steps.add("Ensure your app is up to date");
                steps.add("If the problem persists, contact support");
                break;

            case TIMESTAMP_INVALID:
                steps.add("Check that your device's date and time are set correctly");
                steps.add("Enable automatic time setting on your device");
                steps.add("Try syncing again after correcting the time");
                break;

            default:
                steps.add("Contact support for assistance with this specific issue");
                steps.add("Provide the transaction hash: " + conflict.getTransactionHash());
                break;
        }

        return steps;
    }
}
