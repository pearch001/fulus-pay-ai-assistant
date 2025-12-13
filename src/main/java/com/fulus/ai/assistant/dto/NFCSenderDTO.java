package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NFC Payload - Sender Information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NFCSenderDTO {

    private String phoneNumber;
    private String publicKey;
    private String deviceId;
}
