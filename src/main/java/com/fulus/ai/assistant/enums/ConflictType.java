package com.fulus.ai.assistant.enums;

/**
 * Types of sync conflicts for offline transactions
 */
public enum ConflictType {
    DOUBLE_SPEND,        // Same transaction submitted multiple times
    INSUFFICIENT_FUNDS,  // Account balance insufficient when syncing
    INVALID_HASH,        // Hash chain integrity compromised
    INVALID_SIGNATURE,   // Signature verification failed
    NONCE_REUSED,        // Nonce already used (replay attack)
    CHAIN_BROKEN,        // Previous hash doesn't match chain
    TIMESTAMP_INVALID    // Timestamp too old or in future
}
