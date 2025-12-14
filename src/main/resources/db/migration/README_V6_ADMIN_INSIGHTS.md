# Database Migration: Admin Insights Tables

## Overview
This migration creates the database schema for the **Admin Business Insights** feature, which provides AI-powered business intelligence for SyncPay executives and investors.

## Migration Details
- **Version**: V6
- **File**: `V6__create_admin_insights_tables.sql`
- **Rollback**: `U6__rollback_admin_insights_tables.sql`
- **Date**: 2024-12-14

## Tables Created

### 1. admin_conversations
Stores AI chat conversation sessions for admin users.

**Columns:**
- `id` - Primary key (UUID)
- `admin_id` - Foreign key to users table
- `conversation_id` - Unique identifier for the conversation
- `subject` - Topic/subject of conversation (auto-generated)
- `conversation_summary` - AI-generated summary (created every 10 messages)
- `is_active` - Whether conversation is active or archived
- `message_count` - Total messages in conversation
- `total_tokens` - Total OpenAI tokens used (for cost tracking)
- `created_at` - Timestamp of creation
- `updated_at` - Timestamp of last update (auto-updated via trigger)

**Indexes:**
- `idx_admin_conv_admin_id` - Query conversations by admin
- `idx_admin_conv_conversation_id` - Unique conversation lookup
- `idx_admin_conv_updated` - Sort by last updated
- `idx_admin_conv_active` - Filter active conversations
- `idx_admin_conv_created` - Sort by creation date

### 2. admin_chat_messages
Stores individual messages within conversations.

**Columns:**
- `id` - Primary key (UUID)
- `conversation_id` - Foreign key to admin_conversations
- `role` - Message role (USER, ASSISTANT, SYSTEM)
- `content` - Message content (TEXT)
- `metadata` - Additional data as JSONB (category, charts, etc.)
- `token_count` - Estimated token count for message
- `sequence_number` - Message order in conversation
- `processing_time_ms` - AI response generation time
- `created_at` - Timestamp of message

**Indexes:**
- `idx_admin_msg_conv_id` - Query messages by conversation
- `idx_admin_msg_created` - Sort messages by time
- `idx_admin_msg_seq` - Order messages in conversation
- `idx_admin_msg_role` - Filter by role
- `idx_admin_msg_metadata` - GIN index for JSONB queries

**Constraints:**
- `unique_conversation_sequence` - Ensures unique sequence per conversation
- `role` CHECK constraint - Only allows USER, ASSISTANT, or SYSTEM

### 3. admin_audit_logs
Audit trail for all admin actions and AI interactions.

**Columns:**
- `id` - Primary key (UUID)
- `admin_id` - Admin user who performed action
- `action` - Action type (e.g., ADMIN_CHAT_MESSAGE_SENT)
- `resource_id` - Resource affected (e.g., conversation_id)
- `details` - Additional action details
- `timestamp` - When action occurred
- `ip_address` - Source IP address
- `user_agent` - Client user agent
- `status` - Action status (SUCCESS, FAILURE, ERROR)
- `processing_time_ms` - Action processing time

**Indexes:**
- `idx_audit_admin_id` - Query logs by admin
- `idx_audit_action` - Filter by action type
- `idx_audit_timestamp` - Sort by time
- `idx_audit_resource` - Find logs for specific resource
- `idx_audit_status` - Filter by status

## Running the Migration

### Using Flyway
```bash
./mvnw flyway:migrate
```

### Using Liquibase
```bash
./mvnw liquibase:update
```

### Manual Execution
```bash
psql -U fulus_user -d fulus_ai_db -f src/main/resources/db/migration/V6__create_admin_insights_tables.sql
```

## Rollback

### Using Flyway
```bash
./mvnw flyway:undo
```

### Manual Rollback
```bash
psql -U fulus_user -d fulus_ai_db -f src/main/resources/db/migration/U6__rollback_admin_insights_tables.sql
```

⚠️ **WARNING**: Rollback will permanently delete all admin conversation data!

## Verification

After running the migration, verify with:

```sql
-- Check tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');

-- Should return 3 rows

-- Check indexes
SELECT indexname, tablename FROM pg_indexes 
WHERE tablename IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs')
ORDER BY tablename, indexname;

-- Check constraints
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    conrelid::regclass AS table_name
FROM pg_constraint 
WHERE conrelid IN (
    'admin_conversations'::regclass, 
    'admin_chat_messages'::regclass,
    'admin_audit_logs'::regclass
)
ORDER BY table_name, constraint_name;

-- Check trigger
SELECT 
    trigger_name, 
    event_manipulation, 
    event_object_table
FROM information_schema.triggers
WHERE event_object_table = 'admin_conversations';
```

## Sample Queries

### Get all conversations for an admin
```sql
SELECT * FROM admin_conversations 
WHERE admin_id = 'your-admin-uuid'
ORDER BY updated_at DESC;
```

### Get conversation history
```sql
SELECT 
    role, 
    content, 
    token_count, 
    created_at
FROM admin_chat_messages 
WHERE conversation_id = 'your-conversation-uuid'
ORDER BY sequence_number ASC;
```

### Get audit logs for admin actions
```sql
SELECT 
    action, 
    details, 
    timestamp, 
    status
FROM admin_audit_logs 
WHERE admin_id = 'your-admin-uuid'
ORDER BY timestamp DESC
LIMIT 50;
```

### Query by message metadata
```sql
SELECT * FROM admin_chat_messages
WHERE metadata->>'category' = 'REVENUE_ANALYSIS';
```

## Performance Considerations

1. **Indexes**: All critical query paths are indexed
2. **JSONB**: Uses GIN index for efficient metadata queries
3. **Partitioning**: Consider partitioning admin_audit_logs by timestamp for large datasets
4. **Archival**: Implement archival strategy for old conversations
5. **Cascade Deletes**: Messages are automatically deleted when conversation is deleted

## Security

- Foreign key constraints ensure data integrity
- Audit logs track all admin actions for compliance
- IP address and user agent logged for security analysis
- Consider implementing row-level security (RLS) if multiple tenants

## Maintenance

### Clean up old audit logs (older than 90 days)
```sql
DELETE FROM admin_audit_logs 
WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '90 days';
```

### Archive inactive conversations
```sql
UPDATE admin_conversations 
SET is_active = false 
WHERE updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
```

### Vacuum tables periodically
```sql
VACUUM ANALYZE admin_conversations;
VACUUM ANALYZE admin_chat_messages;
VACUUM ANALYZE admin_audit_logs;
```

## Related Files

- Entity: `AdminConversation.java`
- Entity: `AdminChatMessage.java`
- Entity: `AdminAuditLog.java`
- Repository: `AdminConversationRepository.java`
- Repository: `AdminChatMessageRepository.java`
- Repository: `AdminAuditLogRepository.java`
- Service: `AdminInsightsService.java`
- Controller: `AdminInsightsController.java`

## Support

For issues or questions about this migration:
1. Check application logs
2. Verify database connection
3. Ensure Flyway/Liquibase is properly configured
4. Review entity mappings match table schema

