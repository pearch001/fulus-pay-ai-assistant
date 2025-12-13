package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPairResponse {

    private String keyId;
    private String publicKey;
    private String algorithm;
    private Integer keySize;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

