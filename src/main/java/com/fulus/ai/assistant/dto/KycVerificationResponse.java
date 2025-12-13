package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for KYC verification response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationResponse {

    private boolean success;
    private KycStatus kycStatus;
    private String message;
    private String documentType;
    private LocalDateTime verifiedAt;

    /**
     * Factory method for successful verification
     */
    public static KycVerificationResponse success(KycStatus status, String documentType, LocalDateTime verifiedAt) {
        return KycVerificationResponse.builder()
                .success(true)
                .kycStatus(status)
                .message("KYC verification successful. Enhanced account limits activated.")
                .documentType(documentType)
                .verifiedAt(verifiedAt)
                .build();
    }

    /**
     * Factory method for failed verification
     */
    public static KycVerificationResponse failure(String reason) {
        return KycVerificationResponse.builder()
                .success(false)
                .kycStatus(KycStatus.REJECTED)
                .message("KYC verification failed: " + reason)
                .build();
    }
}
