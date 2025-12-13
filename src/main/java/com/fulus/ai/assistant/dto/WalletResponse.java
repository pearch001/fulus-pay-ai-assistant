package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for wallet details response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private BigDecimal balance;
    private String accountNumber;
    private String walletStatus; // ACTIVE, SUSPENDED, LOCKED
    private Integer pendingTransactionCount;
    private String currency;

    /**
     * Factory method for active wallet
     */
    public static WalletResponse active(BigDecimal balance, String accountNumber, Integer pendingCount) {
        return WalletResponse.builder()
                .balance(balance)
                .accountNumber(accountNumber)
                .walletStatus("ACTIVE")
                .pendingTransactionCount(pendingCount)
                .currency("NGN")
                .build();
    }
}
