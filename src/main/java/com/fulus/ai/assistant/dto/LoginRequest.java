package com.fulus.ai.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login request with phone number and password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotNull(message = "Device information is required")
    @Valid
    private DeviceInfoRequest deviceInfo;
}
