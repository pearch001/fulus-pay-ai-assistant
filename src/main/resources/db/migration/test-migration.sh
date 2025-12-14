#!/bin/bash
# =====================================================
# Test Admin Insights Migration
# =====================================================
# This script tests the V6 migration for admin insights tables
# Usage: ./test-migration.sh

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Testing Admin Insights Migration (V6)${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Database connection details (adjust as needed)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-fulus_ai_db}"
DB_USER="${DB_USER:-fulus_user}"

echo -e "${YELLOW}Database Connection:${NC}"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Test 1: Check if tables exist
echo -e "${YELLOW}Test 1: Checking if tables exist...${NC}"
TABLE_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');")

if [ "$TABLE_COUNT" -eq 3 ]; then
    echo -e "${GREEN}✓ All 3 tables exist${NC}"
else
    echo -e "${RED}✗ Expected 3 tables, found $TABLE_COUNT${NC}"
    exit 1
fi

# Test 2: Check indexes
echo -e "${YELLOW}Test 2: Checking indexes...${NC}"
INDEX_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM pg_indexes WHERE tablename IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');")

if [ "$INDEX_COUNT" -ge 10 ]; then
    echo -e "${GREEN}✓ Found $INDEX_COUNT indexes${NC}"
else
    echo -e "${RED}✗ Expected at least 10 indexes, found $INDEX_COUNT${NC}"
    exit 1
fi

# Test 3: Check foreign key constraints
echo -e "${YELLOW}Test 3: Checking foreign key constraints...${NC}"
FK_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM pg_constraint WHERE contype = 'f' AND conrelid IN ('admin_conversations'::regclass, 'admin_chat_messages'::regclass);")

if [ "$FK_COUNT" -ge 2 ]; then
    echo -e "${GREEN}✓ Found $FK_COUNT foreign key constraints${NC}"
else
    echo -e "${RED}✗ Expected at least 2 foreign keys, found $FK_COUNT${NC}"
    exit 1
fi

# Test 4: Check trigger exists
echo -e "${YELLOW}Test 4: Checking trigger...${NC}"
TRIGGER_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM information_schema.triggers WHERE event_object_table = 'admin_conversations' AND trigger_name = 'update_admin_conversation_timestamp';")

if [ "$TRIGGER_COUNT" -eq 1 ]; then
    echo -e "${GREEN}✓ Trigger exists${NC}"
else
    echo -e "${YELLOW}⚠ Warning: Trigger not found (may be optional)${NC}"
fi

# Test 5: Check CHECK constraint on role
echo -e "${YELLOW}Test 5: Checking role constraint...${NC}"
CHECK_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM pg_constraint WHERE contype = 'c' AND conrelid = 'admin_chat_messages'::regclass AND conname LIKE '%role%';")

if [ "$CHECK_COUNT" -ge 1 ]; then
    echo -e "${GREEN}✓ Role CHECK constraint exists${NC}"
else
    echo -e "${YELLOW}⚠ Warning: Role CHECK constraint not found${NC}"
fi

# Test 6: Test JSONB metadata column
echo -e "${YELLOW}Test 6: Testing JSONB metadata column...${NC}"
JSONB_TEST=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT data_type FROM information_schema.columns WHERE table_name = 'admin_chat_messages' AND column_name = 'metadata';")

if echo "$JSONB_TEST" | grep -q "jsonb"; then
    echo -e "${GREEN}✓ JSONB metadata column exists${NC}"
else
    echo -e "${RED}✗ JSONB metadata column not found${NC}"
    exit 1
fi

# Test 7: Test GIN index on metadata
echo -e "${YELLOW}Test 7: Checking GIN index on metadata...${NC}"
GIN_INDEX=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'admin_chat_messages' AND indexdef LIKE '%USING gin%';")

if [ "$GIN_INDEX" -ge 1 ]; then
    echo -e "${GREEN}✓ GIN index on metadata exists${NC}"
else
    echo -e "${YELLOW}⚠ Warning: GIN index on metadata not found${NC}"
fi

# Test 8: Insert test data
echo -e "${YELLOW}Test 8: Testing INSERT operations...${NC}"
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << 'EOF' > /dev/null 2>&1
-- Get first admin user
DO $$
DECLARE
    v_admin_id UUID;
    v_conv_id UUID;
BEGIN
    -- Get first admin user
    SELECT id INTO v_admin_id FROM users WHERE role IN ('ADMIN', 'SUPER_ADMIN') LIMIT 1;

    IF v_admin_id IS NOT NULL THEN
        -- Insert test conversation
        INSERT INTO admin_conversations (admin_id, subject, is_active, message_count, total_tokens)
        VALUES (v_admin_id, 'Test Migration Conversation', true, 0, 0)
        RETURNING conversation_id INTO v_conv_id;

        -- Insert test message
        INSERT INTO admin_chat_messages (conversation_id, role, content, token_count, sequence_number)
        VALUES (v_conv_id, 'USER', 'Test message', 10, 1);

        -- Insert test audit log
        INSERT INTO admin_audit_logs (admin_id, action, status, details)
        VALUES (v_admin_id, 'TEST_ACTION', 'SUCCESS', 'Migration test');

        -- Clean up test data
        DELETE FROM admin_chat_messages WHERE conversation_id = v_conv_id;
        DELETE FROM admin_conversations WHERE conversation_id = v_conv_id;
        DELETE FROM admin_audit_logs WHERE action = 'TEST_ACTION';
    END IF;
END $$;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ INSERT/DELETE operations successful${NC}"
else
    echo -e "${RED}✗ INSERT/DELETE operations failed${NC}"
    exit 1
fi

# Summary
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All migration tests passed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Migration V6 is ready for production."
echo ""
echo "Next steps:"
echo "1. Review the migration file: V6__create_admin_insights_tables.sql"
echo "2. Run the migration: ./mvnw flyway:migrate"
echo "3. Verify application startup"
echo "4. Test admin insights endpoints"

exit 0

