package com.fulus.ai.assistant.function;

import com.fulus.ai.assistant.dto.SendMoneyRequest;
import com.fulus.ai.assistant.dto.TransactionResult;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.UserRepository;
import com.fulus.ai.assistant.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spring AI Function Tool for sending money to other users
 * ⚠️ WARNING: This function executes real financial transactions
 */
@Component("sendMoneyFunction")
@Description("Send money to another user by phone number or name. This function executes REAL financial transactions. " +
        "Use extreme caution and ALWAYS confirm transaction details with the user before executing. " +
        "Returns confirmation with transaction reference and new balance. " +
        "Only use when the user explicitly requests to send money or transfer funds.")
@RequiredArgsConstructor
@Slf4j
public class SendMoneyFunction implements Function<SendMoneyRequest, String> {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    private static final String CURRENCY_SYMBOL = "₦";

    /**
     * Execute money transfer
     * ⚠️ This function performs REAL financial transactions
     *
     * @param request Request containing senderId, recipientIdentifier, amount, note
     * @return Confirmation message with transaction details
     */
    @Override
    public String apply(SendMoneyRequest request) {
        log.warn("TRANSACTION EXECUTION REQUESTED: sender={}, recipient={}, amount={}",
                request.getSenderId(), request.getRecipientIdentifier(), request.getAmount());

        try {
            // Validate inputs
            if (request.getSenderId() == null || request.getSenderId().trim().isEmpty()) {
                return "❌ ERROR: Sender ID is required to send money.";
            }

            if (request.getRecipientIdentifier() == null || request.getRecipientIdentifier().trim().isEmpty()) {
                return "❌ ERROR: Recipient identifier (phone number or name) is required.";
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                return "❌ ERROR: Amount must be greater than zero.";
            }

            if (request.getAmount() > 1_000_000) {
                return "❌ ERROR: Amount exceeds maximum transfer limit of ₦1,000,000. " +
                        "Please contact support for large transfers.";
            }

            // Resolve recipient
            User recipient = resolveRecipient(request.getRecipientIdentifier());
            if (recipient == null) {
                return String.format("❌ RECIPIENT NOT FOUND: Could not find a user with identifier '%s'. " +
                        "Please check the phone number or name and try again.",
                        request.getRecipientIdentifier());
            }

            // Verify sender exists
            UUID senderUuid;
            try {
                senderUuid = UUID.fromString(request.getSenderId());
                User sender = userRepository.findById(senderUuid).orElse(null);
                if (sender == null) {
                    return "❌ ERROR: Sender account not found.";
                }
            } catch (IllegalArgumentException e) {
                return "❌ ERROR: Invalid sender ID format.";
            }

            // Safety check: Don't allow self-transfer
            if (recipient.getId().equals(senderUuid)) {
                return "❌ ERROR: You cannot send money to yourself.";
            }

            log.warn("EXECUTING TRANSFER: {} -> {} ({}), amount: {}",
                    request.getSenderId(), recipient.getId(), recipient.getName(), request.getAmount());

            // Execute transfer
            TransactionResult result = paymentService.transfer(
                    request.getSenderId(),
                    recipient.getId().toString(),
                    request.getAmount(),
                    request.getNote() != null ? request.getNote() : "Money transfer"
            );

            // Format response
            if (result.isSuccess()) {
                return formatSuccessResponse(result, recipient, request.getAmount(), request.getNote());
            } else {
                return formatErrorResponse(result.getMessage());
            }

        } catch (Exception e) {
            log.error("ERROR executing money transfer", e);
            return "❌ TRANSACTION FAILED: An unexpected error occurred. " +
                    "Your money is safe. Please try again or contact support.";
        }
    }

    /**
     * Resolve recipient by phone number or name
     */
    private User resolveRecipient(String identifier) {
        String normalized = identifier.trim();

        log.debug("Resolving recipient: {}", normalized);

        // Try phone number first (starts with digits)
        if (normalized.matches("^[0-9+]+.*")) {
            // Clean phone number (remove spaces, dashes, etc.)
            String cleanPhone = normalized.replaceAll("[^0-9+]", "");

            Optional<User> byPhone = userRepository.findByPhoneNumber(cleanPhone);
            if (byPhone.isPresent()) {
                log.debug("Recipient found by phone: {}", byPhone.get().getName());
                return byPhone.get();
            }
        }

        // Try exact name match
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getName().equalsIgnoreCase(normalized)) {
                log.debug("Recipient found by exact name match: {}", user.getName());
                return user;
            }
        }

        // Try partial name match (contains)
        for (User user : allUsers) {
            if (user.getName().toLowerCase().contains(normalized.toLowerCase())) {
                log.debug("Recipient found by partial name match: {}", user.getName());
                return user;
            }
        }

        log.warn("Recipient not found: {}", identifier);
        return null;
    }

    /**
     * Format success response
     */
    private String formatSuccessResponse(TransactionResult result, User recipient, double amount, String note) {
        StringBuilder response = new StringBuilder();

        response.append("✅ MONEY SENT SUCCESSFULLY!\n");
        response.append("═══════════════════════════════════════\n\n");

        response.append(String.format("Amount:         %s%,.2f\n", CURRENCY_SYMBOL, amount));
        response.append(String.format("Recipient:      %s\n", recipient.getName()));
        response.append(String.format("Phone:          %s\n", recipient.getPhoneNumber()));

        if (note != null && !note.trim().isEmpty()) {
            response.append(String.format("Note:           %s\n", note));
        }

        response.append("\n");
        response.append(String.format("Transaction ID: %s\n", result.getTransactionId()));
        response.append(String.format("Reference:      %s\n", result.getReference()));
        response.append(String.format("New Balance:    %s%,.2f\n", CURRENCY_SYMBOL, result.getNewBalance()));
        response.append(String.format("Time:           %s\n", result.getTimestamp()));

        response.append("\n");
        response.append("The recipient has been notified of this transfer.\n");
        response.append("Thank you for using Fulus Pay! 💚");

        return response.toString();
    }

    /**
     * Format error response
     */
    private String formatErrorResponse(String errorMessage) {
        return String.format("❌ TRANSACTION FAILED\n\n%s\n\nPlease verify your details and try again.", errorMessage);
    }
}
