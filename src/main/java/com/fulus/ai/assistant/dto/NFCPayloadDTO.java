package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NFC Payment Payload Structure
 * Maximum size: 4KB for NFC compatibility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCPayloadDTO {

    private String version; // Protocol version (e.g., "1.0")
    private String type; // Payload type (e.g., "OFFLINE_PAYMENT")
    private NFCSenderDTO sender;
    private NFCRecipientDTO recipient;
    private NFCTransactionDTO transaction;
    private NFCSecurityDTO security;

    /**
     * Validate payload size for NFC compatibility
     * NFC typically supports up to 4KB
     */
    public boolean isNFCCompatible(String jsonString) {
        return jsonString.getBytes().length <= 4096; // 4KB limit
    }

    /**
     * Check if payload version is supported
     */
    public boolean isSupportedVersion() {
        return "1.0".equals(version);
    }

    /**
     * Check if payload type is valid
     */
    public boolean isValidType() {
        return "OFFLINE_PAYMENT".equals(type);
    }
}
