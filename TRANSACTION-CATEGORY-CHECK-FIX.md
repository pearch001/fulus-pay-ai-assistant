# Transaction Category Check Constraint Fix

## Problem
When trying to save transactions with `category = 'FEES'`, PostgreSQL throws the error:
```
new row for relation "transactions" violates check constraint "transactions_category_check"
```

This happens because:
1. The Java enum `TransactionCategory` includes `FEES`
2. The database check constraint `transactions_category_check` does NOT include `FEES`

## Root Cause
The database schema was created before the `FEES` category was added to the enum, or the schema wasn't properly updated when the enum was modified.

## Solution
Update the PostgreSQL check constraint to include all categories from the `TransactionCategory` enum.

### Migration Script
File: `migration-add-fees-to-category-check.sql`

```sql
-- Drop the existing constraint
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_category_check;

-- Add updated constraint with FEES
ALTER TABLE transactions ADD CONSTRAINT transactions_category_check 
CHECK (category IN (
    'BILL_PAYMENT',
    'TRANSFER',
    'SAVINGS',
    'WITHDRAWAL',
    'DEPOSIT',
    'AIRTIME',
    'DATA',
    'SHOPPING',
    'FOOD',
    'TRANSPORT',
    'ENTERTAINMENT',
    'UTILITIES',
    'HEALTHCARE',
    'EDUCATION',
    'FEES',
    'OTHER'
));
```

## How to Apply

### Option 1: Using psql command-line
```bash
psql -U your_username -d your_database -f migration-add-fees-to-category-check.sql
```

### Option 2: Using pgAdmin or any PostgreSQL client
1. Connect to your database
2. Open the SQL query tool
3. Copy and paste the content of `migration-add-fees-to-category-check.sql`
4. Execute the script

### Option 3: Using Spring Boot application
If `spring.jpa.hibernate.ddl-auto=update`, you can:
1. Temporarily set it to `validate` or `none`
2. Run the migration script manually
3. Restart the application

## Verification

After running the migration, verify the constraint:

```sql
-- Check the constraint definition
SELECT conname, contype, pg_get_constraintdef(oid) as definition
FROM pg_constraint 
WHERE conrelid = 'transactions'::regclass 
AND conname = 'transactions_category_check';
```

Expected output should show `FEES` in the list.

### Test the Fix
```sql
-- This should now work without errors
INSERT INTO transactions (
    id, user_id, type, category, amount, description, 
    balance_after, reference, status, created_at, is_offline
) VALUES (
    gen_random_uuid(),
    'some-user-uuid',
    'DEBIT',
    'FEES',
    50.00,
    'Inter-bank transfer fee',
    1000.00,
    'FEE-TEST-123',
    'COMPLETED',
    NOW(),
    false
);
```

## Impact on Code

The following code in `TransferService.java` creates FEES transactions:

```java
// Create fee transaction
Transaction feeTransaction = createTransaction(
        sender.getId(),
        TransactionType.DEBIT,
        INTER_BANK_TRANSFER_FEE,
        sender.getBalance(),
        "FEE-" + reference,
        "Inter-bank transfer fee",
        null,
        null
);
feeTransaction.setCategory(TransactionCategory.FEES);  // ← This was failing
transactionRepository.save(feeTransaction);
```

After applying the migration, this code will work correctly.

## Prevention
To prevent this issue in the future:

1. **Keep enum and database in sync**: When adding new enum values, always update the database constraint
2. **Use migrations**: Create migration scripts for all schema changes
3. **Testing**: Test all enum values in integration tests
4. **Documentation**: Document all allowed values in API docs

## Related Files
- `TransactionCategory.java` - Java enum definition
- `Transaction.java` - Entity with category field
- `TransferService.java` - Creates FEES transactions for inter-bank transfers
- `migration-add-fees-to-category-check.sql` - Migration script

## See Also
- PostgreSQL CHECK Constraints: https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS
- JPA Enum Mapping: https://docs.oracle.com/javaee/7/api/javax/persistence/EnumType.html

