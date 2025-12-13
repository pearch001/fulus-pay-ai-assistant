# PIN Management API Documentation

Complete documentation for PIN management endpoints in the Fulus Pay AI Assistant.

---

## Overview

The PIN Management API provides secure endpoints for users to manage their account PINs:

1. **Change PIN** - Change PIN with old PIN verification (authenticated)
2. **Forgot PIN** - Initiate PIN reset with BVN/DOB verification
3. **Reset PIN** - Complete PIN reset with OTP verification

All PIN operations include:
- ✅ PIN strength validation (no sequential/repeating digits)
- ✅ Session invalidation (force re-login)
- ✅ Comprehensive security logging
- ✅ Audit trail for compliance

---

## Table of Contents

- [Endpoints](#endpoints)
  - [1. Change PIN](#1-change-pin)
  - [2. Forgot PIN](#2-forgot-pin)
  - [3. Reset PIN](#3-reset-pin)
- [PIN Validation Rules](#pin-validation-rules)
- [Security Measures](#security-measures)
- [Testing Guide](#testing-guide)
- [Error Responses](#error-responses)

---

## Endpoints

### 1. Change PIN

**Endpoint:** `PUT /api/v1/auth/change-pin`

**Description:** Change user PIN (requires authentication and old PIN verification).

**Authentication:** Required (Bearer token)

**Request Body:**
```json
{
  "oldPin": "1234",
  "newPin": "5678"
}
```

**Validation Rules:**
- Old PIN must be correct
- New PIN must be 4-6 digits
- New PIN cannot be same as old PIN
- New PIN must pass strength validation (see below)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "PIN changed successfully. Please log in again with your new PIN."
}
```

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 401 | Invalid old PIN | Old PIN verification failed |
| 400 | PIN is too weak | New PIN doesn't meet security requirements |
| 400 | New PIN must be different from old PIN | Same PIN not allowed |
| 401 | Not authenticated | Missing or invalid access token |

**Security Measures:**
- ✅ Old PIN verified with BCrypt
- ✅ All existing tokens invalidated (force re-login)
- ✅ Security audit log created
- ✅ Failed attempts logged

**Example Request:**
```bash
curl -X PUT http://localhost:8080/api/v1/auth/change-pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "oldPin": "1234",
    "newPin": "8765"
  }'
```

**Flow:**
```
1. Verify user is authenticated (JWT token)
2. Extract user ID from token
3. Verify old PIN using BCrypt
4. Validate new PIN strength
5. Check new PIN != old PIN
6. Hash new PIN with BCrypt
7. Update PIN in database
8. Revoke all refresh tokens (force re-login)
9. Log security event
10. Return success
```

---

### 2. Forgot PIN

**Endpoint:** `POST /api/v1/auth/forgot-pin`

**Description:** Initiate PIN reset process with BVN and DOB verification.

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
  "phoneNumber": "08012345678",
  "bvn": "12345678902",
  "dateOfBirth": "1990-05-15"
}
```

**Validation Rules:**
- Phone number must be valid Nigerian format (11 digits)
- BVN must be exactly 11 digits
- BVN must match account records
- Date of birth must match account records

**Success Response (200 OK):**
```json
{
  "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
  "otp": "123456",
  "expiresAt": "2024-12-11T15:00:00",
  "message": "OTP sent successfully. Valid for 10 minutes."
}
```

**Note:** In production, the `otp` field should be removed and OTP sent via SMS. For testing/PoC, it's included in the response.

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Account not found with provided details | User not found |
| 400 | BVN does not match | BVN verification failed |
| 400 | Date of birth does not match | DOB verification failed |
| 400 | Invalid Nigerian phone number format | Phone validation failed |

**Security Measures:**
- ✅ BVN and DOB cross-verification
- ✅ Previous unused reset tokens invalidated
- ✅ OTP expires in 10 minutes
- ✅ One-time use reset token
- ✅ Security alerts for verification failures

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-pin \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }'
```

**Flow:**
```
1. Validate request format
2. Find user by phone number
3. Verify BVN matches account
4. Verify date of birth matches account
5. Invalidate any existing unused reset tokens for user
6. Generate random 6-digit OTP
7. Generate unique reset token (UUID)
8. Save reset token to database with 10-min expiry
9. Send OTP via SMS (mock for PoC)
10. Return reset token and OTP
```

**OTP Format:**
- 6-digit numeric code
- Randomly generated (100000-999999)
- Expires in 10 minutes
- One-time use only

**Reset Token Format:**
- UUID v4
- Unique per request
- Linked to user ID
- Expires in 10 minutes
- Cannot be reused

---

### 3. Reset PIN

**Endpoint:** `POST /api/v1/auth/reset-pin`

**Description:** Complete PIN reset using OTP and reset token.

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
  "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
  "otp": "123456",
  "newPin": "8765"
}
```

**Validation Rules:**
- Reset token must be valid and not expired
- OTP must match reset token
- OTP must be exactly 6 digits
- New PIN must be 4-6 digits
- New PIN must pass strength validation
- New PIN cannot be same as old PIN

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "PIN reset successfully. Please log in with your new PIN."
}
```

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 401 | Invalid or expired reset token/OTP | Token/OTP verification failed |
| 400 | PIN is too weak | New PIN doesn't meet security requirements |
| 400 | New PIN must be different from old PIN | Same PIN not allowed |
| 400 | OTP must be exactly 6 digits | Invalid OTP format |

**Security Measures:**
- ✅ Reset token verified from database
- ✅ OTP verified (exact match)
- ✅ Expiry time checked (10 minutes)
- ✅ One-time use token (marked as used)
- ✅ All existing sessions invalidated
- ✅ Account unlocked (failed attempts reset)
- ✅ Security audit log created

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
    "otp": "123456",
    "newPin": "8765"
  }'
```

**Flow:**
```
1. Validate request format
2. Find reset token in database by resetToken + OTP
3. Verify token is not expired (< 10 minutes old)
4. Verify token is not already used
5. Validate new PIN strength
6. Get user from token's userId
7. Check new PIN != old PIN
8. Hash new PIN with BCrypt
9. Update PIN in database
10. Reset failed login attempts to 0
11. Clear account lock (if locked)
12. Mark reset token as used
13. Revoke all refresh tokens (force re-login)
14. Log security event
15. Return success
```

---

## PIN Validation Rules

All new PINs must pass the following strength checks:

### Format Requirements
- **Length:** 4-6 digits
- **Type:** Numeric only

### Prohibited Patterns

#### Sequential Digits (Ascending/Descending)
❌ Not allowed:
- `1234`, `2345`, `3456`, `4567`, `5678`, `6789`
- `9876`, `8765`, `7654`, `6543`, `5432`, `4321`
- `12345`, `23456`, `34567`, `45678`, `56789`
- `98765`, `87654`, `76543`, `65432`, `54321`
- `123456`, `234567`, `345678`, `456789`
- `987654`, `876543`, `765432`, `654321`

#### Repeating Digits
❌ Not allowed:
- `0000`, `1111`, `2222`, `3333`, `4444`, `5555`, `6666`, `7777`, `8888`, `9999`
- `00000`, `11111`, `22222`, `33333`, `44444`, `55555`, `66666`, `77777`, `88888`, `99999`
- `000000`, `111111`, `222222`, `333333`, `444444`, `555555`, `666666`, `777777`, `888888`, `999999`

#### Common Patterns
❌ Not allowed:
- `1212`, `2121`, `1313`, `3131`
- `12121`, `21212`, `13131`, `31313`
- `121212`, `212121`, `131313`, `313131`

### Validation Examples

✅ **Strong PINs:**
- `4859` - Random, non-sequential
- `7193` - No repeating patterns
- `52847` - 5-digit random
- `941726` - 6-digit random

❌ **Weak PINs:**
- `1234` - Sequential ascending
- `9876` - Sequential descending
- `1111` - All same digits
- `1212` - Repeating pattern

### Error Messages

When validation fails, users receive specific feedback:

```json
{
  "success": false,
  "error": "PIN is too weak. Avoid sequential or repeating digits."
}
```

```json
{
  "success": false,
  "error": "PIN cannot contain all same digits."
}
```

```json
{
  "success": false,
  "error": "PIN cannot be sequential (e.g., 1234, 4321)."
}
```

---

## Security Measures

### Session Invalidation

All PIN change/reset operations invalidate existing sessions:

```sql
-- All refresh tokens revoked
UPDATE refresh_tokens
SET revoked = true
WHERE user_id = ?
```

**Why:** Prevents unauthorized access with old tokens after PIN change.

### OTP Security

| Feature | Implementation |
|---------|----------------|
| Length | 6 digits (100000-999999) |
| Expiry | 10 minutes |
| Usage | One-time use only |
| Storage | Database-backed |
| Transmission | SMS (production) / Response (PoC) |

### Reset Token Security

| Feature | Implementation |
|---------|----------------|
| Format | UUID v4 |
| Expiry | 10 minutes |
| Usage | One-time use only |
| Validation | Token + OTP required |
| Cleanup | Expired tokens deleted |

### Audit Logging

All PIN operations are logged:

```log
# PIN Change
INFO: PIN change request for user: uuid
WARN: SECURITY ALERT: Failed PIN change attempt for user uuid - Invalid old PIN
INFO: SECURITY: PIN changed successfully for user uuid. All sessions invalidated.

# Forgot PIN
INFO: Forgot PIN request for phone number: 08012345678
WARN: SECURITY ALERT: Forgot PIN BVN mismatch for user uuid (Phone: 08012345678)
INFO: PIN reset OTP generated for user uuid (Phone: 08012345678). Expires at: 2024-12-11T15:00:00
INFO: MOCK SMS: OTP for 08012345678: 123456

# Reset PIN
INFO: PIN reset request with token: a7f3c2e1...
WARN: SECURITY ALERT: Invalid or expired PIN reset token/OTP
INFO: SECURITY: PIN reset successfully for user uuid (Phone: 08012345678). All sessions invalidated.
```

### Account Recovery Benefits

Reset PIN also:
- ✅ Resets failed login attempts to 0
- ✅ Unlocks account if locked
- ✅ Allows immediate login with new PIN

---

## Testing Guide

### Test Scenario 1: Change PIN Successfully

```bash
# 1. Login to get access token
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "pin": "1234"
  }')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')

# 2. Change PIN
curl -X PUT http://localhost:8080/api/v1/auth/change-pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "oldPin": "1234",
    "newPin": "8765"
  }'

# Expected: 200 OK with success message
# All existing tokens invalidated

# 3. Try to use old token (should fail)
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Expected: 401 Unauthorized

# 4. Login with new PIN
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "pin": "8765"
  }'

# Expected: 200 OK with new tokens
```

### Test Scenario 2: Forgot PIN Flow

```bash
# 1. Initiate forgot PIN
FORGOT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/forgot-pin \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }')

echo $FORGOT_RESPONSE | jq

# Expected: 200 OK with resetToken and OTP
# {
#   "resetToken": "uuid",
#   "otp": "123456",
#   "expiresAt": "2024-12-11T15:00:00",
#   "message": "OTP sent successfully. Valid for 10 minutes."
# }

RESET_TOKEN=$(echo $FORGOT_RESPONSE | jq -r '.resetToken')
OTP=$(echo $FORGOT_RESPONSE | jq -r '.otp')

# 2. Reset PIN with OTP
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d "{
    \"resetToken\": \"$RESET_TOKEN\",
    \"otp\": \"$OTP\",
    \"newPin\": \"9517\"
  }"

# Expected: 200 OK with success message

# 3. Login with new PIN
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "pin": "9517"
  }'

# Expected: 200 OK with tokens
```

### Test Scenario 3: Weak PIN Rejection

```bash
# Attempt to set weak PIN
curl -X PUT http://localhost:8080/api/v1/auth/change-pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "oldPin": "8765",
    "newPin": "1234"
  }'

# Expected: 400 Bad Request
# {
#   "success": false,
#   "error": "PIN is too weak. Avoid sequential or repeating digits."
# }
```

### Test Scenario 4: Invalid BVN/DOB

```bash
# Attempt forgot PIN with wrong BVN
curl -X POST http://localhost:8080/api/v1/auth/forgot-pin \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "bvn": "99999999999",
    "dateOfBirth": "1990-05-15"
  }'

# Expected: 400 Bad Request
# {
#   "success": false,
#   "error": "Account verification failed. BVN does not match."
# }
```

### Test Scenario 5: Expired OTP

```bash
# Wait 11 minutes after forgot-pin request, then try to reset
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "expired-token",
    "otp": "123456",
    "newPin": "8765"
  }'

# Expected: 401 Unauthorized
# {
#   "success": false,
#   "error": "Invalid or expired reset token/OTP"
# }
```

### Test Scenario 6: OTP Reuse Prevention

```bash
# 1. Successfully reset PIN
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "valid-token",
    "otp": "123456",
    "newPin": "8765"
  }'

# Expected: 200 OK

# 2. Try to reuse same OTP/token
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "valid-token",
    "otp": "123456",
    "newPin": "9517"
  }'

# Expected: 401 Unauthorized (token already used)
```

---

## Error Responses

### Common Errors

#### 400 Bad Request
```json
{
  "success": false,
  "error": "PIN is too weak. Avoid sequential or repeating digits."
}
```

#### 401 Unauthorized
```json
{
  "success": false,
  "error": "Invalid old PIN"
}
```

```json
{
  "success": false,
  "error": "Invalid or expired reset token/OTP"
}
```

#### 423 Locked (if account locked during change attempt)
```json
{
  "success": false,
  "error": "Account is temporarily locked due to multiple failed login attempts. Please try again after 2024-12-11T15:00:00."
}
```

#### 500 Internal Server Error
```json
{
  "success": false,
  "error": "PIN change failed. Please try again."
}
```

---

## Database Schema

### pin_reset_tokens Table

```sql
CREATE TABLE pin_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    reset_token VARCHAR(100) UNIQUE NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reset_token ON pin_reset_tokens(reset_token);
CREATE INDEX idx_user_id ON pin_reset_tokens(user_id);
CREATE INDEX idx_expiry_date ON pin_reset_tokens(expiry_date);
```

### Cleanup Query (Scheduled)

```sql
-- Delete expired tokens (run daily)
DELETE FROM pin_reset_tokens
WHERE expiry_date < NOW();
```

---

## Production Deployment

### SMS Integration

Replace mock SMS with real SMS provider:

```java
// In AuthenticationService.forgotPin()
// TODO: Replace this with real SMS service
smsService.sendOTP(request.getPhoneNumber(), otp);

// Remove OTP from response in production
return ForgotPinResponse.builder()
        .resetToken(resetToken)
        // .otp(otp)  // REMOVE IN PRODUCTION
        .expiresAt(expiryDate)
        .message("OTP sent to your registered phone number. Valid for 10 minutes.")
        .build();
```

### Recommended SMS Providers (Nigeria)

- **Termii** - Nigerian SMS provider
- **Africa's Talking** - Pan-African provider
- **Twilio** - Global provider

### Rate Limiting

Implement rate limiting for forgot-pin endpoint:

```yaml
# application.yml
security:
  pin-reset:
    max-attempts-per-hour: 3
    max-attempts-per-day: 5
```

### Monitoring

Set up alerts for:
- High number of forgot-pin requests from same IP
- Multiple failed OTP verifications
- Unusual PIN change patterns
- BVN/DOB mismatch attempts

---

## Related Documentation

- [AUTHENTICATION_SUMMARY.md](./AUTHENTICATION_SUMMARY.md) - Complete auth system overview
- [SECURITY_API.md](./SECURITY_API.md) - Authentication API documentation
- [README.md](./README.md) - Main project documentation

---

**Last Updated:** December 11, 2024
**Version:** 1.0
**Status:** ✅ PRODUCTION READY (with SMS integration for prod)
