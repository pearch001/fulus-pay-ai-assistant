# Offline Transaction Validation Service - Documentation

Complete documentation for the OfflineTransactionValidationService with validation methods, DTOs, and usage examples.

---

## Overview

The `OfflineTransactionValidationService` provides comprehensive validation for offline transactions, including:

1. **Chain Integrity Validation** - Verify blockchain-like hash chain
2. **Payload Validation** - Decrypt, verify signatures, check timestamps
3. **Double-Spending Detection** - Detect transactions exceeding balance

### Key Features

✅ **Hash Chain Verification** - Ensures each transaction properly links to previous
✅ **Duplicate Detection** - Prevents replay attacks via hash/nonce checking
✅ **Signature Verification** - Validates transaction authenticity
✅ **Timestamp Validation** - Rejects old/future transactions
✅ **Balance Tracking** - Detects insufficient funds
✅ **Comprehensive Logging** - Full audit trail for security

---

## Service Methods

### 1. validateTransactionChain()

**Purpose:** Validate blockchain-like chain integrity

**Signature:**
```java
public ChainValidationResult validateTransactionChain(
    UUID userId,
    List<OfflineTransaction> transactions
)
```

**Validations Performed:**

| Check | Description | Error Type |
|-------|-------------|------------|
| Duplicate Hash | Same hash used multiple times | `DUPLICATE_HASH` |
| Duplicate Nonce | Same nonce used multiple times | `DUPLICATE_NONCE` |
| Chain Link | previousHash matches previous TX | `CHAIN_BROKEN` |
| Genesis Hash | First TX links to genesis | `INVALID_GENESIS` |
| Hash Calculation | Hash matches calculated value | `INVALID_HASH` |
| Chronological Order | Timestamps in order | `INVALID_CHRONOLOGY` |

**Example Usage:**

```java
@Autowired
private OfflineTransactionValidationService validationService;

public void validateChain(UUID userId) {
    // Get pending transactions
    List<OfflineTransaction> transactions =
        offlineTransactionRepository.findPendingByUserId(userId);

    // Validate chain
    ChainValidationResult result =
        validationService.validateTransactionChain(userId, transactions);

    if (result.isValid()) {
        log.info("Chain valid: {} transactions", result.getTotalTransactions());
    } else {
        log.error("Chain invalid: {} errors found", result.getErrorCount());

        for (ChainValidationError error : result.getErrors()) {
            log.error("Error at index {}: {} - {}",
                error.getTransactionIndex(),
                error.getErrorType(),
                error.getErrorMessage());
        }
    }
}
```

**Response Example:**

```json
{
  "valid": false,
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "totalTransactions": 5,
  "validTransactions": 3,
  "invalidTransactions": 2,
  "errors": [
    {
      "transactionId": "abc123...",
      "transactionHash": "a7f3c2e1...",
      "transactionIndex": 2,
      "errorType": "CHAIN_BROKEN",
      "errorMessage": "Previous hash does not match previous transaction hash",
      "expectedValue": "def456...",
      "actualValue": "xyz789..."
    },
    {
      "transactionId": "def456...",
      "transactionHash": "b8g4d3f2...",
      "transactionIndex": 4,
      "errorType": "DUPLICATE_NONCE",
      "errorMessage": "Duplicate nonce detected - replay attack",
      "expectedValue": null,
      "actualValue": "nonce123"
    }
  ]
}
```

**Logging Output:**

```log
INFO: Validating transaction chain for user: 123e4567 with 5 transactions
DEBUG: Validating transaction 1/5: hash=a7f3c2e1...
DEBUG: Validating transaction 2/5: hash=def456...
WARN: SECURITY ALERT: Duplicate nonce detected for user 123e4567 at index 4: nonce123
ERROR: Chain broken for user 123e4567 at index 2: expected=def456..., actual=xyz789...
WARN: Chain validation FAILED for user 123e4567: 2 errors found in 5 transactions
```

---

### 2. validateTransactionPayload()

**Purpose:** Validate individual transaction payload and metadata

**Signature:**
```java
public PayloadValidationResult validateTransactionPayload(
    OfflineTransaction transaction
)
```

**Validations Performed:**

