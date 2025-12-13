# Key Registration Guide

## Overview
To use offline transactions and QR code generation, you must first register a cryptographic key pair. The system supports both RSA and ECDSA algorithms.

---

## Available Endpoints

### 1. Generate Keys (Server-Side - For Testing Only)

**Endpoint:** `POST /api/v1/crypto/generate-keys`

**Description:** Generates a key pair on the server and automatically registers the public key.

**⚠️ WARNING:** Only use this for testing/development. In production, keys should be generated on the client device.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/crypto/generate-keys?algorithm=RSA \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Query Parameters:**
- `algorithm` (optional): `RSA` (default) or `ECDSA`

**Response:**
```json
{
  "success": true,
  "message": "Key pair generated successfully. IMPORTANT: Store your private key securely!",
  "data": {
    "keyId": "550e8400-e29b-41d4-a716-446655440000",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
    "privateKey": "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSj...",
    "algorithm": "RSA",
    "keySize": 2048,
    "createdAt": "2025-12-13T13:00:00.000",
    "expiresAt": "2027-12-13T13:00:00.000",
    "format": "PKCS8/X509"
  },
  "timestamp": "2025-12-13T13:00:00.000"
}
```

**⚠️ IMPORTANT:** Save the `privateKey` securely! You'll need it to sign offline transactions.

---

### 2. Register Public Key (Production Method)

**Endpoint:** `POST /api/v1/crypto/register-keys`

**Description:** Register a public key that was generated on your device.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/crypto/register-keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
    "algorithm": "RSA",
    "keySize": 2048
  }'
```

**Request Body:**
```json
{
  "publicKey": "BASE64_ENCODED_PUBLIC_KEY",
  "algorithm": "RSA",      // Optional: defaults to "RSA"
  "keySize": 2048          // Optional: defaults to 2048
}
```

**Response:**
```json
{
  "success": true,
  "message": "Public key registered successfully. You can now generate QR codes!",
  "data": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-12-13T13:00:00.000"
}
```

---

### 3. Get Registered Public Key

**Endpoint:** `GET /api/v1/crypto/public-key`

**Description:** Retrieve your currently registered public key.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/crypto/public-key \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Public key retrieved",
  "data": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
  "timestamp": "2025-12-13T13:00:00.000"
}
```

**Error (No Key Registered):**
```json
{
  "success": false,
  "message": "No public key registered. Please register your keys first.",
  "data": null,
  "timestamp": "2025-12-13T13:00:00.000"
}
```

---

## Quick Start Guide

### For Testing/Development

1. **Login to get JWT token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+2348012345678",
    "password": "yourpassword"
  }'
```

2. **Generate and register keys (server-side):**
```bash
curl -X POST http://localhost:8080/api/v1/crypto/generate-keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. **Save the returned private key** to a secure location.

4. **Now you can generate QR codes:**
```bash
curl -X POST http://localhost:8080/api/v1/offline/qr/generate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "note": "Payment for services",
    "qrSize": 300
  }'
```

---

### For Production (Mobile App)

1. **Generate keys on the mobile device:**
```java
// Android/Java example
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(2048);
KeyPair keyPair = keyGen.generateKeyPair();

// Export public key
String publicKey = Base64.getEncoder()
    .encodeToString(keyPair.getPublic().getEncoded());

// Store private key in Android Keystore
// ... (secure storage implementation)
```

2. **Register only the public key with the server:**
```bash
curl -X POST http://localhost:8080/api/v1/crypto/register-keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "publicKey": "YOUR_DEVICE_GENERATED_PUBLIC_KEY",
    "algorithm": "RSA",
    "keySize": 2048
  }'
```

3. **Use the private key on device to sign transactions.**

---

## Security Best Practices

✅ **DO:**
- Generate keys on the client device (mobile app)
- Store private keys in secure hardware (Android Keystore, iOS Keychain)
- Only send public keys to the server
- Rotate keys every 1-2 years

❌ **DON'T:**
- Send private keys over the network
- Store private keys in plain text
- Share your private key with anyone
- Use server-generated keys in production

---

## Troubleshooting

### Error: "User does not have a registered key pair"
**Solution:** Register your keys using `/api/v1/crypto/generate-keys` or `/api/v1/crypto/register-keys`

### Error: "No public key registered"
**Solution:** You need to register a public key before you can use crypto features.

### Error: "Invalid algorithm"
**Solution:** Only `RSA` and `ECDSA` algorithms are supported.

---

## API Flow

```
1. User Login → Get JWT Token
2. Generate/Register Keys → Store Private Key Securely
3. Generate QR Code → Uses Registered Public Key
4. Sign Transactions → Uses Private Key (on device)
5. Validate Transactions → Uses Public Key (on server)
```

---

## Next Steps

After registering your keys, you can:
- Generate payment QR codes
- Create offline transactions
- Sign transaction payloads
- Sync offline transactions when back online

For more details, see:
- `OFFLINE_TRANSACTION_IMPLEMENTATION.md`
- `NFC_PAYLOAD_SPECIFICATION.md`

