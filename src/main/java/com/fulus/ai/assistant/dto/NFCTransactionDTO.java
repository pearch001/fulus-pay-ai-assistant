package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * NFC Payload - Transaction Information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCTransactionDTO {

    private BigDecimal amount;
    private String currency;
    private Long timestamp; // Unix timestamp in milliseconds
    private String nonce;
    private String note;
}
