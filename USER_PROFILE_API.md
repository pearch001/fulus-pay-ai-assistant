# User Profile API Documentation

Complete documentation for User Profile management endpoints in the Fulus Pay AI Assistant.

---

## Overview

The User Profile API provides endpoints for users to manage their account information, view wallet details, and complete enhanced KYC verification.

### Features
- ✅ View complete user profile
- ✅ Update profile information (name, email)
- ✅ View wallet details with pending transaction count
- ✅ Enhanced KYC verification with document upload
- ✅ Authorization checks (users can only access their own profile)
- ✅ Virtual account number generation

---

## Base URL

```
http://localhost:8080/api/v1/users/profile
```

---

## Authentication

All endpoints require authentication with a valid JWT access token.

**Header:**
```
Authorization: Bearer <access-token>
```

**Authorization:** Users can only access their own profile. User ID is extracted from the JWT token.

---

## Endpoints

### 1. Get User Profile

**Endpoint:** `GET /api/v1/users/profile`

**Description:** Retrieve current user's profile information.

**Authentication:** Required

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "phoneNumber": "08012345678",
  "name": "Chukwuemeka Okonkwo",
  "email": "chukwuemeka@example.com",
  "balance": 5000.00,
  "accountNumber": "3012345678",
  "kycStatus": "VERIFIED",
  "dateOfBirth": "1990-05-15",
  "createdAt": "2024-12-01T10:30:00",
  "lastLoginAt": "2024-12-11T14:30:22",
  "active": true
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | User unique identifier |
| phoneNumber | String | Nigerian phone number (cannot be changed) |
| name | String | User's full name |
| email | String | User's email address |
| balance | BigDecimal | Current wallet balance (₦) |
| accountNumber | String | Virtual account number (10 digits) |
| kycStatus | Enum | KYC verification status |
| dateOfBirth | Date | User's date of birth |
| createdAt | DateTime | Account creation timestamp |
| lastLoginAt | DateTime | Last successful login timestamp |
| active | Boolean | Account active status |

**KYC Status Values:**

| Status | Description |
|--------|-------------|
| PENDING | Registration complete, verification pending |
| VERIFIED | Basic verification complete (BVN verified) |
| ENHANCED | Enhanced verification with ID document |
| REJECTED | Verification failed |
| SUSPENDED | Account suspended for security reasons |

**Example (cURL):**
```bash
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Example (JavaScript):**
```javascript
async function getUserProfile() {
  const token = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8080/api/v1/users/profile', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  const profile = await response.json();
  console.log('User Profile:', profile);
  return profile;
}
```

---

### 2. Update User Profile

**Endpoint:** `PUT /api/v1/users/profile`

**Description:** Update user profile information (name and email only). Phone number cannot be changed.

**Authentication:** Required

**Request Body:**
```json
{
  "name": "Chukwuemeka John Okonkwo",
  "email": "chukwuemeka.new@example.com"
}
```

**Validation:**
- `name`: Optional, 2-100 characters
- `email`: Optional, must be valid email format, must be unique

**Note:** Both fields are optional. Send only the fields you want to update.

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "phoneNumber": "08012345678",
  "name": "Chukwuemeka John Okonkwo",
  "email": "chukwuemeka.new@example.com",
  "balance": 5000.00,
  "accountNumber": "3012345678",
  "kycStatus": "VERIFIED",
  "dateOfBirth": "1990-05-15",
  "createdAt": "2024-12-01T10:30:00",
  "lastLoginAt": "2024-12-11T14:30:22",
  "active": true
}
```

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Email already registered | Email is used by another account |
| 400 | Name must be between 2 and 100 characters | Invalid name length |
| 400 | Email must be valid | Invalid email format |
| 401 | Not authenticated | Missing or invalid access token |

**Example (cURL):**
```bash
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -d '{
    "name": "Chukwuemeka John Okonkwo",
    "email": "chukwuemeka.new@example.com"
  }'
```

**Example (JavaScript):**
```javascript
async function updateProfile(updates) {
  const token = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8080/api/v1/users/profile', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(updates)
  });

  if (response.ok) {
    const updatedProfile = await response.json();
    console.log('Profile updated:', updatedProfile);
    return updatedProfile;
  } else {
    const error = await response.json();
    throw new Error(error.error);
  }
}

// Usage: Update only name
await updateProfile({ name: "New Name" });

// Usage: Update only email
await updateProfile({ email: "newemail@example.com" });

// Usage: Update both
await updateProfile({
  name: "New Name",
  email: "newemail@example.com"
});
```

---

### 3. Get Wallet Details

**Endpoint:** `GET /api/v1/users/profile/wallet`

**Description:** Retrieve wallet details including balance, account number, and pending transaction count.

**Authentication:** Required

