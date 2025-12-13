package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for suspending a device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendDeviceRequest {

    private String reason;
}
