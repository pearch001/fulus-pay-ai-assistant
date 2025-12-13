package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for providing demo credits to new users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemoCreditService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Schedule demo credit for new user (1 minute after registration)
     * This simulates a deposit from "John Doe" for demo purposes
     */
    @Async
    public void scheduleDemoCredit(UUID userId) {
        log.info("Scheduling demo credit for user: {}", userId);

        try {
            // Wait 1 minute
            Thread.sleep(70000);

            // Credit the account
            creditDemoAmount(userId);

        } catch (InterruptedException e) {
            log.error("Demo credit scheduling interrupted for user: {}", userId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error scheduling demo credit for user: {}", userId, e);
        }
    }

    /**
     * Credit demo amount to user account
     */
    @Transactional
    public void creditDemoAmount(UUID userId) {
        log.info("Processing demo credit for user: {}", userId);

        try {
            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            BigDecimal demoAmount = new BigDecimal("1000000.00");
            BigDecimal newBalance = user.getBalance().add(demoAmount);

            // Create transaction record
            Transaction demoTransaction = new Transaction();
            demoTransaction.setUserId(user.getId());
            demoTransaction.setType(TransactionType.CREDIT);
            demoTransaction.setCategory(TransactionCategory.TRANSFER);
            demoTransaction.setAmount(demoAmount);
            demoTransaction.setDescription("Demo Credit from John Doe - Test your account features!");
            demoTransaction.setBalanceAfter(newBalance);
            demoTransaction.setReference("DEMO-" + System.currentTimeMillis() + "-" + user.getId().toString().substring(0, 8).toUpperCase());
            demoTransaction.setStatus(TransactionStatus.COMPLETED);
            demoTransaction.setIsOffline(false);
            demoTransaction.setSenderPhoneNumber("08012345678"); // John Doe's phone
            demoTransaction.setRecipientPhoneNumber(user.getPhoneNumber());

            transactionRepository.save(demoTransaction);

            // Update user balance
            user.setBalance(newBalance);
            userRepository.save(user);

            log.info("Demo credit successful: ₦{} credited to user {} ({}). New balance: ₦{}",
                    demoAmount, user.getId(), user.getPhoneNumber(), newBalance);

        } catch (Exception e) {
            log.error("Failed to process demo credit for user: {}", userId, e);
        }
    }
}
