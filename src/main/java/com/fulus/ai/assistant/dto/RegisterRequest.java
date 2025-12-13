package com.fulus.ai.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for user registration with BVN verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[789][01]\\d{8}$", message = "Phone number must be valid Nigerian format (e.g., 08012345678)")
    private String phoneNumber;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Residential address is required")
    @Size(min = 5, max = 255, message = "Residential address must be between 5 and 255 characters")
    private String residentialAddress;

    @NotBlank(message = "BVN is required")
    @Pattern(regexp = "^\\d{11}$", message = "BVN must be exactly 11 digits")
    private String bvn;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Device information is required")
    @Valid
    private DeviceInfoRequest deviceInfo;
}