| Check | Description | Field Set |
|-------|-------------|-----------|
| Amount | > 0 | `amountValid` |
| Timestamp | Within acceptable range | `timestampValid` |
| Nonce | Unique in database | `nonceUnique` |
| Signature | Valid for sender | `signatureValid` |
| Payload | Not empty | `decryptedPayload` |

**Configuration:**

```yaml
# application.yml
offline:
  transaction:
    max-age-days: 30            # Max transaction age (default: 30 days)
    future-tolerance-minutes: 5  # Future timestamp tolerance (default: 5 min)
```

**Example Usage:**

```java
public void validatePayload(OfflineTransaction transaction) {
    PayloadValidationResult result =
        validationService.validateTransactionPayload(transaction);

    if (result.isValid()) {
        log.info("Payload valid for TX: {}", transaction.getId());
    } else {
        log.error("Payload invalid: {} errors", result.getErrors().size());

        // Check specific validations
        if (!result.isSignatureValid()) {
            log.error("SECURITY: Invalid signature - possible forgery");
        }

        if (!result.isNonceUnique()) {
            log.error("SECURITY: Nonce reused - replay attack");
        }

        if (!result.isTimestampValid()) {
            log.warn("Timestamp out of range");
        }
    }
}
```

**Response Example:**

```json
{
  "valid": false,
  "transactionId": "abc123...",
  "transactionHash": "a7f3c2e1...",
  "errors": [
    "Invalid signature - transaction may be forged",
    "Nonce already used - replay attack detected"
  ],
  "warnings": [
    "Transaction is 15 days old"
  ],
  "decryptedPayload": "Payload present (not decrypted in validation)",
  "signatureValid": false,
  "nonceUnique": false,
  "timestampValid": true,
  "amountValid": true
}
```

**Logging Output:**

```log
INFO: Validating payload for transaction: abc123 (hash: a7f3c2e1...)
DEBUG: Signature verified for transaction abc123
WARN: Transaction abc123 timestamp too old: 2024-11-01T10:00:00 (max age: 2024-11-11T10:00:00)
ERROR: SECURITY ALERT: Nonce reuse detected for transaction abc123: nonce123
ERROR: SECURITY ALERT: Invalid signature for transaction abc123 (sender: 08012345678)
WARN: Payload validation FAILED for transaction abc123: 2 errors
```

---

### 3. detectDoubleSpending()

**Purpose:** Detect transactions that would exceed available balance

**Signature:**
```java
public DoubleSpendingReport detectDoubleSpending(
    UUID userId,
    List<OfflineTransaction> pendingTransactions
)
```

**Algorithm:**

```
1. Get user's current balance (₦B)
2. Sort transactions by timestamp (chronological)
3. For each transaction:
   a. If sender = user: runningBalance -= amount (DEBIT)
   b. If recipient = user: runningBalance += amount (CREDIT)
   c. If runningBalance < 0: FLAG transaction
4. Return report with flagged transactions
```

**Example Usage:**

```java
public void checkDoubleSpending(UUID userId) {
    // Get pending transactions
    List<OfflineTransaction> pending =
        offlineTransactionRepository.findPendingByUserId(userId);

    // Detect double-spending
    DoubleSpendingReport report =
        validationService.detectDoubleSpending(userId, pending);

    log.info("Balance: ₦{}, Debits: ₦{}, Credits: ₦{}, Projected: ₦{}",
        report.getLastKnownBalance(),
        report.getTotalDebits(),
        report.getTotalCredits(),
        report.getProjectedBalance());

    if (report.isHasDoubleSpending()) {
        log.error("Double-spending detected: {} transactions flagged",
            report.getFlaggedTransactions());

        for (FlaggedTransaction flagged : report.getFlaggedTransactionList()) {
            log.error("Flagged TX {}: amount=₦{}, balance before=₦{}, after=₦{}, reason={}",
                flagged.getTransactionIndex(),
                flagged.getAmount(),
                flagged.getBalanceBeforeTransaction(),
                flagged.getBalanceAfterTransaction(),
                flagged.getReason());
        }

        // Create conflicts
        createConflicts(report);
    }
}
```

**Response Example:**

