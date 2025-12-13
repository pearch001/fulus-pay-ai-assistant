package com.fulus.ai.assistant.enums;

/**
 * Offline transaction sync status
 */
public enum SyncStatus {
    PENDING,    // Transaction created offline, not yet synced
    SYNCED,     // Successfully synced to server
    FAILED,     // Sync failed (network error, validation error)
    CONFLICT    // Sync conflict detected (double-spend, insufficient funds, etc.)
}
