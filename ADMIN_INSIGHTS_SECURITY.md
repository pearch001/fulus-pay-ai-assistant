# Admin Insights Security Configuration

## Application.yml Configuration

Add the following to your `application.yml`:

```yaml
admin:
  insights:
    # IP Whitelist Configuration (optional for demo)
    ip-whitelist:
      enabled: false  # Set to true to enable IP whitelisting
      allowed-ips: "127.0.0.1,192.168.1.100,::1"  # Comma-separated list of allowed IPs
    
    # Rate Limiting (handled in code)
    rate-limit:
      minute: 30  # Max requests per minute
      hour: 100   # Max requests per hour
    
    # AI Model Configuration
    ai:
      model: gpt-4-turbo
      temperature: 0.7
      max-tokens: 1500
```

## Security Features Implemented

### 1. AdminSecurityService
- `validateAdminRole(UUID userId)` - Validates user has ADMIN or SUPER_ADMIN role
- `validateConversationAccess(UUID adminId, UUID conversationId)` - Validates conversation ownership
- `auditAdminAction(...)` - Comprehensive audit logging
- `checkRateLimit(UUID adminId)` - Rate limit validation
- `validateIpAddress(String ipAddress)` - IP whitelist validation
- `sanitizeMessage(String message)` - Request sanitization

### 2. AdminAuditLog Entity
Tracks:
- Admin ID
- Action type
- Resource ID
- Details
- Timestamp
- IP Address
- User Agent
- Status (SUCCESS/FAILURE/ERROR)
- Processing time

### 3. AdminInsightsAuditAspect
Automatically logs:
- All chat messages sent
- Responses received
- Errors encountered
- Processing time
- IP addresses
- User agents

### 4. Message Sanitization
Protects against:
- HTML injection (strips tags)
- XSS attacks (encodes special characters)
- Prompt injection (removes system/assistant markers)
- SQL injection (validated by SafeMessageValidator)
- Command injection (validated by SafeMessageValidator)

### 5. Rate Limiting
- 30 requests per minute per admin
- 100 requests per hour per admin
- Token bucket algorithm
- Automatic refill

### 6. Validation
Multiple layers:
- `@Valid` annotation validation
- `@SafeMessage` custom validator
- `@Pattern` regex validation
- `@Size` length validation
- Runtime sanitization

## Usage Examples

### Enable IP Whitelist for Production
```yaml
admin:
  insights:
    ip-whitelist:
      enabled: true
      allowed-ips: "203.0.113.10,203.0.113.20"  # Your office IPs
```

### Disable for Development
```yaml
admin:
  insights:
    ip-whitelist:
      enabled: false
```

## Audit Log Queries

### Get all logs for an admin
```java
List<AdminAuditLog> logs = securityService.getAdminAuditLogs(adminId);
```

### Get failed actions
```java
List<AdminAuditLog> failures = securityService.getFailedActions(adminId);
```

## Security Best Practices

1. **Always enable IP whitelist in production**
2. **Monitor audit logs regularly**
3. **Review failed actions for security threats**
4. **Keep rate limits reasonable**
5. **Sanitize all user input**
6. **Use HTTPS in production**
7. **Implement token expiration**
8. **Regular security audits**

## Endpoints Protected

All endpoints under `/api/v1/admin/insights/`:
- `POST /chat` - AI chat with full audit
- `GET /conversations` - List conversations
- `GET /conversations/{id}/history` - View history
- `DELETE /conversations/{id}` - Delete conversation

## Audit Actions Logged

- `ADMIN_CHAT_MESSAGE_SENT` - Successful message
- `ADMIN_CHAT_ERROR` - Error during processing
- `ADMIN_CHAT_BLOCKED` - IP blocked
- `ADMIN_CONVERSATION_DELETED` - Conversation deleted
- `ADMIN_CONVERSATION_DELETE_FAILED` - Delete failed
- `CONVERSATION_ACCESS_DENIED` - Unauthorized access
- `RATE_LIMIT_EXCEEDED` - Rate limit hit
- `ADMIN_ROLE_VALIDATION_FAILED` - Role check failed