**Response (200 OK):**
```json
{
  "balance": 5000.00,
  "accountNumber": "3012345678",
  "walletStatus": "ACTIVE",
  "pendingTransactionCount": 2,
  "currency": "NGN"
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| balance | BigDecimal | Current wallet balance (₦) |
| accountNumber | String | Virtual account number |
| walletStatus | String | Wallet status (ACTIVE, SUSPENDED, LOCKED) |
| pendingTransactionCount | Integer | Number of pending transactions |
| currency | String | Currency code (NGN for Naira) |

**Wallet Status Values:**

| Status | Description |
|--------|-------------|
| ACTIVE | Wallet is active and can process transactions |
| SUSPENDED | Wallet temporarily suspended (contact support) |
| LOCKED | Wallet locked due to security concerns |

**Example (cURL):**
```bash
curl -X GET http://localhost:8080/api/v1/users/profile/wallet \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Example (JavaScript):**
```javascript
async function getWallet() {
  const token = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8080/api/v1/users/profile/wallet', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  const wallet = await response.json();
  console.log('Wallet:', wallet);
  console.log(`Balance: ₦${wallet.balance}`);
  console.log(`Pending Transactions: ${wallet.pendingTransactionCount}`);
  return wallet;
}
```

**Use Cases:**
- Display wallet balance in app
- Show pending transaction notifications
- Check account number for deposits
- Monitor wallet status

---

### 4. Verify Identity (Enhanced KYC)

**Endpoint:** `POST /api/v1/users/profile/verify-identity`

**Description:** Complete enhanced KYC verification by uploading ID document for higher transaction limits.

**Authentication:** Required

**Content-Type:** `multipart/form-data`

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentType | String | Yes | Document type (NIN, DRIVER_LICENSE, INTERNATIONAL_PASSPORT, VOTERS_CARD) |
| documentNumber | String | Yes | Document identification number |
| documentFile | File | No | Document image (JPG, PNG, PDF) max 5MB |

**Document Types:**

| Type | Description |
|------|-------------|
| NIN | National Identification Number |
| DRIVER_LICENSE | Nigerian Driver's License |
| INTERNATIONAL_PASSPORT | International Passport |
| VOTERS_CARD | Permanent Voter's Card |

**File Requirements:**
- **Formats:** JPG, PNG, PDF
- **Max Size:** 5 MB
- **Content:** Clear, readable document image

**Response (200 OK):**
```json
{
  "success": true,
  "kycStatus": "ENHANCED",
  "message": "KYC verification successful. Enhanced account limits activated.",
  "documentType": "NIN",
  "verifiedAt": "2024-12-11T15:30:00"
}
```

**Response (400 Bad Request) - Verification Failed:**
```json
{
  "success": false,
  "kycStatus": "REJECTED",
  "message": "KYC verification failed: Document validation failed. Please ensure document is clear and valid."
}
```

**Response (200 OK) - Already Verified:**
```json
{
  "success": true,
  "kycStatus": "ENHANCED",
  "message": "Account is already enhanced verified.",
  "documentType": "NIN",
  "verifiedAt": "2024-12-10T10:00:00"
}
```

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Document file size must not exceed 5MB | File too large |
| 400 | Document must be an image (JPG, PNG) or PDF | Invalid file type |
| 400 | Document type must be one of: ... | Invalid document type |
| 401 | Not authenticated | Missing or invalid access token |

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/v1/users/profile/verify-identity \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -F "documentType=NIN" \
  -F "documentNumber=12345678901" \
  -F "documentFile=@/path/to/nin_card.jpg"
```

**Example (JavaScript with FormData):**
```javascript
async function verifyIdentity(documentType, documentNumber, file) {
  const token = localStorage.getItem('accessToken');

  const formData = new FormData();
  formData.append('documentType', documentType);
  formData.append('documentNumber', documentNumber);
  if (file) {
    formData.append('documentFile', file);
  }

  const response = await fetch('http://localhost:8080/api/v1/users/profile/verify-identity', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  const result = await response.json();

  if (result.success) {
    console.log('✅ Verification successful!');
    console.log('KYC Status:', result.kycStatus);
    console.log('Enhanced limits activated');
  } else {
    console.log('❌ Verification failed:', result.message);
  }

  return result;
}

// Usage with file input
const fileInput = document.getElementById('documentFile');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  await verifyIdentity('NIN', '12345678901', file);
});
```

**Example (React Component):**
```jsx
import React, { useState } from 'react';

