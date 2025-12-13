package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for authentication response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String phoneNumber;
        private String name;
        private String email;
        private String accountNumber;
        private BigDecimal balance;
        private LocalDateTime lastLoginAt;
        private Boolean isPinCreated;
    }

    /**
     * Factory method for successful authentication
     */
    public static AuthResponse success(String accessToken, String refreshToken, Long expiresIn, UserInfo userInfo) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();
    }
}
