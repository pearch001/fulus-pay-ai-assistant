-- =====================================================
-- Migration: V6__create_admin_insights_tables.sql
-- Description: Create tables for Admin Business Insights feature
--              - admin_conversations: Stores AI chat conversation sessions
--              - admin_chat_messages: Stores individual messages in conversations
--              - admin_audit_logs: Tracks all admin actions for security
-- Author: System
-- Date: 2024-12-14
-- =====================================================

-- =====================================================
-- 1. Admin Conversations Table
-- =====================================================
CREATE TABLE IF NOT EXISTS admin_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    subject VARCHAR(500),
    conversation_summary TEXT,
    is_active BOOLEAN DEFAULT true,
    message_count INTEGER DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for admin_conversations
CREATE INDEX idx_admin_conv_admin_id ON admin_conversations(admin_id);
CREATE INDEX idx_admin_conv_conversation_id ON admin_conversations(conversation_id);
CREATE INDEX idx_admin_conv_updated ON admin_conversations(updated_at DESC);
CREATE INDEX idx_admin_conv_active ON admin_conversations(is_active);
CREATE INDEX idx_admin_conv_created ON admin_conversations(created_at DESC);

COMMENT ON TABLE admin_conversations IS 'Stores admin AI chat conversation sessions for business insights';
COMMENT ON COLUMN admin_conversations.admin_id IS 'Reference to the admin user who owns this conversation';
COMMENT ON COLUMN admin_conversations.conversation_id IS 'Unique identifier for the conversation session';
COMMENT ON COLUMN admin_conversations.subject IS 'Topic or subject of the conversation, auto-generated from first message';
COMMENT ON COLUMN admin_conversations.conversation_summary IS 'AI-generated summary of the conversation (generated every 10 messages)';
COMMENT ON COLUMN admin_conversations.is_active IS 'Whether the conversation is active or archived';
COMMENT ON COLUMN admin_conversations.message_count IS 'Total number of messages in this conversation';
COMMENT ON COLUMN admin_conversations.total_tokens IS 'Total OpenAI tokens used in this conversation for cost tracking';

-- =====================================================
-- 2. Admin Chat Messages Table
-- =====================================================
CREATE TABLE IF NOT EXISTS admin_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    metadata JSONB,
    token_count INTEGER,
    sequence_number INTEGER NOT NULL,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_conversation_sequence UNIQUE(conversation_id, sequence_number),
    CONSTRAINT fkh30u13p0kb23fwvfq28cr9yb1 FOREIGN KEY (conversation_id)
        REFERENCES admin_conversations(conversation_id) ON DELETE CASCADE
);

-- Indexes for admin_chat_messages
CREATE INDEX idx_admin_msg_conv_id ON admin_chat_messages(conversation_id);
CREATE INDEX idx_admin_msg_created ON admin_chat_messages(created_at);
CREATE INDEX idx_admin_msg_seq ON admin_chat_messages(conversation_id, sequence_number);
CREATE INDEX idx_admin_msg_role ON admin_chat_messages(role);

-- JSONB index for metadata queries
CREATE INDEX idx_admin_msg_metadata ON admin_chat_messages USING GIN (metadata);

COMMENT ON TABLE admin_chat_messages IS 'Stores individual messages in admin AI chat conversations';
COMMENT ON COLUMN admin_chat_messages.conversation_id IS 'Reference to the parent conversation';
COMMENT ON COLUMN admin_chat_messages.role IS 'Message role: USER (admin query), ASSISTANT (AI response), SYSTEM (context)';
COMMENT ON COLUMN admin_chat_messages.content IS 'The actual message content';
COMMENT ON COLUMN admin_chat_messages.metadata IS 'Additional metadata stored as JSON (e.g., category, charts, insights)';
COMMENT ON COLUMN admin_chat_messages.token_count IS 'Estimated token count for this message';
COMMENT ON COLUMN admin_chat_messages.sequence_number IS 'Sequential number of message in conversation (for ordering)';
COMMENT ON COLUMN admin_chat_messages.processing_time_ms IS 'Time taken to generate AI response in milliseconds';