function KycVerification() {
  const [documentType, setDocumentType] = useState('NIN');
  const [documentNumber, setDocumentNumber] = useState('');
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('documentNumber', documentNumber);
    if (file) formData.append('documentFile', file);

    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch('/api/v1/users/profile/verify-identity', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
      });

      const data = await response.json();
      setResult(data);
    } catch (error) {
      console.error('Verification error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <select value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
        <option value="NIN">National ID (NIN)</option>
        <option value="DRIVER_LICENSE">Driver's License</option>
        <option value="INTERNATIONAL_PASSPORT">International Passport</option>
        <option value="VOTERS_CARD">Voter's Card</option>
      </select>

      <input
        type="text"
        placeholder="Document Number"
        value={documentNumber}
        onChange={(e) => setDocumentNumber(e.target.value)}
        required
      />

      <input
        type="file"
        accept="image/*,application/pdf"
        onChange={(e) => setFile(e.target.files[0])}
      />

      <button type="submit" disabled={loading}>
        {loading ? 'Verifying...' : 'Verify Identity'}
      </button>

      {result && (
        <div className={result.success ? 'success' : 'error'}>
          {result.message}
        </div>
      )}
    </form>
  );
}
```

**Mock Verification Logic (PoC):**

For testing purposes, the API uses mock verification:
- Document number ending in **EVEN digit** → ✅ APPROVED
- Document number ending in **ODD digit** → ❌ REJECTED

**Production Implementation:**

In production, replace mock verification with:
1. Real ID verification service (e.g., Smile Identity, Youverify)
2. Document OCR for data extraction
3. Liveness detection
4. Facial recognition matching

---

## KYC Status & Transaction Limits

### Basic Verification (VERIFIED)

**Requirements:**
- ✅ BVN verified during registration
- ✅ Phone number verified

**Limits:**
- Daily transaction limit: ₦50,000
- Single transaction limit: ₦20,000
- Monthly limit: ₦500,000

### Enhanced Verification (ENHANCED)

**Requirements:**
- ✅ Basic verification complete
- ✅ Government-issued ID uploaded and verified

**Limits:**
- Daily transaction limit: ₦500,000
- Single transaction limit: ₦200,000
- Monthly limit: ₦5,000,000

---

## Security & Privacy

### Authorization

- Users can only access their own profile
- User ID extracted from JWT token (not from request)
- Prevents unauthorized access to other users' data

### Data Privacy

- BVN masked in logs (12***78)
- Document numbers masked (1234****89)
- Document files stored securely
- Sensitive data excluded from responses

### Document Storage

**Development:**
```
Directory: /tmp/fulus-kyc/
Filename: {userId}_{documentType}_{timestamp}.{ext}
```

**Production Recommendations:**
- Use cloud storage (AWS S3, Azure Blob, Google Cloud Storage)
- Encrypt at rest
- Set expiration policies
- Implement access controls

---

## Error Handling

### Common Errors

**401 Unauthorized**
```json
{
  "success": false,
  "error": "Not authenticated"
}
```

**400 Bad Request**
```json
{
  "success": false,
  "error": "Email already registered"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "error": "Failed to retrieve profile. Please try again."
}
```

---

## Complete Flow Example

### Registration → Profile → KYC

```bash
# 1. Register user
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "fullName": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com",
    "pin": "5678",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }')

TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.accessToken')

# 2. View profile
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN"

# 3. Check wallet
curl -X GET http://localhost:8080/api/v1/users/profile/wallet \
  -H "Authorization: Bearer $TOKEN"

# 4. Update profile
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name": "Chukwuemeka John Okonkwo"}'

# 5. Enhanced KYC verification
curl -X POST http://localhost:8080/api/v1/users/profile/verify-identity \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=NIN" \
  -F "documentNumber=12345678902" \
  -F "documentFile=@nin_card.jpg"

# 6. Verify KYC status updated
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.kycStatus'
```

---

## Testing Guide

### Test Scenario 1: View Profile

```bash
# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"08012345678","pin":"1234"}' \
  | jq -r '.accessToken')

# Get profile
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  | jq

# Expected: Full profile with all fields
```

### Test Scenario 2: Update Email

```bash
# Update email
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"email":"newemail@example.com"}' \
  | jq

# Expected: 200 OK with updated profile
```

### Test Scenario 3: Duplicate Email

```bash
# Try to use existing email
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"email":"existing@example.com"}' \
  | jq

# Expected: 400 Bad Request - "Email already registered"
```

### Test Scenario 4: Enhanced KYC (Mock - Approved)

```bash
# Document number ending in even digit (approved)
curl -X POST http://localhost:8080/api/v1/users/profile/verify-identity \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=NIN" \
  -F "documentNumber=12345678902" \
  | jq

# Expected: 200 OK with kycStatus=ENHANCED
```

### Test Scenario 5: Enhanced KYC (Mock - Rejected)

```bash
# Document number ending in odd digit (rejected)
curl -X POST http://localhost:8080/api/v1/users/profile/verify-identity \
  -H "Authorization: Bearer $TOKEN" \
  -F "documentType=NIN" \
  -F "documentNumber=12345678901" \
  | jq

# Expected: 400 Bad Request with kycStatus=REJECTED
```

---

## Related Documentation

- [AUTHENTICATION_SUMMARY.md](./AUTHENTICATION_SUMMARY.md) - Authentication system
- [SECURITY_API.md](./SECURITY_API.md) - Security & auth API
- [TRANSACTION_API.md](./TRANSACTION_API.md) - Transaction management
- [README.md](./README.md) - Main documentation

---

**Last Updated:** December 11, 2024
**Version:** 1.0
**Status:** ✅ PRODUCTION READY (with real ID verification service for production)
