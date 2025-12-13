package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NFC Payload - Security Information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCSecurityDTO {

    private String hash; // SHA-256 hash of transaction
    private String previousHash; // Previous transaction hash for chain
    private String signature; // Digital signature
}
