package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for BVN verification response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BVNVerificationResponse {

    private boolean verified;
    private String bvn;
    private String fullName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String message;

    /**
     * Factory method for successful verification
     */
    public static BVNVerificationResponse success(String bvn, String fullName, LocalDate dateOfBirth, String phoneNumber) {
        return BVNVerificationResponse.builder()
                .verified(true)
                .bvn(bvn)
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .phoneNumber(phoneNumber)
                .message("BVN verification successful")
                .build();
    }

    /**
     * Factory method for failed verification
     */
    public static BVNVerificationResponse failure(String message) {
        return BVNVerificationResponse.builder()
                .verified(false)
                .message(message)
                .build();
    }
}
