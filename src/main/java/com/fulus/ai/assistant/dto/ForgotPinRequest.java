package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for forgot PIN request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPinRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[789][01]\\d{8}$", message = "Invalid Nigerian phone number format")
    private String phoneNumber;

    @NotBlank(message = "BVN is required")
    @Pattern(regexp = "^\\d{11}$", message = "BVN must be exactly 11 digits")
    private String bvn;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
}
