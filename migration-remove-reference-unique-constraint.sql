-- Migration: Remove unique constraint from transactions.reference column
-- Date: 2025-12-13
-- Reason: Internal transfers use the same reference for both debit and credit transactions

-- For PostgreSQL
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS uk_reference;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_reference_key;

-- For MySQL
-- ALTER TABLE transactions DROP INDEX uk_reference;

-- Note: The index on reference column (idx_reference) will remain for performance
-- This allows multiple transactions to share the same reference (e.g., debit and credit in internal transfers)

-- Verify the change
-- SELECT constraint_name, constraint_type FROM information_schema.table_constraints
-- WHERE table_name = 'transactions' AND constraint_type = 'UNIQUE';

