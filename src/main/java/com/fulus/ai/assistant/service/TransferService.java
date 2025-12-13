package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.*;
import com.fulus.ai.assistant.entity.BankInfo;
import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import com.fulus.ai.assistant.repository.BankInfoRepository;
import com.fulus.ai.assistant.repository.TransactionRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling internal and inter-bank transfers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BankInfoRepository bankInfoRepository;
    private final BankService bankService;
    private final PasswordEncoder passwordEncoder;

    private static final String INTERNAL_BANK_CODE = "999999";
    private static final BigDecimal INTER_BANK_TRANSFER_FEE = new BigDecimal("50.00");

    /**
     * Internal transfer between Fulus Pay users
     */
    @Transactional
    public TransferResponse internalTransfer(UUID userId, InternalTransferRequest request) {
        log.info("Internal transfer initiated by user: {} to recipient: {}", userId, request.getRecipientIdentifier());

        // Validate sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // Verify PIN
        if (!passwordEncoder.matches(request.getPin(), sender.getPin())) {
            log.warn("SECURITY: Invalid PIN for transfer by user {}", userId);
            throw new IllegalArgumentException("Invalid PIN");
        }

        // Find recipient by phone number or account number
        User recipient = findRecipient(request.getRecipientIdentifier());

        // Prevent self-transfer
        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        // Check sufficient balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Debit sender
        sender.setBalance(sender.getBalance().subtract(amount));
        userRepository.save(sender);

        // Credit recipient
        recipient.setBalance(recipient.getBalance().add(amount));
        userRepository.save(recipient);

        // Create debit transaction for sender
        String reference = generateReference("INT");
        Transaction debitTransaction = createTransaction(
                sender.getId(),
                TransactionType.DEBIT,
                amount,
                sender.getBalance(),
                reference,
                "Transfer to " + recipient.getName() + " (" + recipient.getAccountNumber() + ")",
                recipient.getPhoneNumber(),
                sender.getPhoneNumber()
        );
        transactionRepository.save(debitTransaction);

        // Create credit transaction for recipient
        Transaction creditTransaction = createTransaction(
                recipient.getId(),
                TransactionType.CREDIT,
                amount,
                recipient.getBalance(),
                reference,
                "Transfer from " + sender.getName() + " (" + sender.getAccountNumber() + ")",
                sender.getPhoneNumber(),
                recipient.getPhoneNumber()
        );
        transactionRepository.save(creditTransaction);

        log.info("Internal transfer successful: {} -> {}, Amount: ₦{}",
                sender.getAccountNumber(), recipient.getAccountNumber(), amount);

        return TransferResponse.success(
                debitTransaction.getId(),
                reference,
                amount,
                sender.getBalance(),
                recipient.getName(),
                recipient.getAccountNumber(),
                "INTERNAL"
        );
    }

    /**
     * Inter-bank transfer to external banks
     */
    @Transactional
    public TransferResponse interBankTransfer(UUID userId, InterBankTransferRequest request) {
        log.info("Inter-bank transfer initiated by user: {} to account: {} at bank: {}",
                userId, request.getAccountNumber(), request.getBankCode());

        // Validate sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // Verify PIN
        if (!passwordEncoder.matches(request.getPin(), sender.getPin())) {
            log.warn("SECURITY: Invalid PIN for transfer by user {}", userId);
            throw new IllegalArgumentException("Invalid PIN");
        }

        // Validate bank
        BankInfo bank = bankInfoRepository.findByBankCode(request.getBankCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid bank code"));

        // Check if it's actually internal transfer
        if (INTERNAL_BANK_CODE.equals(request.getBankCode())) {
            throw new IllegalArgumentException("Use internal transfer for Fulus Pay accounts");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        //BigDecimal totalDebit = amount.add(INTER_BANK_TRANSFER_FEE);

        // Check sufficient balance (amount + fee)
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance (including ₦" + INTER_BANK_TRANSFER_FEE + " transfer fee)");
        }

        // Perform name enquiry to get recipient name
        NameEnquiryRequest nameEnquiry = new NameEnquiryRequest(request.getAccountNumber(), request.getBankCode());
        NameEnquiryResponse nameEnquiryResponse = bankService.nameEnquiry(nameEnquiry, userId);

        if (!nameEnquiryResponse.isSuccess()) {
            throw new IllegalArgumentException("Account verification failed");
        }

        // Debit sender (amount + fee)
        sender.setBalance(sender.getBalance().subtract(amount));
        userRepository.save(sender);

        // Create transfer transaction
        String reference = generateReference("EXT");
        Transaction transferTransaction = createTransaction(
                sender.getId(),
                TransactionType.DEBIT,
                amount,
                sender.getBalance().add(INTER_BANK_TRANSFER_FEE), // Balance after amount but before fee
                reference,
                "Transfer to " + nameEnquiryResponse.getAccountName() + " (" + bank.getBankName() + ")",
                request.getAccountNumber(),
                sender.getPhoneNumber()
        );
        transactionRepository.save(transferTransaction);

//        // Create fee transaction
//        Transaction feeTransaction = createTransaction(
//                sender.getId(),
//                TransactionType.DEBIT,
//                INTER_BANK_TRANSFER_FEE,
//                sender.getBalance(),
//                "FEE-" + reference,
//                "Inter-bank transfer fee",
//                null,
//                null
//        );
//        feeTransaction.setCategory(TransactionCategory.FEES);
//        transactionRepository.save(feeTransaction);

        // TODO: In production, integrate with actual banking API to process transfer
        log.info("Inter-bank transfer successful: {} -> {} ({}), Amount: ₦{}, Fee: ₦{}",
                sender.getAccountNumber(), request.getAccountNumber(), bank.getBankName(),
                amount, INTER_BANK_TRANSFER_FEE);

        return TransferResponse.success(
                transferTransaction.getId(),
                reference,
                amount,
                sender.getBalance(),
                nameEnquiryResponse.getAccountName(),
                request.getAccountNumber() + " (" + bank.getBankName() + ")",
                "INTER_BANK"
        );
    }

    /**
     * Find recipient by phone number or account number
     */
    private User findRecipient(String identifier) {
        // Try phone number first
        if (identifier.matches("^0[789][01]\\d{8}$")) {
            return userRepository.findByPhoneNumber(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        }

        // Try account number
        if (identifier.matches("^\\d{10}$")) {
            return userRepository.findByAccountNumber(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        }

        throw new IllegalArgumentException("Invalid recipient identifier. Use phone number or account number");
    }

    /**
     * Create transaction record
     */
    private Transaction createTransaction(UUID userId, TransactionType type, BigDecimal amount,
                                         BigDecimal balanceAfter, String reference, String description,
                                         String recipientPhone, String senderPhone) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setCategory(TransactionCategory.TRANSFER);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setReference(reference);
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setIsOffline(false);
        transaction.setRecipientPhoneNumber(recipientPhone);
        transaction.setSenderPhoneNumber(senderPhone);
        return transaction;
    }

    /**
     * Generate unique transaction reference
     */
    private String generateReference(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
