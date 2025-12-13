package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a PIN after signup/login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinCreateRequest {

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String newPin;

    @NotBlank(message = "Confirm PIN is required")
    private String confirmPin;
}

