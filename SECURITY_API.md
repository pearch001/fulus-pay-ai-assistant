# Security & Authentication API Documentation

The Fulus Pay AI Assistant uses JWT-based stateless authentication with PIN-based login for mobile users.

## Overview

### Authentication Flow
1. **Register (with BVN)** → Verify BVN → Create account → Receive ₦1,000 bonus → Get tokens
2. **Login** → Authenticate with PIN → Receive access token (24h) + refresh token (7d)
3. **Use access token** → Include in `Authorization: Bearer <token>` header
4. **Token expires** → Use refresh token to get new access token
5. **Logout** → Revoke all refresh tokens

### Security Features
- **JWT Tokens:** Stateless authentication with HS512 signing
- **Access Tokens:** 24-hour expiry, include user claims
- **Refresh Tokens:** 7-day expiry, stored in database, revocable
- **PIN Security:** 4-6 digit PIN encrypted with BCrypt (12 rounds)
- **Account Lockout:** 5 failed attempts = 15-minute lockout
- **Token Revocation:** Logout revokes all refresh tokens
- **BVN Verification:** Mock Nigerian Bank Verification Number validation
- **Name Cross-Check:** Verify provided name matches BVN records
- **Welcome Bonus:** ₦1,000 credited on successful registration

### BVN Verification (Mock Implementation)

The registration process includes Bank Verification Number (BVN) verification to comply with Nigerian financial regulations.

**Mock Verification Logic:**
```
if (BVN last digit is EVEN):
    ✅ APPROVED - Return mock user details
else:
    ❌ REJECTED - Return verification failed
```

**Mock API Behavior:**
- **Delay:** 500ms (simulates real API call)
- **Returns:** Mock name, date of birth, phone number
- **Name Matching:** Uses similarity algorithm (50% word overlap required)

**Test Data:**
```
✅ Approved BVNs (for testing):
  12345678902, 22222222220, 98765432104

❌ Rejected BVNs (for testing):
  12345678901, 11111111111, 98765432103
```

## Base URL

```
http://localhost:8080/api/v1/auth
```

## Endpoints

### 1. Register New User (with BVN Verification)

**Endpoint:** `POST /api/v1/auth/register`

Register a new user account with BVN verification, phone number, and PIN.

**Request Body:**
```json
{
  "phoneNumber": "08012345678",
  "fullName": "Chukwuemeka Okonkwo",
  "email": "chukwuemeka@example.com",
  "pin": "1234",
  "bvn": "12345678902",
  "dateOfBirth": "1990-05-15"
}
```

**Validation:**
- `phoneNumber`: Required, Nigerian format (070/080/081/090/091 + 8 digits), must be unique
- `fullName`: Required, 2-100 characters
- `email`: Required, must be valid email format, must be unique
- `pin`: Required, 4-6 digits (numeric only)
- `bvn`: Required, exactly 11 digits
- `dateOfBirth`: Required, ISO 8601 date format (YYYY-MM-DD)

**Registration Flow:**
1. **Validate Phone Number** - Nigerian format: `0[789][01]xxxxxxxx`
2. **Check Duplicates** - Phone number and email must be unique
3. **BVN Verification** - Mock verification with 500ms delay
4. **Name Cross-Check** - Verify name similarity with BVN records
5. **Create User** - Hash PIN with BCrypt (12 rounds)
6. **Welcome Bonus** - Credit ₦1,000 to new account
7. **Generate Tokens** - Access token (24h) + Refresh token (7d)

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "phoneNumber": "08012345678",
    "name": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com"
  }
}
```

**Success Details:**
- User account created with ₦1,000 welcome bonus
- JWT tokens generated (access + refresh)
- User can immediately start using the app

**Error Responses:**

**400 Bad Request (Duplicate Phone):**
```json
{
  "success": false,
  "error": "Phone number already registered"
}
```

**400 Bad Request (BVN Verification Failed):**
```json
{
  "success": false,
  "error": "BVN verification failed: Please ensure your BVN is correct."
}
```

**400 Bad Request (Name Mismatch):**
```json
{
  "success": false,
  "error": "Name mismatch: The name provided does not match BVN records. Provided: John Doe, BVN: Chukwuemeka Okonkwo"
}
```

**400 Bad Request (Invalid Phone Format):**
```json
{
  "success": false,
  "error": "Invalid Nigerian phone number format. Must be 11 digits starting with 070, 080, 081, 090, or 091."
}
```

**BVN Mock Verification Rules (for testing):**
- **BVN ending in even number** → APPROVED ✅
- **BVN ending in odd number** → REJECTED ❌

**Test Examples:**
```bash
# ✅ Approved BVNs (end in even number)
12345678902  # Last digit: 2 (even)
22222222220  # Last digit: 0 (even)
98765432104  # Last digit: 4 (even)

