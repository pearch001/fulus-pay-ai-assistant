-- =====================================================
-- Rollback Migration: U6__rollback_admin_insights_tables.sql
-- Description: Rollback script for V6__create_admin_insights_tables.sql
--              Drops all admin insights tables and related objects
-- Author: System
-- Date: 2024-12-14
-- =====================================================

-- WARNING: This will permanently delete all admin conversation data!
-- Only use this in development or if you're sure you want to remove the feature.

-- =====================================================
-- 1. Drop Triggers
-- =====================================================
DROP TRIGGER IF EXISTS update_admin_conversation_timestamp ON admin_conversations;

-- =====================================================
-- 2. Drop Tables (in reverse dependency order)
-- =====================================================
-- Drop admin_chat_messages first (has foreign key to admin_conversations)
DROP TABLE IF EXISTS admin_chat_messages CASCADE;

-- Drop admin_audit_logs (independent table)
DROP TABLE IF EXISTS admin_audit_logs CASCADE;

-- Drop admin_conversations last
DROP TABLE IF EXISTS admin_conversations CASCADE;

-- =====================================================
-- 3. Drop Function (optional - only if not used elsewhere)
-- =====================================================
-- Uncomment if you want to drop the update_updated_at_column function
-- Note: Only drop this if no other tables use it!
-- DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- =====================================================
-- 4. Verification
-- =====================================================
-- Verify tables are dropped
-- SELECT table_name FROM information_schema.tables
-- WHERE table_schema = 'public'
-- AND table_name IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');

-- Should return 0 rows if successful

-- =====================================================
-- END OF ROLLBACK
-- =====================================================

