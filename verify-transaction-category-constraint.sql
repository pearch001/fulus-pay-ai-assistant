-- Verification and Diagnostic Script for Transaction Category Check Constraint
-- Use this script to diagnose and verify the transactions_category_check constraint

-- ============================================================================
-- 1. CHECK CURRENT CONSTRAINT DEFINITION
-- ============================================================================
SELECT
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'transactions'::regclass
AND conname = 'transactions_category_check';

-- Expected output should include all these values:
-- 'BILL_PAYMENT', 'TRANSFER', 'SAVINGS', 'WITHDRAWAL', 'DEPOSIT',
-- 'AIRTIME', 'DATA', 'SHOPPING', 'FOOD', 'TRANSPORT',
-- 'ENTERTAINMENT', 'UTILITIES', 'HEALTHCARE', 'EDUCATION', 'FEES', 'OTHER'


-- ============================================================================
-- 2. CHECK IF CONSTRAINT EXISTS
-- ============================================================================
SELECT
    EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'transactions'::regclass
        AND conname = 'transactions_category_check'
    ) AS constraint_exists;


-- ============================================================================
-- 3. LIST ALL CURRENT CATEGORY VALUES IN USE
-- ============================================================================
SELECT
    category,
    COUNT(*) AS count
FROM transactions
GROUP BY category
ORDER BY count DESC;


-- ============================================================================
-- 4. FIND TRANSACTIONS THAT WOULD VIOLATE THE NEW CONSTRAINT (if any)
-- ============================================================================
-- This query will show any transactions with invalid categories
-- (Should return no rows if data is clean)
SELECT
    id,
    user_id,
    category,
    description,
    created_at
FROM transactions
WHERE category NOT IN (
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
)
ORDER BY created_at DESC
LIMIT 100;


-- ============================================================================
-- 5. TEST INSERT WITH FEES CATEGORY (ROLLBACK)
-- ============================================================================
-- This will test if FEES category works without actually inserting data
BEGIN;
    INSERT INTO transactions (
        id, user_id, type, category, amount, description,
        balance_after, reference, status, created_at, is_offline
    ) VALUES (
        gen_random_uuid(),
        gen_random_uuid(),
        'DEBIT',
        'FEES',
        50.00,
        'Test fee transaction',
        1000.00,
        'FEE-TEST-' || gen_random_uuid(),
        'COMPLETED',
        NOW(),
        false
    );

    -- Check if it was inserted
    SELECT 'FEES category test: SUCCESS' AS result;
ROLLBACK;


-- ============================================================================
-- 6. GET TABLE STRUCTURE
-- ============================================================================
\d transactions


-- ============================================================================
-- 7. SHOW ALL CONSTRAINTS ON TRANSACTIONS TABLE
-- ============================================================================
SELECT
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'transactions'::regclass
ORDER BY contype, conname;

-- Constraint types:
-- 'c' = check constraint
-- 'f' = foreign key
-- 'p' = primary key
-- 'u' = unique constraint