# ❌ Rejected BVNs (end in odd number)
12345678901  # Last digit: 1 (odd)
11111111111  # Last digit: 1 (odd)
98765432103  # Last digit: 3 (odd)
```

**Example (cURL) - Successful Registration:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "fullName": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com",
    "pin": "1234",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }'
```

**Example (cURL) - Failed Registration (Odd BVN):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "fullName": "Test User",
    "email": "test@example.com",
    "pin": "1234",
    "bvn": "12345678901",
    "dateOfBirth": "1990-05-15"
  }'

# Response:
# {
#   "success": false,
#   "error": "BVN verification failed: BVN verification failed. Please ensure your BVN is correct."
# }
```

**Example (JavaScript):**
```javascript
async function registerUser(userData) {
  try {
    const response = await fetch('http://localhost:8080/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        phoneNumber: userData.phoneNumber,
        fullName: userData.fullName,
        email: userData.email,
        pin: userData.pin,
        bvn: userData.bvn,
        dateOfBirth: userData.dateOfBirth  // Format: YYYY-MM-DD
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error);
    }

    const data = await response.json();

    // Store tokens securely
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    console.log('User registered successfully:', data.user);
    console.log('Welcome bonus: ₦1,000 credited');

    return data;
  } catch (error) {
    console.error('Registration failed:', error.message);
    throw error;
  }
}

// Usage
registerUser({
  phoneNumber: '08012345678',
  fullName: 'Chukwuemeka Okonkwo',
  email: 'chukwuemeka@example.com',
  pin: '1234',
  bvn: '12345678902',  // Even number = approved
  dateOfBirth: '1990-05-15'
});
```

---

### 2. Login with PIN

**Endpoint:** `POST /api/v1/auth/login`

Authenticate user with phone number and PIN.

**Request Body:**
```json
{
  "phoneNumber": "08012345678",
  "pin": "1234"
}
```

**Login Flow:**
1. **Find User** - Look up user by phone number
2. **Account Status Check** - Verify account is not locked or inactive
3. **Rate Limiting Check** - Check failed login attempts
4. **PIN Verification** - Verify PIN using BCrypt comparison
5. **Reset Failed Attempts** - Clear failed login counter on success
6. **Update Login Timestamp** - Set `lastLoginAt` to current time
7. **Generate Tokens** - Create access token (24h) + refresh token (7d)
8. **Return Response** - Include tokens, user details, and balance

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "phoneNumber": "08012345678",
    "name": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com",
    "balance": 1000.00,
    "lastLoginAt": "2024-12-11T14:30:22"
  }
}
```

**Error Responses:**

**401 Unauthorized (Invalid credentials):**
```json
{
  "success": false,
  "error": "Invalid phone number or PIN"
}
```

**423 Locked (Account locked - Rate limit exceeded):**
```json
{
  "success": false,
  "error": "Account is temporarily locked due to multiple failed login attempts. Please try again after 2024-12-11T14:45:00."
}
```

**423 Locked (Account inactive):**
```json
{
  "success": false,
  "error": "Account is inactive. Please contact support."
}
```

**Rate Limiting Details:**
- **Maximum Failed Attempts:** 5
- **Lockout Duration:** 15 minutes (900,000 milliseconds)
- **Counter Reset:** On successful login
- **Security Logging:** All failed attempts logged with user ID and phone number

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "pin": "1234"
  }'
