package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.TransactionResult;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.exception.InsufficientFundsException;
import com.fulus.ai.assistant.exception.InvalidAmountException;
import com.fulus.ai.assistant.exception.UserNotFoundException;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Transfer funds from sender to recipient
     *
     * @param senderId    UUID of sender
     * @param recipientId UUID of recipient
     * @param amount      Amount to transfer
     * @param note        Optional transfer note
     * @return TransactionResult with transfer details
     */
    @Transactional
    public TransactionResult transfer(String senderId, String recipientId, double amount, String note) {
        log.info("Initiating transfer: {} -> {}, amount: {}", senderId, recipientId, amount);

        try {
            // Validate amount
            validateAmount(amount);

            // Convert to BigDecimal for precise calculations
            BigDecimal transferAmount = BigDecimal.valueOf(amount);

            // Validate sender and recipient
            UUID senderUuid = UUID.fromString(senderId);
            UUID recipientUuid = UUID.fromString(recipientId);

            User sender = userRepository.findById(senderUuid)
                    .orElseThrow(() -> new UserNotFoundException("Sender not found: " + senderId));

            User recipient = userRepository.findById(recipientUuid)
                    .orElseThrow(() -> new UserNotFoundException("Recipient not found: " + recipientId));

            // Validate sender has sufficient balance
            if (sender.getBalance().compareTo(transferAmount) < 0) {
                throw new InsufficientFundsException(senderId, amount, sender.getBalance().doubleValue());
            }

            // Debit sender
            Transaction debitTransaction = processDebit(
                    sender,
                    transferAmount,
                    TransactionCategory.TRANSFER,
                    String.format("Transfer to %s: %s", recipient.getName(), note)
            );

            // Credit recipient
            Transaction creditTransaction = processCredit(
                    recipient,
                    transferAmount,
                    TransactionCategory.TRANSFER,
                    String.format("Transfer from %s: %s", sender.getName(), note)
            );

            log.info("Transfer completed successfully. Reference: {}", debitTransaction.getReference());

            return TransactionResult.success(
                    debitTransaction,
                    sender.getBalance(),
                    "Transfer completed successfully"
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            return TransactionResult.failure("Invalid user ID format");
        } catch (InsufficientFundsException | UserNotFoundException | InvalidAmountException e) {
            log.error("Transfer failed: {}", e.getMessage());
            return TransactionResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during transfer", e);
            return TransactionResult.failure("Transfer failed due to an unexpected error");
        }
    }

    /**
     * Get current balance for a user
     *
     * @param userId UUID of user
     * @return Current balance
     */
    @Transactional(readOnly = true)
    public double getBalance(String userId) {
        log.debug("Fetching balance for user: {}", userId);

        try {
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            return user.getBalance().doubleValue();

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            throw new UserNotFoundException("Invalid user ID format: " + userId);
        }
    }

    /**
     * Debit amount from user account
     *
     * @param userId      UUID of user
     * @param amount      Amount to debit
     * @param description Transaction description
     * @return Created transaction
     */
    @Transactional
    public Transaction debitAccount(String userId, double amount, String description) {
        log.info("Debiting account: {}, amount: {}", userId, amount);

        // Validate amount
        validateAmount(amount);

        // Convert to BigDecimal
        BigDecimal debitAmount = BigDecimal.valueOf(amount);

        // Get user
        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Validate sufficient balance
        if (user.getBalance().compareTo(debitAmount) < 0) {
            throw new InsufficientFundsException(userId, amount, user.getBalance().doubleValue());
        }

        // Process debit
        return processDebit(user, debitAmount, TransactionCategory.OTHER, description);
    }

    /**
     * Credit amount to user account
     *
     * @param userId      UUID of user
     * @param amount      Amount to credit
     * @param description Transaction description
     * @return Created transaction
     */
    @Transactional
    public Transaction creditAccount(String userId, double amount, String description) {
        log.info("Crediting account: {}, amount: {}", userId, amount);

        // Validate amount
        validateAmount(amount);

        // Convert to BigDecimal
        BigDecimal creditAmount = BigDecimal.valueOf(amount);

        // Get user
        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Process credit
        return processCredit(user, creditAmount, TransactionCategory.OTHER, description);
    }

    /**
     * Internal method to process debit transaction
     */
    private Transaction processDebit(User user, BigDecimal amount, TransactionCategory category, String description) {
        // Deduct from user balance
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setType(TransactionType.DEBIT);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setBalanceAfter(user.getBalance());
        transaction.setReference(generateTransactionReference());
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    /**
     * Internal method to process credit transaction
     */
    private Transaction processCredit(User user, BigDecimal amount, TransactionCategory category, String description) {
        // Add to user balance
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setType(TransactionType.CREDIT);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setBalanceAfter(user.getBalance());
        transaction.setReference(generateTransactionReference());
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    /**
     * Validate transaction amount
     */
    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().substring(0, 18);
    }
}
