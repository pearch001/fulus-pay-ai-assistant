package com.fulus.ai.assistant.enums;

/**
 * Device status for user devices
 */
public enum DeviceStatus {
    /**
     * Device is active and can sync transactions
     */
    ACTIVE,

    /**
     * Device is temporarily suspended (e.g., lost/stolen)
     * Cannot sync transactions until reactivated
     */
    SUSPENDED,

    /**
     * Device is permanently revoked
     * Cannot be reactivated, requires new registration
     */
    REVOKED
}
