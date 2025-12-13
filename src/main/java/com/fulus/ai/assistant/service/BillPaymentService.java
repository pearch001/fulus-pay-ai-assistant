package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.BillPaymentResult;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.BillType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillPaymentService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // Mock provider mapping
    private static final Map<BillType, String> BILL_PROVIDERS = new HashMap<>();

    static {
        BILL_PROVIDERS.put(BillType.ELECTRICITY, "PowerCo Energy");
        BILL_PROVIDERS.put(BillType.WATER, "AquaFlow Utilities");
        BILL_PROVIDERS.put(BillType.AIRTIME, "TeleConnect");
        BILL_PROVIDERS.put(BillType.DATA, "TeleConnect Data");
        BILL_PROVIDERS.put(BillType.CABLE_TV, "ViewStream TV");
    }

    /**
     * Pay bill for a user
     *
     * @param userId        UUID of user paying the bill
     * @param billType      Type of bill to pay
     * @param amount        Amount to pay
     * @param accountNumber Bill account number
     * @return BillPaymentResult with payment details
     */
    @Transactional
    public BillPaymentResult payBill(String userId, BillType billType, double amount, String accountNumber) {
        log.info("Processing bill payment: userId={}, billType={}, amount={}, accountNumber={}",
                userId, billType, amount, accountNumber);

        try {
            // Validate amount
            validateAmount(amount);

            // Validate account number
            validateAccountNumber(accountNumber);

            // Convert to BigDecimal
            BigDecimal paymentAmount = BigDecimal.valueOf(amount);

            // Get user
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Validate sufficient balance
            if (user.getBalance().compareTo(paymentAmount) < 0) {
                log.warn("Insufficient funds for bill payment: userId={}, balance={}, amount={}",
                        userId, user.getBalance(), paymentAmount);
                throw new InsufficientFundsException(userId, amount, user.getBalance().doubleValue());
            }

            // Mock external provider integration
            String paymentToken = processWithExternalProvider(billType, accountNumber, paymentAmount);

            // Debit user account
            user.setBalance(user.getBalance().subtract(paymentAmount));
            userRepository.save(user);

            // Create transaction record
            Transaction transaction = createBillPaymentTransaction(
                    user,
                    billType,
                    paymentAmount,
                    accountNumber,
                    paymentToken
            );

            log.info("Bill payment completed successfully: reference={}, token={}",
                    transaction.getReference(), paymentToken);

            return BillPaymentResult.success(
                    transaction.getId(),
                    transaction.getReference(),
                    paymentToken,
                    billType,
                    accountNumber,
                    paymentAmount,
                    user.getBalance(),
                    BILL_PROVIDERS.get(billType)
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid input format: {}", e.getMessage());
            return BillPaymentResult.failure("Invalid user ID format", billType);
        } catch (InsufficientFundsException | UserNotFoundException | InvalidAmountException e) {
            log.error("Bill payment failed: {}", e.getMessage());
            return BillPaymentResult.failure(e.getMessage(), billType);
        } catch (Exception e) {
            log.error("Unexpected error during bill payment", e);
            return BillPaymentResult.failure("Bill payment failed due to an unexpected error", billType);
        }
    }

    /**
     * Mock integration with external bill payment provider
     * In production, this would make actual API calls to bill payment providers
     */
    private String processWithExternalProvider(BillType billType, String accountNumber, BigDecimal amount) {
        log.info("Processing payment with external provider: {} for account: {}",
                BILL_PROVIDERS.get(billType), accountNumber);

        // Simulate processing delay (in production this would be an actual API call)
        try {
            Thread.sleep(100); // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate mock payment token
        String paymentToken = generatePaymentToken(billType);

        // Simulate random failure (5% chance) for testing purposes
        Random random = new Random();
        if (random.nextInt(100) < 5) {
            log.warn("Simulated provider failure for testing");
            throw new RuntimeException("Provider temporarily unavailable");
        }

        log.info("External provider confirmed payment: token={}", paymentToken);
        return paymentToken;
    }

    /**
     * Create transaction record for bill payment
     */
    private Transaction createBillPaymentTransaction(
            User user,
            BillType billType,
            BigDecimal amount,
            String accountNumber,
            String paymentToken) {

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setType(TransactionType.DEBIT);
        transaction.setCategory(TransactionCategory.BILL_PAYMENT);
        transaction.setAmount(amount);
        transaction.setDescription(String.format("%s payment for account %s (Token: %s)",
                billType.getDisplayName(), maskAccountNumber(accountNumber), paymentToken));
        transaction.setBalanceAfter(user.getBalance());
        transaction.setReference(generateTransactionReference());
        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }

    /**
     * Generate unique payment token based on bill type
     */
    private String generatePaymentToken(BillType billType) {
        String prefix = switch (billType) {
            case ELECTRICITY -> "ELEC";
            case WATER -> "WATR";
            case AIRTIME -> "AIRT";
            case DATA -> "DATA";
            case CABLE_TV -> "CBTV";
        };

        // Generate token with prefix + random alphanumeric
        String randomPart = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 12);
        return prefix + "-" + randomPart;
    }

    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        return "BILL-" + UUID.randomUUID().toString().toUpperCase().substring(0, 18);
    }

    /**
     * Validate transaction amount
     */
    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }

        // Add maximum limit check (e.g., 1 million)
        if (amount > 1_000_000) {
            throw new InvalidAmountException("Amount exceeds maximum limit of 1,000,000");
        }
    }

    /**
     * Validate account number format
     */
    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        if (accountNumber.length() < 4 || accountNumber.length() > 20) {
            throw new IllegalArgumentException("Account number must be between 4 and 20 characters");
        }
    }

    /**
     * Mask account number for privacy (show only last 4 digits)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
