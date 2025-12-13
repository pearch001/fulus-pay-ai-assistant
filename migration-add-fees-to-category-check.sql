-- Migration: Add FEES to transactions_category_check constraint
-- Date: 2025-12-13
-- Reason: The FEES category exists in the Java enum but is missing from the database check constraint

-- Step 1: Drop the existing check constraint
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_category_check;

-- Step 2: Add the updated check constraint with FEES included
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

-- Verify the constraint was created successfully
-- SELECT conname, contype, consrc
-- FROM pg_constraint
-- WHERE conname = 'transactions_category_check';

-- Test: You should now be able to insert/update transactions with category = 'FEES'
-- Example:
-- UPDATE transactions SET category = 'FEES' WHERE description LIKE '%fee%';

