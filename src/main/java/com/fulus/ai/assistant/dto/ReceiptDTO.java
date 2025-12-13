package com.fulus.ai.assistant.dto;

import com.fulus.ai.assistant.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDTO {

    private String title;
    private String subtitle;
    private ReceiptStatus status;
    private BigDecimal amount;
    private String amountLabel;
    private String senderName;
    private String senderAccount;
    private String recipientName;
    private String recipientAccount;
    private String reference;
    private String description;
    private LocalDateTime timestamp;
    private String transactionType; // DEBIT, CREDIT, TRANSFER
    private BigDecimal balanceAfter;
    private BigDecimal fee;

    public enum ReceiptStatus {
        SUCCESS("#4CAF50", "✓"),
        FAILED("#F44336", "✗"),
        PENDING("#FFC107", "–");

        private final String color;
        private final String icon;

        ReceiptStatus(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public String getColor() {
            return color;
        }

        public String getIcon() {
            return icon;
        }

        public static ReceiptStatus fromTransactionStatus(TransactionStatus status) {
            return switch (status) {
                case COMPLETED -> SUCCESS;
                case FAILED -> FAILED;
                case PENDING -> PENDING;
                case REVERSED -> FAILED;
            };
        }
    }
}