```

**Example (JavaScript):**
```javascript
async function login(phoneNumber, pin) {
  const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ phoneNumber, pin })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }

  const data = await response.json();

  // Store tokens securely
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);

  return data;
}

// Usage
try {
  const auth = await login('08012345678', '1234');
  console.log('Logged in:', auth.user);
} catch (error) {
  console.error('Login failed:', error.message);
}
```

---

### 3. Refresh Access Token

**Endpoint:** `POST /api/v1/auth/refresh`

Get a new access token using a refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "phoneNumber": "08012345678",
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": "Invalid or expired refresh token"
}
```

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

**Example (JavaScript with Auto-Refresh):**
```javascript
class AuthService {
  constructor() {
    this.accessToken = localStorage.getItem('accessToken');
    this.refreshToken = localStorage.getItem('refreshToken');
  }

  async refreshAccessToken() {
    const response = await fetch('http://localhost:8080/api/v1/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        refreshToken: this.refreshToken
      })
    });

    if (!response.ok) {
      // Refresh token expired, redirect to login
      this.logout();
      throw new Error('Session expired. Please login again.');
    }

    const data = await response.json();

    // Update tokens
    this.accessToken = data.accessToken;
    this.refreshToken = data.refreshToken;
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    return data.accessToken;
  }

  async makeAuthenticatedRequest(url, options = {}) {
    // Add authorization header
    options.headers = {
      ...options.headers,
      'Authorization': `Bearer ${this.accessToken}`
    };

    let response = await fetch(url, options);

    // If 401, try to refresh token
    if (response.status === 401) {
      console.log('Token expired, refreshing...');
      await this.refreshAccessToken();

      // Retry request with new token
      options.headers['Authorization'] = `Bearer ${this.accessToken}`;
      response = await fetch(url, options);
    }

    return response;
  }

  logout() {
    this.accessToken = null;
    this.refreshToken = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
  }
}

// Usage
const authService = new AuthService();

// Make authenticated requests
const response = await authService.makeAuthenticatedRequest(
  'http://localhost:8080/api/v1/transactions/user-id'
);
const data = await response.json();
```

---

### 4. Logout

**Endpoint:** `POST /api/v1/auth/logout`

Revoke all refresh tokens for the authenticated user.

**Headers:**
- `Authorization: Bearer <access-token>`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": "Not authenticated"
}
```

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer your-access-token-here"
```

**Example (JavaScript):**
```javascript
async function logout() {
  const accessToken = localStorage.getItem('accessToken');

  await fetch('http://localhost:8080/api/v1/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  // Clear local storage
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');

  // Redirect to login
  window.location.href = '/login';
}
```

---

### 5. Get Current User

**Endpoint:** `GET /api/v1/auth/me`

Get information about the currently authenticated user.

**Headers:**
- `Authorization: Bearer <access-token>`

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "phoneNumber": "08012345678",
  "name": "John Doe",
  "active": true
}
```

**Example (cURL):**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer your-access-token-here"
```

---

### 6. Health Check

**Endpoint:** `GET /api/v1/auth/health`

Check authentication service health.

**Response:** `Authentication Service is running`