```json
{
  "userId": "123e4567",
  "lastKnownBalance": 5000.00,
  "totalDebits": 7000.00,
  "totalCredits": 1000.00,
  "projectedBalance": -1000.00,
  "hasDoubleSpending": true,
  "totalTransactions": 5,
  "flaggedTransactions": 2,
  "flaggedTransactionList": [
    {
      "transactionId": "tx4...",
      "transactionHash": "hash4...",
      "amount": 3000.00,
      "balanceBeforeTransaction": 500.00,
      "balanceAfterTransaction": -2500.00,
      "reason": "Insufficient funds: balance would be ₦-2500.00 after transaction",
      "transactionIndex": 3
    },
    {
      "transactionId": "tx5...",
      "transactionHash": "hash5...",
      "amount": 1000.00,
      "balanceBeforeTransaction": -2500.00,
      "balanceAfterTransaction": -3500.00,
      "reason": "Insufficient funds: balance would be ₦-3500.00 after transaction",
      "transactionIndex": 4
    }
  ]
}
```

**Scenario Example:**

```
User Balance: ₦5,000

Transaction Timeline:
1. TX1: Send ₦2,000 → Balance: ₦3,000 ✅
2. TX2: Send ₦2,000 → Balance: ₦1,000 ✅
3. TX3: Receive ₦1,000 → Balance: ₦2,000 ✅
4. TX4: Send ₦3,000 → Balance: ₦-1,000 ❌ FLAGGED (insufficient funds)
5. TX5: Send ₦1,000 → Balance: ₦-2,000 ❌ FLAGGED (insufficient funds)

Result: 2 transactions flagged for double-spending
```

**Logging Output:**

```log
INFO: Detecting double-spending for user 123e4567 with 5 pending transactions
DEBUG: User 123e4567 current balance: ₦5000.00
DEBUG: Processing transaction 1/5: hash=tx1, amount=₦2000.00, type=DEBIT
DEBUG: Processing transaction 2/5: hash=tx2, amount=₦2000.00, type=DEBIT
DEBUG: Processing transaction 3/5: hash=tx3, amount=₦1000.00, type=CREDIT
DEBUG: Processing transaction 4/5: hash=tx4, amount=₦3000.00, type=DEBIT
WARN: DOUBLE-SPENDING DETECTED for user 123e4567 at transaction 3: balance before=₦2000.00, amount=₦3000.00, balance after=₦-1000.00
ERROR: Double-spending DETECTED for user 123e4567: 2 flagged transactions, total debits=₦7000.00, total credits=₦1000.00, projected balance=₦-1000.00
```

---

### 4. validateAllTransactions()

**Purpose:** Run all validations together (comprehensive check)

**Signature:**
```java
public Map<String, Object> validateAllTransactions(
    UUID userId,
    List<OfflineTransaction> transactions
)
```

**Returns:**
```json
{
  "chainValidation": { ChainValidationResult },
  "payloadValidation": [ PayloadValidationResult[] ],
  "doubleSpendingReport": { DoubleSpendingReport },
  "overallValid": true/false,
  "totalTransactions": 5,
  "validTransactions": 3
}
```

**Example Usage:**

```java
public boolean validateBeforeSync(UUID userId, List<OfflineTransaction> transactions) {
    Map<String, Object> results =
        validationService.validateAllTransactions(userId, transactions);

    boolean overallValid = (boolean) results.get("overallValid");

    if (!overallValid) {
        // Handle validation failures
        ChainValidationResult chain =
            (ChainValidationResult) results.get("chainValidation");

        DoubleSpendingReport doubleSpend =
            (DoubleSpendingReport) results.get("doubleSpendingReport");

        // Create conflicts, notify user, etc.
    }

    return overallValid;
}
```

---

## DTO Specifications

### ChainValidationResult

**Fields:**

```java
boolean valid                           // Overall chain validity
String userId                           // User ID
Integer totalTransactions               // Total transactions validated
Integer validTransactions               // Number of valid transactions
Integer invalidTransactions             // Number of invalid transactions
List<ChainValidationError> errors       // List of errors found
```

**ChainValidationError:**

```java
UUID transactionId                      // Transaction ID
String transactionHash                  // Transaction hash
Integer transactionIndex                // Index in chain (0-based)
String errorType                        // Error type enum
String errorMessage                     // Human-readable message
String expectedValue                    // Expected value (for comparison)
String actualValue                      // Actual value (for comparison)
```

**Error Types:**

