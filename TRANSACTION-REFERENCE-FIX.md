# Transaction Reference Unique Constraint Fix

## Problem
The `transactions` table had a `UNIQUE` constraint on the `reference` column, which prevented internal transfers from being saved properly. 

In an internal transfer:
- One **DEBIT** transaction is created for the sender
- One **CREDIT** transaction is created for the recipient
- Both transactions share the **same reference** to link them together

The unique constraint caused the second transaction (credit) to fail with a duplicate key violation.

## Solution
Removed the `unique = true` constraint from the `reference` field in the `Transaction` entity.

### Changes Made

**File: `Transaction.java`**
```java
// Before:
@Column(unique = true, nullable = false, length = 100)
private String reference;

// After:
@Column(nullable = false, length = 100)
private String reference;
```

### Why This is Safe
1. **Index remains**: The `idx_reference` index is still present for fast lookups
2. **Business logic**: Multiple transactions can share the same reference:
   - Internal transfers (debit + credit)
   - Batch transactions
   - Refunds and reversals
3. **Uniqueness**: The `id` field (UUID) remains the primary key and ensures uniqueness

## Database Migration

### Development Environment
If using `spring.jpa.hibernate.ddl-auto=update`, Hibernate will automatically drop the constraint.

### Production Environment
Run the migration script: `migration-remove-reference-unique-constraint.sql`

```sql
-- For PostgreSQL
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS uk_reference;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_reference_key;

-- For MySQL
ALTER TABLE transactions DROP INDEX uk_reference;
```

### Verification
After migration, verify the constraint is removed:

**PostgreSQL:**
```sql
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'transactions' AND constraint_type = 'UNIQUE';
```

**MySQL:**
```sql
SHOW INDEX FROM transactions WHERE Column_name = 'reference';
```

## Testing
After applying the fix, internal transfers will work correctly:

1. Sender's balance is debited
2. Recipient's balance is credited
3. Both transactions are saved with the same reference
4. Transaction history shows linked debit/credit pairs

## Example Transaction Records
```
| id   | userId    | type   | amount | reference        | description          |
|------|-----------|--------|--------|------------------|----------------------|
| uuid1| sender-id | DEBIT  | 1000   | INT-1234567-ABC  | Transfer to Jane Doe |
| uuid2| recip-id  | CREDIT | 1000   | INT-1234567-ABC  | Transfer from John   |
```

Notice both transactions share the reference `INT-1234567-ABC`.

## Related Code
- `TransferService.internalTransfer()` - Creates paired debit/credit transactions
- `TransferService.generateReference()` - Generates unique references for transfer pairs

