package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for QR code generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeResponse {

    private String qrCodeImage; // Base64 encoded PNG
    private String paymentRequestId;
    private String qrData; // Raw QR data (JSON)
    private LocalDateTime expiryTime;
    private Long expirySeconds;
    private Integer qrCodeSize;
    private String format; // PNG, JPG, etc.

    /**
     * Factory method for successful generation
     */
    public static QRCodeResponse success(String qrCodeImage, String paymentRequestId,
                                         String qrData, LocalDateTime expiryTime, int size) {
        long expirySeconds = java.time.Duration.between(LocalDateTime.now(), expiryTime).getSeconds();

        return QRCodeResponse.builder()
                .qrCodeImage(qrCodeImage)
                .paymentRequestId(paymentRequestId)
                .qrData(qrData)
                .expiryTime(expiryTime)
                .expirySeconds(expirySeconds)
                .qrCodeSize(size)
                .format("PNG")
                .build();
    }
}
