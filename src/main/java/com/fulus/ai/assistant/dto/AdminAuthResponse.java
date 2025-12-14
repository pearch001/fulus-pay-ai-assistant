package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for admin authentication response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
    private AdminUserInfo admin;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserInfo {
        private UUID id;
        private String phoneNumber;
        private String name;
        private String email;
        private UserRole role;
        private LocalDateTime lastLoginAt;
    }

    /**
     * Factory method for successful admin authentication
     */
    public static AdminAuthResponse success(String accessToken, String refreshToken, Long expiresIn, AdminUserInfo adminInfo) {
        return AdminAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .admin(adminInfo)
                .build();
    }
}

