-- Migration script to add role column to users table
-- Adds role field with default value USER for existing users

-- Add role column to users table
ALTER TABLE users
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Create index on role column for better query performance
CREATE INDEX idx_user_role ON users(role);

-- Update any existing users to have USER role (in case they were null)
UPDATE users
SET role = 'USER'
WHERE role IS NULL OR role = '';

-- Optional: Create a super admin user (update with your actual details)
-- INSERT INTO users (id, phone_number, name, email, password, role, active, balance, failed_login_attempts, account_number, created_at, updated_at)
-- VALUES (
--     gen_random_uuid(),
--     '08000000000',  -- Update with actual phone number
--     'Super Admin',
--     'admin@fuluspay.com',  -- Update with actual email
--     '$2a$10$your_bcrypt_hashed_password_here',  -- Update with bcrypt hashed password
--     'SUPER_ADMIN',
--     true,
--     0.00,
--     0,
--     'ADMIN-' || EXTRACT(EPOCH FROM NOW())::bigint,
--     NOW(),
--     NOW()
-- );

