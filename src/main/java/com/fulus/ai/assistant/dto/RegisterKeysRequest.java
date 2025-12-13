package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterKeysRequest {

    @NotBlank(message = "Public key is required")
    private String publicKey;

    private String algorithm; // Optional: defaults to RSA
    private Integer keySize;  // Optional: defaults to 2048
}