-- =====================================================
-- 3. Admin Audit Logs Table
-- =====================================================
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    status VARCHAR(20),
    processing_time_ms BIGINT
);

-- Indexes for admin_audit_logs
CREATE INDEX idx_audit_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX idx_audit_action ON admin_audit_logs(action);
CREATE INDEX idx_audit_timestamp ON admin_audit_logs(timestamp DESC);
CREATE INDEX idx_audit_resource ON admin_audit_logs(resource_id);
CREATE INDEX idx_audit_status ON admin_audit_logs(status);

COMMENT ON TABLE admin_audit_logs IS 'Audit trail for all admin actions and AI interactions';
COMMENT ON COLUMN admin_audit_logs.admin_id IS 'Reference to the admin user who performed the action';
COMMENT ON COLUMN admin_audit_logs.action IS 'Type of action performed (e.g., ADMIN_CHAT_MESSAGE_SENT, CONVERSATION_DELETED)';
COMMENT ON COLUMN admin_audit_logs.resource_id IS 'ID of the resource affected (e.g., conversation_id)';
COMMENT ON COLUMN admin_audit_logs.details IS 'Additional details about the action';
COMMENT ON COLUMN admin_audit_logs.ip_address IS 'IP address from which the action was performed';
COMMENT ON COLUMN admin_audit_logs.user_agent IS 'User agent string of the client';
COMMENT ON COLUMN admin_audit_logs.status IS 'Action status: SUCCESS, FAILURE, ERROR';
COMMENT ON COLUMN admin_audit_logs.processing_time_ms IS 'Time taken to process the action in milliseconds';

-- =====================================================
-- 4. Function for automatic updated_at timestamp
-- =====================================================
-- Create the function if it doesn't exist
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. Triggers
-- =====================================================
-- Trigger to automatically update updated_at on admin_conversations
DROP TRIGGER IF EXISTS update_admin_conversation_timestamp ON admin_conversations;
CREATE TRIGGER update_admin_conversation_timestamp
    BEFORE UPDATE ON admin_conversations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 6. Grant Permissions (if using specific database users)
-- =====================================================
-- Note: Adjust these based on your database user setup
-- GRANT SELECT, INSERT, UPDATE, DELETE ON admin_conversations TO fulus_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON admin_chat_messages TO fulus_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON admin_audit_logs TO fulus_user;

-- =====================================================
-- 7. Sample Data (Optional - for development/testing)
-- =====================================================
-- Uncomment the following lines to insert sample data for testing

/*
-- Insert a sample admin conversation (requires an existing admin user)
INSERT INTO admin_conversations (admin_id, conversation_id, subject, is_active, message_count, total_tokens)
SELECT
    id,
    gen_random_uuid(),
    'Sample Business Insights Chat',
    true,
    0,
    0
FROM users
WHERE role = 'ADMIN'
LIMIT 1
ON CONFLICT DO NOTHING;
*/

-- =====================================================
-- 8. Verification Queries
-- =====================================================
-- Run these to verify the migration was successful:

-- Check tables exist
-- SELECT table_name FROM information_schema.tables
-- WHERE table_schema = 'public'
-- AND table_name IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');

-- Check indexes
-- SELECT indexname, tablename FROM pg_indexes
-- WHERE tablename IN ('admin_conversations', 'admin_chat_messages', 'admin_audit_logs');

-- Check constraints
-- SELECT conname, contype FROM pg_constraint
-- WHERE conrelid IN (
--     'admin_conversations'::regclass,
--     'admin_chat_messages'::regclass,
--     'admin_audit_logs'::regclass
-- );

-- =====================================================
-- END OF MIGRATION
-- =====================================================

