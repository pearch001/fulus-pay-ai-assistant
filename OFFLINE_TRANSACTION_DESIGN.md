# Offline Transaction System - Design Documentation

Complete technical documentation for the offline peer-to-peer transaction system with blockchain-like features.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [Blockchain-Like Features](#blockchain-like-features)
5. [Security Features](#security-features)
6. [Conflict Detection & Resolution](#conflict-detection--resolution)
7. [API Workflows](#api-workflows)
8. [Utility Classes](#utility-classes)
9. [Production Considerations](#production-considerations)

---

## Overview

### Purpose

The offline transaction system enables peer-to-peer payments when internet connectivity is unavailable, with:
- **Hash chain integrity** (blockchain-like)
- **Digital signatures** for authenticity
- **Replay attack prevention** (nonce)
- **Automatic conflict detection**
- **Chain validation**
- **Sync when online**

### Use Cases

1. **Rural Areas:** Limited internet connectivity
2. **Network Outages:** Temporary connectivity loss
3. **Emergency Payments:** Critical transactions during disasters
4. **Peer-to-Peer:** Direct phone-to-phone transfers (NFC, Bluetooth)

### Key Features

✅ **Offline Transaction Creation** - Create transactions without internet
✅ **Hash Chain** - Each transaction links to previous (blockchain concept)
✅ **Digital Signatures** - Verify transaction authenticity
✅ **Nonce System** - Prevent replay attacks
✅ **Conflict Detection** - Detect double-spend, insufficient funds
✅ **Auto-Resolution** - Resolve conflicts automatically when possible
✅ **Chain Validation** - Verify chain integrity before sync

---

## Architecture

### High-Level Flow

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Mobile    │         │   Offline    │         │   Online    │
│   Device    │────────>│  Transaction │────────>│   Server    │
│  (Sender)   │         │    Chain     │         │  (Sync)     │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │                         │
      │ 1. Create TX          │                         │
      │ 2. Sign with PIN      │                         │
      │ 3. Generate Hash      │                         │
      │ 4. Link to Previous   │                         │
      │ 5. Store Locally      │                         │
      │                        │ 6. When Online         │
      │                        │ 7. Validate Chain      │
      │                        │ 8. Detect Conflicts    │
      │                        │ 9. Sync to Server      │
      │                        │<────────────────────────│
      │                        │ 10. Update Status      │
```

### Components

1. **OfflineTransaction** - Individual transaction entity
2. **OfflineTransactionChain** - Per-user chain metadata
3. **SyncConflict** - Conflict tracking and resolution
4. **Hash Utilities** - SHA-256 hash generation
5. **Signature Utilities** - HMAC/RSA signatures
6. **Nonce Generator** - Unique nonce generation
7. **Payload Encryption** - AES-256-GCM encryption

---

## Database Schema

### 1. OfflineTransaction Table

```sql
CREATE TABLE offline_transactions (
    id UUID PRIMARY KEY,

    -- Transaction Details
    sender_phone_number VARCHAR(20) NOT NULL,
    recipient_phone_number VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),

    -- Blockchain Features
    transaction_hash VARCHAR(64) UNIQUE NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    nonce VARCHAR(64) UNIQUE NOT NULL,
    signature_key VARCHAR(500) NOT NULL,

    -- Encrypted Payload
    payload TEXT NOT NULL,

    -- Metadata
    timestamp TIMESTAMP NOT NULL,
    sync_status VARCHAR(20) NOT NULL,
    sender_offline_balance DECIMAL(15,2),

    -- Sync Tracking
    sync_attempts INTEGER DEFAULT 0,
    last_sync_attempt TIMESTAMP,
    sync_error VARCHAR(500),
    synced_at TIMESTAMP,
    online_transaction_id UUID,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),

    -- Indexes
    INDEX idx_sender_phone (sender_phone_number),
    INDEX idx_recipient_phone (recipient_phone_number),
    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_nonce (nonce),
    INDEX idx_sync_status (sync_status),
    INDEX idx_timestamp (timestamp)
);
```

**Key Fields:**

| Field | Type | Purpose |
|-------|------|---------|
| transaction_hash | VARCHAR(64) | SHA-256 hash of transaction data |
| previous_hash | VARCHAR(64) | Hash of previous TX (chain link) |
| nonce | VARCHAR(64) | Unique value (prevents replay) |
| signature_key | VARCHAR(500) | Digital signature (authenticity) |
| payload | TEXT | Encrypted JSON (full TX details) |
| sync_status | ENUM | PENDING/SYNCED/FAILED/CONFLICT |

### 2. OfflineTransactionChain Table

```sql
CREATE TABLE offline_transaction_chains (
    id UUID PRIMARY KEY,

    -- User Reference
    user_id UUID UNIQUE NOT NULL,

    -- Chain State
    last_synced_hash VARCHAR(64),
    last_synced_at TIMESTAMP,
    genesis_hash VARCHAR(64),
    current_head_hash VARCHAR(64),

    -- Counters
    pending_count INTEGER DEFAULT 0,
    total_transactions INTEGER DEFAULT 0,
    synced_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    conflict_count INTEGER DEFAULT 0,

    -- Validation
    chain_valid BOOLEAN DEFAULT TRUE,
    last_validated_at TIMESTAMP,
    validation_error VARCHAR(500),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_chain_valid (chain_valid)
);
```

**Purpose:** Track chain state per user for quick validation and stats.

### 3. SyncConflict Table

```sql
CREATE TABLE sync_conflicts (
    id UUID PRIMARY KEY,

    -- References
    transaction_id UUID NOT NULL,
    user_id UUID NOT NULL,

    -- Conflict Details
    conflict_type VARCHAR(30) NOT NULL,
    conflict_description VARCHAR(1000) NOT NULL,
    suggested_resolution VARCHAR(500),

    -- Resolution
    resolution_status VARCHAR(20) DEFAULT 'UNRESOLVED',
    resolution_notes VARCHAR(1000),
    resolved_by UUID,

    -- Balances (for insufficient funds)
    expected_balance DECIMAL(15,2),
    actual_balance DECIMAL(15,2),

    -- Hashes (for chain errors)
    expected_previous_hash VARCHAR(64),
    actual_previous_hash VARCHAR(64),

    -- Metadata
    priority INTEGER DEFAULT 3,
    auto_resolution_attempted BOOLEAN DEFAULT FALSE,

    -- Timestamps
    detected_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP,

    INDEX idx_transaction_id (transaction_id),
    INDEX idx_user_id (user_id),
    INDEX idx_conflict_type (conflict_type),
    INDEX idx_resolution_status (resolution_status)
);
```

**Conflict Types:**
- `DOUBLE_SPEND` - Same transaction submitted multiple times
- `INSUFFICIENT_FUNDS` - Balance too low when syncing
- `INVALID_HASH` - Hash doesn't match calculated value
- `INVALID_SIGNATURE` - Signature verification failed
- `NONCE_REUSED` - Nonce already used
- `CHAIN_BROKEN` - Previous hash doesn't match
- `TIMESTAMP_INVALID` - Timestamp too old/future

---

## Blockchain-Like Features

### 1. Hash Chain

Each transaction contains:
- **Current Hash:** SHA-256 of transaction data
- **Previous Hash:** Link to previous transaction

```
TX1 (Genesis)                TX2                     TX3
┌──────────────┐            ┌──────────────┐        ┌──────────────┐
│ Hash: abc123 │            │ Hash: def456 │        │ Hash: ghi789 │
│ Prev: 000000 │───────────>│ Prev: abc123 │───────>│ Prev: def456 │
└──────────────┘            └──────────────┘        └──────────────┘
```

**Hash Calculation:**
```java
String data = senderPhone + recipientPhone + amount +
              timestamp + nonce + previousHash;
String hash = SHA256(data);
```

**Benefits:**
- Tamper-evident (changing TX breaks chain)
- Order preservation
- Integrity verification

### 2. Digital Signatures

Each transaction is signed by sender:

```java
// Generate signature
String signature = HMAC_SHA256(transactionHash, userSecretKey);

// Verify signature
boolean valid = verifySignature(transactionHash, signature, userSecretKey);
```

**Production:** Use RSA or ECDSA with public/private key pairs.

### 3. Nonce (Number Used Once)

Prevents replay attacks:

```java
String nonce = UUID.randomUUID().toString();
// Store nonce in database (unique constraint)
// Reject if nonce already exists
```

**Formats:**
- UUID: `a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n`
- Hex: `3a7f4c2e1b5d6e7f8g9h0i1j2k3l4m5n`
- Timestamp: `1702305000000-a7f3c2e14b5d`

### 4. Encrypted Payload

Full transaction details stored encrypted:

```json
{
  "senderPhone": "08012345678",
  "recipientPhone": "08087654321",
  "amount": 1000.00,
  "description": "Payment for goods",
  "metadata": {
    "location": "Lagos",
    "deviceId": "abc123"
  }
}
```

**Encryption:** AES-256-GCM (authenticated encryption)

---

## Security Features

### 1. Chain Integrity Validation

```java
// Validate entire chain
for (int i = 1; i < transactions.size(); i++) {
    Transaction current = transactions.get(i);
    Transaction previous = transactions.get(i - 1);

    // Check if previous hash matches
    if (!current.getPreviousHash().equals(previous.getTransactionHash())) {
        // Chain broken!
        createConflict(current, ConflictType.CHAIN_BROKEN);
    }

    // Verify hash calculation
    String calculatedHash = calculateHash(current);
    if (!calculatedHash.equals(current.getTransactionHash())) {
        // Hash invalid!
        createConflict(current, ConflictType.INVALID_HASH);
    }
}
```

### 2. Replay Attack Prevention

```java
// Check nonce uniqueness
if (offlineTransactionRepository.existsByNonce(nonce)) {
    throw new SecurityException("Nonce already used - replay attack detected");
}
```

### 3. Signature Verification

```java
// Verify transaction signature
boolean valid = TransactionSignatureUtil.verifySignature(
    transactionHash,
    signature,
    getUserSecretKey(senderPhone)
);

if (!valid) {
    createConflict(transaction, ConflictType.INVALID_SIGNATURE);
}
```

### 4. Timestamp Validation

```java
// Check timestamp is not too old (e.g., > 7 days)
LocalDateTime threshold = LocalDateTime.now().minusDays(7);
if (transaction.getTimestamp().isBefore(threshold)) {
    createConflict(transaction, ConflictType.TIMESTAMP_INVALID);
}

// Check timestamp is not in future
if (transaction.getTimestamp().isAfter(LocalDateTime.now())) {
    createConflict(transaction, ConflictType.TIMESTAMP_INVALID);
}
```

---

## Conflict Detection & Resolution

### Conflict Types & Auto-Resolution

| Conflict Type | Auto-Resolvable | Resolution Strategy |
|---------------|-----------------|---------------------|
| DOUBLE_SPEND | ✅ Yes | Keep first, reject duplicates |
| INSUFFICIENT_FUNDS | ❌ No | Require user action (top-up) |
| INVALID_HASH | ❌ No | Reject transaction |
| INVALID_SIGNATURE | ❌ No | Reject transaction |
| NONCE_REUSED | ✅ Yes | Reject replay attempt |
| CHAIN_BROKEN | ⚠️ Partial | Rebuild chain if possible |
| TIMESTAMP_INVALID | ⚠️ Partial | Accept if within tolerance |

### Auto-Resolution Flow

```java
public void autoResolveConflicts() {
    List<SyncConflict> conflicts = conflictRepository.findAllUnresolvedConflicts();

    for (SyncConflict conflict : conflicts) {
        switch (conflict.getConflictType()) {
            case DOUBLE_SPEND:
                // Keep first transaction, reject others
                resolveDoubleSpend(conflict);
                break;

            case NONCE_REUSED:
                // Reject replay attempt
                rejectTransaction(conflict, "Replay attack detected");
                break;

            case TIMESTAMP_INVALID:
                // Accept if within tolerance (e.g., 30 days)
                if (isWithinTolerance(conflict)) {
                    acceptWithWarning(conflict);
                } else {
                    rejectTransaction(conflict, "Timestamp too old");
                }
                break;

            default:
                // Escalate to manual resolution
                conflict.markAsPendingUser("Manual review required");
                break;
        }
    }
}
```

### Manual Resolution Flow

```
1. Admin/User reviews conflict
2. Checks transaction details
3. Verifies balances
4. Makes decision:
   - Accept (credit transaction)
   - Reject (refund if applicable)
   - Request more info
5. Update conflict status
6. Notify user
```

---

## API Workflows

### 1. Create Offline Transaction

**Client-Side (Mobile App):**

```javascript
async function createOfflineTransaction(senderPhone, recipientPhone, amount, pin) {
  // 1. Get user's chain state
  const chain = await getLocalChain(senderPhone);

  // 2. Generate nonce
  const nonce = generateUUID();

  // 3. Get previous hash
  const previousHash = chain.lastHash || getGenesisHash();

  // 4. Calculate hash
  const timestamp = new Date().toISOString();
  const hash = calculateHash({
    senderPhone,
    recipientPhone,
    amount: amount.toString(),
    timestamp,
    nonce,
    previousHash
  });

  // 5. Generate signature
  const secretKey = deriveSecretKey(senderPhone, pin);
  const signature = generateSignature(hash, secretKey);

  // 6. Create encrypted payload
  const payload = encryptPayload({
    senderPhone,
    recipientPhone,
    amount,
    description: "Offline payment",
    metadata: {}
  }, secretKey);

  // 7. Create transaction object
  const transaction = {
    id: generateUUID(),
    senderPhoneNumber: senderPhone,
    recipientPhoneNumber: recipientPhone,
    amount,
    transactionHash: hash,
    previousHash,
    payload,
    signatureKey: signature,
    timestamp,
    nonce,
    syncStatus: 'PENDING',
    senderOfflineBalance: getOfflineBalance(senderPhone)
  };

  // 8. Store locally
  await saveToLocalDatabase(transaction);

  // 9. Update chain
  chain.lastHash = hash;
  chain.pendingCount++;
  await saveChainState(chain);

  // 10. Update offline balance
  updateOfflineBalance(senderPhone, -amount);

  return transaction;
}
```

### 2. Sync Offline Transactions

**Server-Side:**

```java
@Transactional
public SyncResult syncOfflineTransactions(UUID userId, List<OfflineTransaction> transactions) {
    // 1. Get user's chain
    OfflineTransactionChain chain = getOrCreateChain(userId);

    // 2. Validate chain integrity
    List<ValidationError> chainErrors = validateChain(transactions);
    if (!chainErrors.isEmpty()) {
        chain.invalidateChain("Chain integrity compromised");
        return SyncResult.failed(chainErrors);
    }

    // 3. Process each transaction
    List<SyncConflict> conflicts = new ArrayList<>();
    List<Transaction> syncedTransactions = new ArrayList<>();

    for (OfflineTransaction offlineTx : transactions) {
        try {
            // Verify signature
            if (!verifySignature(offlineTx)) {
                conflicts.add(createConflict(offlineTx, ConflictType.INVALID_SIGNATURE));
                continue;
            }

            // Check for duplicates
            if (isDuplicate(offlineTx)) {
                conflicts.add(createConflict(offlineTx, ConflictType.DOUBLE_SPEND));
                continue;
            }

            // Check nonce
            if (nonceExists(offlineTx.getNonce())) {
                conflicts.add(createConflict(offlineTx, ConflictType.NONCE_REUSED));
                continue;
            }

            // Check balance
            if (!hasSufficientFunds(offlineTx)) {
                conflicts.add(createConflict(offlineTx, ConflictType.INSUFFICIENT_FUNDS));
                continue;
            }

            // Create online transaction
            Transaction onlineTx = processTransaction(offlineTx);
            syncedTransactions.add(onlineTx);

            // Mark as synced
            offlineTx.markAsSynced(onlineTx.getId());
            chain.markTransactionSynced(offlineTx.getTransactionHash());

        } catch (Exception e) {
            offlineTx.markAsFailed(e.getMessage());
            chain.markTransactionFailed();
        }
    }

    // 4. Save all
    offlineTransactionRepository.saveAll(transactions);
    chainRepository.save(chain);
    conflictRepository.saveAll(conflicts);

    // 5. Auto-resolve conflicts
    autoResolveConflicts(conflicts);

    return SyncResult.builder()
            .syncedCount(syncedTransactions.size())
            .conflictCount(conflicts.size())
            .failedCount(transactions.size() - syncedTransactions.size() - conflicts.size())
            .transactions(syncedTransactions)
            .conflicts(conflicts)
            .build();
}
```

### 3. Validate Chain

```java
public boolean validateChain(String phoneNumber) {
    List<OfflineTransaction> chain = repository.getTransactionChain(phoneNumber);

    if (chain.isEmpty()) {
        return true; // Empty chain is valid
    }

    // Check genesis transaction
    if (!chain.get(0).getPreviousHash().equals(getGenesisHash())) {
        return false;
    }

    // Validate each link
    for (int i = 1; i < chain.size(); i++) {
        OfflineTransaction current = chain.get(i);
        OfflineTransaction previous = chain.get(i - 1);

        // Check hash link
        if (!current.getPreviousHash().equals(previous.getTransactionHash())) {
            return false;
        }

        // Verify hash calculation
        if (!verifyHash(current)) {
            return false;
        }

        // Verify signature
        if (!verifySignature(current)) {
            return false;
        }
    }

    return true;
}
```

---

## Utility Classes

### 1. TransactionHashUtil.java

**Purpose:** Generate SHA-256 hashes

**Key Methods:**
```java
// Generate transaction hash
String hash = TransactionHashUtil.generateTransactionHash(
    senderPhone, recipientPhone, amount, timestamp, nonce, previousHash);

// Verify hash
boolean valid = TransactionHashUtil.verifyTransactionHash(...);

// Get genesis hash
String genesis = TransactionHashUtil.getGenesisHash();
// Returns: "0000000000000000000000000000000000000000000000000000000000000000"
```

### 2. TransactionSignatureUtil.java

**Purpose:** Generate and verify signatures

**Key Methods:**
```java
// Generate signature (HMAC-SHA256)
String signature = TransactionSignatureUtil.generateSignature(hash, secretKey);

// Verify signature
boolean valid = TransactionSignatureUtil.verifySignature(hash, signature, secretKey);

// Generate user secret key
String secretKey = TransactionSignatureUtil.generateUserSecretKey(phoneNumber, pin);
```

**Production:** Use RSA/ECDSA with public/private keys

### 3. NonceGenerator.java

**Purpose:** Generate unique nonces

**Key Methods:**
```java
// UUID-based (most secure)
String nonce = NonceGenerator.generateUUIDNonce();

// Hex random
String nonce = NonceGenerator.generateHexNonce();

// With context
String nonce = NonceGenerator.generateContextualNonce(phoneNumber);
```

### 4. PayloadEncryptionUtil.java

**Purpose:** Encrypt/decrypt transaction payloads

**Key Methods:**
```java
// Encrypt payload
Map<String, Object> payload = Map.of(
    "senderPhone", "08012345678",
    "recipientPhone", "08087654321",
    "amount", "1000.00"
);
String encrypted = PayloadEncryptionUtil.encryptPayload(payload, encryptionKey);

// Decrypt payload
String decrypted = PayloadEncryptionUtil.decryptPayload(encrypted, encryptionKey);
```

**Algorithm:** AES-256-GCM with random IV

---

## Production Considerations

### 1. Key Management

**Current (PoC):**
- HMAC with derived key from phone + PIN
- Symmetric encryption

**Production:**
- **Public/Private Key Pairs** (RSA 2048-bit or ECDSA)
- **Hardware Security Module (HSM)** for key storage
- **Key Rotation** policy
- **Certificate Authority** for public key verification

### 2. Payload Encryption

**Current:** AES-256-GCM with derived key

**Production:**
- Use **Key Derivation Function** (PBKDF2, scrypt, Argon2)
- Implement **Key Wrapping** (encrypt encryption keys)
- Use **Hardware-backed Keystore** (Android Keystore, iOS Keychain)

### 3. Sync Strategy

**Options:**

1. **Periodic Sync** (every X minutes when online)
2. **Manual Sync** (user-initiated)
3. **Background Sync** (when app in background)
4. **Push-based Sync** (server notifies clients)

**Recommended:** Combination of manual + periodic + background

### 4. Conflict Resolution

**Automated:**
- Double-spend: Keep first, reject duplicates
- Nonce reuse: Reject automatically
- Timestamp tolerance: Accept within 30 days

**Manual:**
- Insufficient funds: User tops up, retry
- Invalid hash: Reject, notify user
- Chain broken: Admin review

### 5. Scalability

**Optimizations:**
- **Batch Processing:** Process multiple transactions together
- **Async Sync:** Use message queues (RabbitMQ, Kafka)
- **Distributed Validation:** Parallel chain validation
- **Caching:** Cache chain state for quick lookups

### 6. Monitoring & Alerts

**Metrics to Monitor:**
- Pending transaction count
- Sync success rate
- Conflict rate by type
- Chain validation failures
- Average sync time

**Alerts:**
- High conflict rate (> 5%)
- Chain integrity violations
- Sync failures (> 10%)
- Old pending transactions (> 7 days)

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testHashGeneration() {
    String hash = TransactionHashUtil.generateTransactionHash(
        "08012345678", "08087654321", "1000",
        LocalDateTime.now(), "nonce123", "prevhash");

    assertEquals(64, hash.length()); // SHA-256 = 64 hex chars
    assertTrue(TransactionHashUtil.isValidHashFormat(hash));
}

@Test
public void testChainIntegrity() {
    List<OfflineTransaction> chain = createTestChain();
    assertTrue(chainValidator.validateChain(chain));

    // Break chain
    chain.get(2).setPreviousHash("invalid");
    assertFalse(chainValidator.validateChain(chain));
}

@Test
public void testNonceUniqueness() {
    String nonce1 = NonceGenerator.generateUUIDNonce();
    String nonce2 = NonceGenerator.generateUUIDNonce();

    assertNotEquals(nonce1, nonce2);
}
```

### Integration Tests

```java
@Test
@Transactional
public void testOfflineTransactionSync() {
    // Create offline transaction
    OfflineTransaction tx = createOfflineTransaction();

    // Sync
    SyncResult result = syncService.syncOfflineTransactions(userId, List.of(tx));

    // Verify
    assertEquals(1, result.getSyncedCount());
    assertEquals(0, result.getConflictCount());
    assertEquals(SyncStatus.SYNCED, tx.getSyncStatus());
}

@Test
public void testConflictDetection() {
    // Create duplicate transaction
    OfflineTransaction tx1 = createTransaction("nonce1");
    OfflineTransaction tx2 = createTransaction("nonce1"); // Same nonce

    // Sync
    SyncResult result = syncService.syncOfflineTransactions(userId, List.of(tx1, tx2));

    // Verify conflict detected
    assertEquals(1, result.getConflictCount());
    assertEquals(ConflictType.NONCE_REUSED, result.getConflicts().get(0).getConflictType());
}
```

---

## Related Documentation

- [README.md](./README.md) - Main project documentation
- [TRANSACTION_API.md](./TRANSACTION_API.md) - Online transaction API
- [SECURITY_API.md](./SECURITY_API.md) - Authentication & security

---

**Last Updated:** December 11, 2024
**Version:** 1.0
**Status:** ✅ DESIGN COMPLETE - Ready for Implementation
