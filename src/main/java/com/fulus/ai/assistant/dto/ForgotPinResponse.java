package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for forgot PIN response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPinResponse {

    private String resetToken;
    private String otp; // For testing only - remove in production
    private LocalDateTime expiresAt;
    private String message;

    public static ForgotPinResponse success(String resetToken, String otp, LocalDateTime expiresAt) {
        return ForgotPinResponse.builder()
                .resetToken(resetToken)
                .otp(otp)
                .expiresAt(expiresAt)
                .message("OTP sent successfully. Valid for 10 minutes.")
                .build();
    }
}
