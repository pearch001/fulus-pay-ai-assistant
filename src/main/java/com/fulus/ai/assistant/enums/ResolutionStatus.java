package com.fulus.ai.assistant.enums;

/**
 * Conflict resolution status
 */
public enum ResolutionStatus {
    UNRESOLVED,       // Conflict detected, not yet resolved
    AUTO_RESOLVED,    // Automatically resolved by system
    MANUAL_RESOLVED,  // Manually resolved by admin/user
    REJECTED,         // Transaction rejected due to conflict
    PENDING_USER      // Waiting for user action
}
