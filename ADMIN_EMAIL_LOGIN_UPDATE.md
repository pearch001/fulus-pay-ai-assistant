# Admin Email-Based Login Update

## ✅ Change Summary

Updated the admin login system to use **email** instead of **phone number** for authentication.

## 🔄 What Changed

### 1. Admin Login Request
**Before:**
```json
{
  "phoneNumber": "08000000000",
  "password": "adminPassword"
}
```

**After:**
```json
{
  "email": "admin@example.com",
  "password": "adminPassword"
}
```

### 2. Files Modified

#### Code Files (3 files):
1. **`AdminLoginRequest.java`**
   - Changed field from `phoneNumber` to `email`
   - Added `@Email` validation annotation
   - Updated validation messages

2. **`AuthenticationService.java`**
   - Updated `adminLogin()` method to find user by email
   - Changed all log messages to use email instead of phone number
   - Authentication still uses phone number internally (Spring Security requirement)

3. **`AuthController.java`**
   - Updated log messages to use email
   - Changed error message from "Invalid phone number or password" to "Invalid email or password"

#### Documentation Files (4 files):
4. **`ADMIN_USER_GUIDE.md`** - Updated all examples to use email
5. **`ADMIN_IMPLEMENTATION_SUMMARY.md`** - Updated all curl commands to use email
6. **`ADMIN_QUICK_REFERENCE.md`** - Updated quick reference examples
7. **`test-admin-endpoints.sh`** - Updated test script to use email

## 🔐 How It Works

1. Admin provides **email** and **password** in login request
2. System looks up user by **email** in the database
3. Verifies the user has ADMIN or SUPER_ADMIN role
4. Authenticates using the user's **phone number** (internal Spring Security requirement)
5. Returns admin auth response with access token

**Note:** Although admins login with email, the system still uses phone number internally for Spring Security's `UserDetails` authentication.

## 📋 Updated Examples

### Admin Login (cURL)
```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@fuluspay.com",
    "password": "YourPassword123!"
  }'
```

### Admin Login (Postman)
**POST** `http://localhost:8080/api/v1/auth/admin/login`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "admin@fuluspay.com",
  "password": "YourPassword123!"
}
```

## 🆚 Comparison: Regular vs Admin Login

| Feature | Regular User Login | Admin Login |
|---------|-------------------|-------------|
| **Endpoint** | `/api/v1/auth/login` | `/api/v1/auth/admin/login` |
| **Credential** | Phone Number | **Email** ✨ |
| **Password** | Required | Required |
| **Device Info** | Required | Not Required |
| **Role Check** | None | Must be ADMIN or SUPER_ADMIN |

## ✅ Testing

### Test Admin Login
```bash
# Test with valid admin email
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@fuluspay.com",
    "password": "AdminPass123!"
  }'

# Expected: Success with access token

# Test with invalid email
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "notadmin@example.com",
    "password": "password"
  }'

# Expected: 401 Unauthorized - "Invalid email or password"

# Test with regular user email
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "UserPass123!"
  }'

# Expected: 401 Unauthorized - "Access denied. Admin privileges required."
```

### Run Test Script
```bash
chmod +x test-admin-endpoints.sh
./test-admin-endpoints.sh
```

## 📝 Important Notes

1. **Email is now required** for all admin users - ensure all admin accounts have valid email addresses
2. **Phone number still exists** in the User entity for internal authentication
3. **Regular users still use phone number** for login - this change only affects admin login
4. **No database migration needed** - email field already exists in the users table

## 🔧 Creating Admin Users

When creating admin users, ensure they have a valid email address:

```sql
INSERT INTO users (
  id, 
  phone_number,  -- Still required for internal auth
  name, 
  email,         -- Now required for admin login
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
  '08000000001',
  'Super Admin',
  'superadmin@fuluspay.com',  -- Email for login
  '$2a$10$your_bcrypt_hash',
  'SUPER_ADMIN',
  true,
  0.00,
  0,
  'ADMIN-' || EXTRACT(EPOCH FROM NOW())::bigint,
  NOW(),
  NOW()
);
```

## ✅ Verification Checklist

- [x] AdminLoginRequest uses email field
- [x] Email validation added
- [x] AuthenticationService finds user by email
- [x] Error messages updated to mention email
- [x] All documentation updated
- [x] Test script updated
- [x] No compilation errors
- [x] Regular user login unchanged

## 🎉 Done!

The admin login system now uses **email-based authentication** instead of phone numbers, providing a better separation between regular users and admin users.

