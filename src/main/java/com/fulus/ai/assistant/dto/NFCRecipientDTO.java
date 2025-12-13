package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NFC Payload - Recipient Information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCRecipientDTO {

    private String phoneNumber;
    private String publicKey;
}
