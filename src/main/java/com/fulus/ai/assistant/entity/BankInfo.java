package com.fulus.ai.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing Nigerian bank information
 */
@Entity
@Table(name = "bank_info", indexes = {
    @Index(name = "idx_bank_code", columnList = "bankCode", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String bankCode;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false)
    private Boolean active = true;
}
