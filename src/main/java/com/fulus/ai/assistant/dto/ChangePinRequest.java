package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for change PIN request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePinRequest {

    @NotBlank(message = "Old PIN is required")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "New PIN must be exactly 4 digits")
    private String newPin;
}
