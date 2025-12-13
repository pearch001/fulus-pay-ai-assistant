package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user profile response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String phoneNumber;
    private String name;
    private String email;
    private BigDecimal balance;
    private String accountNumber;
    private KycStatus kycStatus;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean active;

    /**
     * Factory method to create from User entity
     */
    public static UserProfileResponse fromUser(com.fulus.ai.assistant.entity.User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .email(user.getEmail())
                .balance(user.getBalance())
                .accountNumber(user.getAccountNumber())
                .kycStatus(user.getKycStatus())
                .dateOfBirth(user.getDateOfBirth())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .active(user.isActive())
                .build();
    }
}