---

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "phoneNumber": "08012345678",
  "name": "John Doe",
  "authorities": "ROLE_USER",
  "iss": "fulus-pay-ai-assistant",
  "iat": 1701234567,
  "exp": 1701320967
}
```

### Refresh Token Claims

```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "type": "refresh",
  "iss": "fulus-pay-ai-assistant",
  "iat": 1701234567,
  "exp": 1701839367
}
```

## Using Authentication in Requests

All protected endpoints (under `/api/**` except `/api/v1/auth/**`) require authentication.

### Adding Authorization Header

```bash
curl -X GET http://localhost:8080/api/v1/transactions/user-id \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### JavaScript Axios Example

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1'
});

// Add token to all requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 errors (token expired)
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Refresh token
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post('/auth/refresh', { refreshToken });

      const { accessToken } = response.data;
      localStorage.setItem('accessToken', accessToken);

      // Retry original request
      originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      return api(originalRequest);
    }

    return Promise.reject(error);
  }
);

// Usage
const transactions = await api.get('/transactions/user-id');
```

### Python Requests Example

```python
import requests

class AuthClient:
    def __init__(self, base_url='http://localhost:8080/api/v1'):
        self.base_url = base_url
        self.access_token = None
        self.refresh_token = None

    def login(self, phone_number, pin):
        response = requests.post(f'{self.base_url}/auth/login', json={
            'phoneNumber': phone_number,
            'pin': pin
        })
        data = response.json()

        self.access_token = data['accessToken']
        self.refresh_token = data['refreshToken']

        return data

    def get_headers(self):
        return {'Authorization': f'Bearer {self.access_token}'}

    def refresh(self):
        response = requests.post(f'{self.base_url}/auth/refresh', json={
            'refreshToken': self.refresh_token
        })
        data = response.json()

        self.access_token = data['accessToken']
        self.refresh_token = data['refreshToken']

    def get(self, endpoint):
        response = requests.get(
            f'{self.base_url}{endpoint}',
            headers=self.get_headers()
        )

        if response.status_code == 401:
            # Try to refresh token
            self.refresh()
            response = requests.get(
                f'{self.base_url}{endpoint}',
                headers=self.get_headers()
            )

        return response.json()

# Usage
client = AuthClient()
client.login('08012345678', '1234')

transactions = client.get('/transactions/user-id')
```

## Security Best Practices

### Token Storage

**Web Applications:**
- Store tokens in `httpOnly` cookies (most secure)
- Alternative: `localStorage` with XSS protection
- **Never** store tokens in plain text

**Mobile Applications:**
- Use platform-specific secure storage
- iOS: Keychain
- Android: KeyStore

### Token Handling

1. **Access Token:**
   - Include in `Authorization: Bearer <token>` header
   - Expires after 24 hours
   - Do not store refresh logic in token

2. **Refresh Token:**
   - Store securely
   - Use only to refresh access token
   - Revoked on logout

### PIN Security

- **Minimum:** 4 digits
- **Maximum:** 6 digits
- **Encryption:** BCrypt with 12 rounds
- **Failed Attempts:** 5 attempts = 15-minute lockout

### Production Checklist

- [ ] Change JWT secret key (minimum 256 bits)
- [ ] Enable HTTPS/SSL
- [ ] Set secure, random JWT secret
- [ ] Configure CORS for your domain only
- [ ] Enable rate limiting on auth endpoints
- [ ] Set up token rotation policy
- [ ] Monitor failed login attempts
- [ ] Implement IP-based blocking
- [ ] Enable audit logging
- [ ] Regular security audits

## Error Codes

| Status Code | Meaning | Common Causes |
|-------------|---------|---------------|
| 400 | Bad Request | Invalid input, validation failed |
| 401 | Unauthorized | Invalid/expired token, wrong PIN |
| 423 | Locked | Account locked (5 failed attempts) |
| 500 | Internal Server Error | Server error |

## Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET_KEY=your-super-secret-key-at-least-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=86400000   # 24 hours
JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7 days

# PIN Security
PIN_MAX_ATTEMPTS=5
PIN_LOCKOUT_DURATION=900000 # 15 minutes
```

### application.yml

```yaml
security:
  jwt:
    secret-key: ${JWT_SECRET_KEY}
    access-token-expiration: 86400000   # 24 hours
    refresh-token-expiration: 604800000 # 7 days
    issuer: fulus-pay-ai-assistant
  pin:
    max-attempts: 5
    lockout-duration: 900000  # 15 minutes
```

## Testing Authentication

### Test User Creation

```bash
# Register test user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "name": "Test User",
    "email": "test@example.com",
    "pin": "1234"
  }'

# Login and extract token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"08012345678","pin":"1234"}' \
  | jq -r '.accessToken')

# Use token for authenticated requests
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## PIN Management

The API provides three endpoints for secure PIN management:

### 6. Change PIN

**Endpoint:** `PUT /api/v1/auth/change-pin`

Change user PIN (requires authentication and old PIN verification).

**Headers:**
- `Authorization: Bearer <access-token>`

**Request Body:**
```json
{
  "oldPin": "1234",
  "newPin": "8765"
}
```

**Validation:**
- Old PIN must be correct
- New PIN must be 4-6 digits
- New PIN cannot be same as old PIN
- New PIN must not be sequential (e.g., 1234, 4321)
- New PIN must not be repeating (e.g., 1111, 2222)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PIN changed successfully. Please log in again with your new PIN."
}
```

**Security Features:**
- ✅ All existing tokens invalidated (force re-login)
- ✅ Old PIN verified with BCrypt
- ✅ Comprehensive security audit logging
- ✅ PIN strength validation

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/auth/change-pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-access-token" \
  -d '{
    "oldPin": "1234",
    "newPin": "8765"
  }'
```

---

### 7. Forgot PIN (Initiate Reset)

**Endpoint:** `POST /api/v1/auth/forgot-pin`

Initiate PIN reset process with BVN and DOB verification.

**Request Body:**
```json
{
  "phoneNumber": "08012345678",
  "bvn": "12345678902",
  "dateOfBirth": "1990-05-15"
}
```

**Validation:**
- Phone number must be valid Nigerian format
- BVN must match account records (11 digits)
- Date of birth must match account records

**Response (200 OK):**
```json
{
  "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
  "otp": "123456",
  "expiresAt": "2024-12-11T15:00:00",
  "message": "OTP sent successfully. Valid for 10 minutes."
}
```

**Note:** In production, `otp` should be removed from response and sent via SMS.

**Security Features:**
- ✅ BVN and DOB cross-verification
- ✅ OTP expires in 10 minutes
- ✅ One-time use reset token
- ✅ Previous unused reset tokens invalidated

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-pin \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }'
```

---

### 8. Reset PIN (Complete Reset)

**Endpoint:** `POST /api/v1/auth/reset-pin`

Complete PIN reset using OTP and reset token.

**Request Body:**
```json
{
  "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
  "otp": "123456",
  "newPin": "8765"
}
```

**Validation:**
- Reset token must be valid and not expired
- OTP must match reset token (6 digits)
- New PIN must be 4-6 digits
- New PIN must pass strength validation
- New PIN cannot be same as old PIN

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PIN reset successfully. Please log in with your new PIN."
}
```

**Security Features:**
- ✅ Reset token verified from database
- ✅ OTP verified (exact match)
- ✅ Expiry time checked (10 minutes)
- ✅ One-time use token (marked as used after reset)
- ✅ All existing sessions invalidated
- ✅ Account unlocked (failed attempts reset to 0)

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-pin \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "a7f3c2e1-4b5d-6e7f-8g9h-0i1j2k3l4m5n",
    "otp": "123456",
    "newPin": "8765"
  }'
```

---

### PIN Validation Rules

All PINs must meet the following requirements:

**Format:**
- 4-6 digits
- Numeric only

**Prohibited Patterns:**
- Sequential digits: `1234`, `2345`, `9876`, `8765`
- Repeating digits: `1111`, `2222`, `3333`
- Common patterns: `1212`, `2121`, `1313`

**Weak PIN Examples (Rejected):**
```
❌ 1234 - Sequential ascending
❌ 9876 - Sequential descending
❌ 1111 - All same digits
❌ 1212 - Repeating pattern
```

**Strong PIN Examples (Accepted):**
```
✅ 4859 - Random, non-sequential
✅ 7193 - No repeating patterns
✅ 52847 - 5-digit random
```

For complete PIN management documentation, see [PIN_MANAGEMENT_API.md](./PIN_MANAGEMENT_API.md).

---

## Related Documentation

- [README](./README.md) - Main documentation
- [PIN Management API](./PIN_MANAGEMENT_API.md) - Complete PIN management guide
- [Authentication Summary](./AUTHENTICATION_SUMMARY.md) - Complete auth system overview
- [AI Chat API](./AI_CHAT_API.md) - Text-based chat API
- [Voice API](./VOICE_API.md) - Voice interaction API
- [Transaction API](./TRANSACTION_API.md) - Transaction management

---

**Generated for Fulus Pay AI Assistant - Security & Authentication API v1.0**
