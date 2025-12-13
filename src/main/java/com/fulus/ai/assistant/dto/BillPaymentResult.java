package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.BillType;
import com.fulus.ai.assistant.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPaymentResult {

    private boolean success;
    private String message;
    private UUID transactionId;
    private String transactionReference;
    private String paymentToken;
    private BillType billType;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private TransactionStatus status;
    private String providerName;
    private LocalDateTime timestamp;

    // Convenience method to create success result
    public static BillPaymentResult success(
            UUID transactionId,
            String transactionReference,
            String paymentToken,
            BillType billType,
            String accountNumber,
            BigDecimal amount,
            BigDecimal newBalance,
            String providerName) {
        return BillPaymentResult.builder()
                .success(true)
                .message("Bill payment successful")
                .transactionId(transactionId)
                .transactionReference(transactionReference)
                .paymentToken(paymentToken)
                .billType(billType)
                .accountNumber(accountNumber)
                .amount(amount)
                .newBalance(newBalance)
                .status(TransactionStatus.COMPLETED)
                .providerName(providerName)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Convenience method to create failure result
    public static BillPaymentResult failure(String message, BillType billType) {
        return BillPaymentResult.builder()
                .success(false)
                .message(message)
                .billType(billType)
                .status(TransactionStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
