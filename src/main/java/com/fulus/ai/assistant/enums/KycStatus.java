package com.fulus.ai.assistant.enums;

/**
 * KYC (Know Your Customer) verification status
 */
public enum KycStatus {
    PENDING,      // Registration complete, basic verification pending
    VERIFIED,     // Basic verification complete (BVN verified)
    ENHANCED,     // Enhanced verification with ID document
    REJECTED,     // Verification failed
    SUSPENDED     // Account suspended for security reasons
}