- `DUPLICATE_HASH` - Same hash found multiple times
- `DUPLICATE_NONCE` - Same nonce found multiple times
- `CHAIN_BROKEN` - previousHash doesn't match
- `INVALID_GENESIS` - First transaction doesn't link to genesis
- `INVALID_HASH` - Hash calculation doesn't match
- `INVALID_CHRONOLOGY` - Timestamp out of order

---

### PayloadValidationResult

**Fields:**

```java
boolean valid                           // Overall payload validity
UUID transactionId                      // Transaction ID
String transactionHash                  // Transaction hash
List<String> errors                     // List of errors
List<String> warnings                   // List of warnings (non-fatal)
String decryptedPayload                 // Decrypted payload (if successful)
boolean signatureValid                  // Signature verification result
boolean nonceUnique                     // Nonce uniqueness check
boolean timestampValid                  // Timestamp validation result
boolean amountValid                     // Amount validation result
```

---

### DoubleSpendingReport

**Fields:**

```java
String userId                           // User ID
BigDecimal lastKnownBalance             // Balance before offline TXs
BigDecimal totalDebits                  // Sum of all debits
BigDecimal totalCredits                 // Sum of all credits
BigDecimal projectedBalance             // Calculated balance after TXs
boolean hasDoubleSpending               // Any transactions flagged?
Integer totalTransactions               // Total transactions checked
Integer flaggedTransactions             // Number of flagged transactions
List<FlaggedTransaction> flaggedList    // Detailed flagged transactions
```

**FlaggedTransaction:**

```java
UUID transactionId                      // Transaction ID
String transactionHash                  // Transaction hash
BigDecimal amount                       // Transaction amount
BigDecimal balanceBeforeTransaction     // Balance before this TX
BigDecimal balanceAfterTransaction      // Balance after this TX (negative)
String reason                           // Why flagged
Integer transactionIndex                // Index in chain
```

---

## Integration Example

### Complete Sync Flow with Validation

```java
@Service
public class OfflineTransactionSyncService {

    @Autowired
    private OfflineTransactionValidationService validationService;

    @Autowired
    private SyncConflictRepository conflictRepository;

    @Transactional
    public SyncResult syncOfflineTransactions(UUID userId) {
        // 1. Get pending transactions
        List<OfflineTransaction> pending =
            offlineTransactionRepository.findPendingByUserId(userId);

        if (pending.isEmpty()) {
            return SyncResult.noTransactions();
        }

        log.info("Syncing {} offline transactions for user {}", pending.size(), userId);

        // 2. Validate chain integrity
        ChainValidationResult chainResult =
            validationService.validateTransactionChain(userId, pending);

        if (!chainResult.isValid()) {
            // Create conflicts for chain errors
            for (ChainValidationError error : chainResult.getErrors()) {
                createChainConflict(error);
            }
            return SyncResult.chainInvalid(chainResult);
        }

        // 3. Validate each payload
        List<OfflineTransaction> validTransactions = new ArrayList<>();

        for (OfflineTransaction tx : pending) {
            PayloadValidationResult payloadResult =
                validationService.validateTransactionPayload(tx);

            if (payloadResult.isValid()) {
                validTransactions.add(tx);
            } else {
                // Create conflict
                createPayloadConflict(tx, payloadResult);
                tx.markAsConflict(payloadResult.getErrors().toString());
            }
        }

        // 4. Detect double-spending
        DoubleSpendingReport doubleSpendReport =
            validationService.detectDoubleSpending(userId, validTransactions);

        if (doubleSpendReport.isHasDoubleSpending()) {
            // Create conflicts for flagged transactions
            for (FlaggedTransaction flagged : doubleSpendReport.getFlaggedTransactionList()) {
                createDoubleSpendConflict(flagged);
            }

            // Remove flagged transactions from sync
            Set<UUID> flaggedIds = doubleSpendReport.getFlaggedTransactionList()
                .stream()
                .map(FlaggedTransaction::getTransactionId)
                .collect(Collectors.toSet());

            validTransactions.removeIf(tx -> flaggedIds.contains(tx.getId()));
        }

        // 5. Process valid transactions
        List<Transaction> syncedTransactions = new ArrayList<>();

        for (OfflineTransaction offlineTx : validTransactions) {
            try {
                // Create online transaction
                Transaction onlineTx = processOfflineTransaction(offlineTx);
                syncedTransactions.add(onlineTx);

                // Mark as synced
                offlineTx.markAsSynced(onlineTx.getId());

            } catch (Exception e) {
                log.error("Failed to sync transaction {}: {}", offlineTx.getId(), e.getMessage());
                offlineTx.markAsFailed(e.getMessage());
            }
        }

        // 6. Save all
        offlineTransactionRepository.saveAll(pending);

        // 7. Return result
        return SyncResult.builder()
                .totalTransactions(pending.size())
                .syncedCount(syncedTransactions.size())
                .conflictCount(chainResult.getErrorCount() +
                              doubleSpendReport.getFlaggedTransactions())
                .failedCount(pending.size() - syncedTransactions.size())
                .transactions(syncedTransactions)
                .build();
    }

    private void createChainConflict(ChainValidationError error) {
        SyncConflict conflict = SyncConflict.builder()
                .transactionId(error.getTransactionId())
                .conflictType(mapErrorTypeToConflictType(error.getErrorType()))
                .conflictDescription(error.getErrorMessage())
                .expectedPreviousHash(error.getExpectedValue())
                .actualPreviousHash(error.getActualValue())
                .priority(5) // Chain errors are critical
                .build();

        conflictRepository.save(conflict);
    }
}
```

