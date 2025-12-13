package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCardPinRequest {

    @NotBlank(message = "Old PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "Old PIN must be exactly 4 digits")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "New PIN must be exactly 4 digits")
    private String newPin;

    @NotBlank(message = "Account PIN is required for verification")
    @Pattern(regexp = "^\\d{4}$", message = "Account PIN must be exactly 4 digits")
    private String accountPin;
}
