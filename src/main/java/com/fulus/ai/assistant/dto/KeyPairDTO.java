package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for key pair data
 * IMPORTANT: Private key should only be sent once during registration
 * and must be stored securely on the user's device, never on backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPairDTO {

    private String keyId;
    private String publicKey;
    private String privateKey; // Only populated during generation, never stored
    private String algorithm; // RSA or ECDSA
    private Integer keySize;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String format; // PKCS8, X509, etc.

    /**
     * Clear private key (for security)
     * Should be called after sending to client
     */
    public void clearPrivateKey() {
        this.privateKey = null;
    }

    /**
     * Check if this DTO contains private key
     */
    public boolean hasPrivateKey() {
        return this.privateKey != null && !this.privateKey.isEmpty();
    }

    /**
     * Factory method for public key only (for queries)
     */
    public static KeyPairDTO publicKeyOnly(String keyId, String publicKey, String algorithm, int keySize) {
        return KeyPairDTO.builder()
                .keyId(keyId)
                .publicKey(publicKey)
                .privateKey(null) // Never include private key in queries
                .algorithm(algorithm)
                .keySize(keySize)
                .build();
    }
}