---

## Security Audit Logging

### Log Levels

| Level | Use Case |
|-------|----------|
| DEBUG | Normal validation steps |
| INFO | Successful validations, summaries |
| WARN | Failed validations, old transactions |
| ERROR | Security issues (replay, forgery, double-spend) |

### Security Alert Patterns

```log
# Replay Attack Detection
WARN: SECURITY ALERT: Duplicate nonce detected for user {userId} at index {i}: {nonce}
ERROR: SECURITY ALERT: Nonce reuse detected for transaction {txId}: {nonce}

# Forgery Detection
ERROR: SECURITY ALERT: Invalid signature for transaction {txId} (sender: {phone})

# Chain Tampering
ERROR: Chain broken for user {userId} at index {i}: expected={expected}, actual={actual}
ERROR: Invalid hash for user {userId} at index {i}: expected={calculated}, actual={stored}

# Double-Spending
WARN: DOUBLE-SPENDING DETECTED for user {userId} at transaction {i}: balance before=₦{before}, amount=₦{amount}, balance after=₦{after}
ERROR: Double-spending DETECTED for user {userId}: {count} flagged transactions
```

---

## Configuration

```yaml
# application.yml
offline:
  transaction:
    # Maximum age for transactions (in days)
    max-age-days: 30

    # Future timestamp tolerance (in minutes)
    future-tolerance-minutes: 5

    # Enable strict validation (reject warnings)
    strict-validation: false
```

---

## Testing

### Unit Test Example

```java
@SpringBootTest
public class OfflineTransactionValidationServiceTest {

    @Autowired
    private OfflineTransactionValidationService validationService;

    @Test
    public void testValidChain() {
        // Create valid chain
        List<OfflineTransaction> chain = createValidChain();

        // Validate
        ChainValidationResult result =
            validationService.validateTransactionChain(userId, chain);

        // Assert
        assertTrue(result.isValid());
        assertEquals(3, result.getTotalTransactions());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    public void testBrokenChain() {
        // Create chain with broken link
        List<OfflineTransaction> chain = createChainWithBrokenLink();

        // Validate
        ChainValidationResult result =
            validationService.validateTransactionChain(userId, chain);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getErrorType().equals("CHAIN_BROKEN")));
    }

    @Test
    public void testDoubleSpending() {
        // Create transactions exceeding balance
        List<OfflineTransaction> transactions = createTransactionsExceedingBalance();

        // Detect
        DoubleSpendingReport report =
            validationService.detectDoubleSpending(userId, transactions);

        // Assert
        assertTrue(report.isHasDoubleSpending());
        assertTrue(report.getFlaggedTransactions() > 0);
        assertTrue(report.getProjectedBalance().compareTo(BigDecimal.ZERO) < 0);
    }
}
```

---

## Related Documentation

- [OFFLINE_TRANSACTION_DESIGN.md](./OFFLINE_TRANSACTION_DESIGN.md) - System design
- [OFFLINE_TRANSACTION_IMPLEMENTATION.md](./OFFLINE_TRANSACTION_IMPLEMENTATION.md) - Implementation details

---

**Last Updated:** December 11, 2024
**Version:** 1.0
**Status:** ✅ COMPLETE
