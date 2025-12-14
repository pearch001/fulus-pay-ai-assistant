# Admin User Management Guide

This guide explains how to use the admin user functionality in the Fulus Pay system.

## Overview

The system now supports three user roles:
- **USER**: Regular users with standard privileges
- **ADMIN**: Admin users with elevated privileges
- **SUPER_ADMIN**: Super administrators with full system access

## Features Added

### 1. User Role System
- `UserRole` enum with three roles: USER, ADMIN, SUPER_ADMIN
- Role-based authentication and authorization
- Separate login endpoints for regular users and admins

### 2. Admin Login Endpoint

**Endpoint**: `POST /api/v1/auth/admin/login`

**Request Body**:
```json
{
  "email": "admin@example.com",
  "password": "your-admin-password"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "admin": {
    "id": "uuid",
    "phoneNumber": "08000000000",
    "name": "Admin Name",
    "email": "admin@example.com",
    "role": "ADMIN",
    "lastLoginAt": "2025-12-14T10:30:00"
  }
}
```

**Security Features**:
- Verifies user has ADMIN or SUPER_ADMIN role
- Uses email instead of phone number for admin login
- Prevents regular users from accessing admin login
- Same security measures as regular login (account locking, failed attempts tracking)

### 3. Create Admin User Endpoint

**Endpoint**: `POST /api/v1/auth/admin/create`

**Authorization**: Requires **SUPER_ADMIN** role

**Headers**:
```
Authorization: Bearer <super-admin-access-token>
```

**Request Body**:
```json
{
  "phoneNumber": "08012345678",
  "fullName": "New Admin User",
  "email": "newadmin@example.com",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!",
  "role": "ADMIN"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "admin": {
    "id": "uuid",
    "phoneNumber": "08012345678",
    "name": "New Admin User",
    "email": "newadmin@example.com",
    "role": "ADMIN",
    "lastLoginAt": null
  }
}
```

**Notes**:
- Only SUPER_ADMIN can create new admin users
- Can create ADMIN or SUPER_ADMIN users
- Cannot create regular USER through this endpoint
- Admin users get account number prefixed with "ADMIN-"
- Admin users don't require BVN verification

## Database Migration

Run the migration script to add the role column to existing users:

```bash
psql -U your_username -d your_database -f migration-add-user-role.sql
```

This will:
1. Add `role` column to `users` table
2. Set default value to 'USER' for all existing users
3. Create an index on the role column for performance

## Creating the First Super Admin

### Option 1: Direct Database Insert

```sql
INSERT INTO users (
  id, 
  phone_number, 
  name, 
  email, 
  password, 
  role, 
  active, 
  balance, 
  failed_login_attempts, 
  account_number, 
  created_at, 
  updated_at
)
VALUES (
  gen_random_uuid(),
  '08000000000',
  'Super Admin',
  'superadmin@fuluspay.com',
  '$2a$10$your_bcrypt_hashed_password',  -- Hash your password first
  'SUPER_ADMIN',
  true,
  0.00,
  0,
  'ADMIN-' || EXTRACT(EPOCH FROM NOW())::bigint,
  NOW(),
  NOW()
);
```

### Option 2: Generate Password Hash

Use this Java code or online bcrypt tool to hash your password:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "YourSuperAdminPassword123!";
        String hashedPassword = encoder.encode(password);
        System.out.println("Hashed password: " + hashedPassword);
    }
}
```

## API Usage Examples

### 1. Regular User Login (Existing)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "password": "userPassword123",
    "deviceInfo": {
      "deviceId": "device-123",
      "deviceName": "iPhone 13",
      "deviceModel": "iPhone 13 Pro",
      "deviceOS": "iOS 16.0"
    }
  }'
```

### 2. Admin Login (New)
```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "adminPassword123"
  }'
```

### 3. Create Admin User (New)
```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <super-admin-token>" \
  -d '{
    "phoneNumber": "08011111111",
    "fullName": "Regional Admin",
    "email": "regional@fuluspay.com",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "role": "ADMIN"
  }'
```

## Security Considerations

1. **Role Verification**: Admin endpoints verify the user's role before granting access
2. **Separate Authentication**: Admin login is separate from regular user login
3. **Authorization Checks**: SUPER_ADMIN role required to create new admins
4. **Audit Logging**: All admin actions are logged for security auditing
5. **No Device Binding**: Admin users don't have device restrictions like regular users
6. **No BVN Required**: Admin users bypass BVN verification (not applicable for admin accounts)

## Role-Based Access Control

The system uses Spring Security's role-based access control:

- `ROLE_USER`: Regular user privileges
- `ROLE_ADMIN`: Admin privileges (to be implemented in future endpoints)
- `ROLE_SUPER_ADMIN`: Full system access

You can protect endpoints using:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/dashboard")
public ResponseEntity<?> adminDashboard() {
    // Admin-only endpoint
}

@PreAuthorize("hasRole('SUPER_ADMIN')")
@DeleteMapping("/admin/{id}")
public ResponseEntity<?> deleteAdmin(@PathVariable UUID id) {
    // Super admin-only endpoint
}
```

## Next Steps

1. Run the database migration
2. Create your first SUPER_ADMIN user
3. Use SUPER_ADMIN to create additional ADMIN users
4. Implement role-based access control on other endpoints
5. Add admin-specific features (user management, reports, etc.)

## Files Modified/Created

### Created Files:
- `UserRole.java` - Enum for user roles
- `AdminLoginRequest.java` - DTO for admin login
- `CreateAdminRequest.java` - DTO for creating admin users
- `AdminAuthResponse.java` - DTO for admin authentication response
- `migration-add-user-role.sql` - Database migration script
- `ADMIN_USER_GUIDE.md` - This guide

### Modified Files:
- `User.java` - Added role field
- `UserPrincipal.java` - Added role support and updated authorities
- `AuthenticationService.java` - Added adminLogin() and createAdmin() methods
- `AuthController.java` - Added admin endpoints

## Testing

See the testing section below for curl commands to test the functionality.

