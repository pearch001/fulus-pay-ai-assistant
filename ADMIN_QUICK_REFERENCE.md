# Quick Reference - Admin User System

## 🚀 Quick Start

### 1. Run Migration
```bash
psql -U postgres -d fulus_pay -f migration-add-user-role.sql
```

### 2. Create Super Admin (Manual)
```sql
-- First, generate password hash using BCrypt
-- Then insert:
INSERT INTO users (id, phone_number, name, email, password, role, active, balance, failed_login_attempts, account_number, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  '08000000001',
  'Super Admin',
  'superadmin@fuluspay.com',
  '$2a$10$...',  -- Replace with BCrypt hash
  'SUPER_ADMIN',
  true,
  0.00,
  0,
  'ADMIN-' || EXTRACT(EPOCH FROM NOW())::bigint,
  NOW(),
  NOW()
);
```

### 3. Admin Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email":"superadmin@fuluspay.com","password":"YourPassword"}'
```

### 4. Create New Admin
```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "phoneNumber":"08011111111",
    "fullName":"New Admin",
    "email":"admin@example.com",
    "password":"Pass123!",
    "confirmPassword":"Pass123!",
    "role":"ADMIN"
  }'
```

## 📋 Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/auth/admin/login` | POST | None | Admin login |
| `/api/v1/auth/admin/create` | POST | SUPER_ADMIN | Create admin user |

## 🔐 Roles

| Role | Authority | Can Create Admins |
|------|-----------|-------------------|
| USER | ROLE_USER | ❌ No |
| ADMIN | ROLE_ADMIN | ❌ No |
| SUPER_ADMIN | ROLE_SUPER_ADMIN | ✅ Yes |

## 📁 Files Changed

**Created:**
- `UserRole.java` - Role enum
- `AdminLoginRequest.java` - Login DTO
- `CreateAdminRequest.java` - Create DTO
- `AdminAuthResponse.java` - Response DTO
- `migration-add-user-role.sql` - DB migration

**Modified:**
- `User.java` - Added role field
- `UserPrincipal.java` - Added role support
- `AuthenticationService.java` - Added admin methods
- `AuthController.java` - Added admin endpoints

## ✅ Done!

