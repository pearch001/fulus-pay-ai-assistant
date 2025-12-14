# Admin Insights Configuration Guide

## Overview
This document explains all configuration options for the Admin Business Insights feature.

## Configuration Files

### 1. application.yml (Development)
Main configuration file with default development settings.

### 2. application-prod.yml (Production)
Production-specific overrides with stricter security settings.

### 3. .env
Environment-specific variables (should not be committed to git).

## Configuration Sections

### Spring AI - OpenAI Admin Configuration

```yaml
spring:
  ai:
    openai:
      admin:
        model: gpt-4-turbo              # AI model for admin insights
        temperature: 0.7                # Creativity level (0.0-1.0)
        max-tokens: 1500                # Maximum response length
        timeout: 30s                    # API call timeout
        retry:
          max-attempts: 3               # Retry failed API calls
          backoff:
            initial-interval: 1s        # Initial retry delay
            multiplier: 2               # Backoff multiplier
            max-interval: 10s           # Maximum retry delay
```

**Environment Variables:**
- `OPENAI_ADMIN_MODEL` - Override the AI model (default: gpt-4-turbo)

**Options:**
- `gpt-4-turbo` - Best quality, higher cost (recommended)
- `gpt-4` - High quality, moderate cost
- `gpt-3.5-turbo` - Fast, lower cost (development only)

---

### Admin Insights Main Configuration

```yaml
admin-insights:
  enabled: true                         # Enable/disable the feature
  
  cache:
    ttl: 300                           # Cache time-to-live in seconds
    max-size: 100                      # Maximum cached responses
    enabled: true                      # Enable Redis caching
  
  rate-limit:
    requests-per-minute: 30            # Max requests per admin per minute
    requests-per-hour: 100             # Max requests per admin per hour
    enabled: true                      # Enable rate limiting
  
  conversation:
    max-messages: 100                  # Maximum messages per conversation
    history-limit: 20                  # Messages included in AI context
    cleanup-days: 30                   # Delete inactive conversations after N days
    auto-summarize-interval: 10        # Generate summary every N messages
  
  stats:
    cache-duration: 300                # Platform stats cache duration (seconds)
    refresh-interval: 300              # Stats refresh interval (seconds)
  
  ip-whitelist:
    enabled: false                     # Enable IP whitelisting (production: true)
    allowed-ips: "127.0.0.1,::1"      # Comma-separated IP addresses
  
  token-budget:
    max-context-tokens: 3000           # Maximum tokens for conversation context
    reserved-for-response: 1500        # Tokens reserved for AI response
    warning-threshold: 2500            # Log warning when approaching limit
```

**Environment Variables:**
- `ADMIN_INSIGHTS_ENABLED` - Enable/disable feature (default: true)
- `ADMIN_ALLOWED_IPS` - Comma-separated list of allowed IP addresses

---

## Configuration by Environment

### Development Settings
```yaml
admin-insights:
  enabled: true
  cache:
    ttl: 300                           # 5 minutes
  rate-limit:
    requests-per-minute: 30            # More lenient for testing
    requests-per-hour: 100
  conversation:
    cleanup-days: 30                   # Shorter retention
  ip-whitelist:
    enabled: false                     # Disabled for easier testing
```

### Production Settings
```yaml
admin-insights:
  enabled: true
  cache:
    ttl: 300
    max-size: 200                      # Increased capacity
  rate-limit:
    requests-per-minute: 20            # Stricter limits
    requests-per-hour: 100
  conversation:
    cleanup-days: 90                   # Longer retention
  stats:
    cache-duration: 600                # 10 minutes (reduce API calls)
  ip-whitelist:
    enabled: true                      # MUST enable in production
    allowed-ips: ${ADMIN_ALLOWED_IPS}  # From environment
```

---

## Performance Tuning

### Cache Configuration

**TTL (Time-To-Live):**
- **Development:** 300s (5 min) - Frequent changes
- **Production:** 300-600s (5-10 min) - Balance freshness vs. API costs

**Max Size:**
- **Development:** 100 responses
- **Production:** 200-500 responses (based on admin count)

### Rate Limiting

**Requests per Minute:**
- **Development:** 30 (testing)
- **Production:** 10-20 (prevent abuse)

**Requests per Hour:**
- **Development:** 100
- **Production:** 50-100

### Token Budget

**Max Context Tokens:**
- 3000 tokens = ~2,250 words of context
- Includes: system prompt + history + current message

**Reserved for Response:**
- 1500 tokens = ~1,125 words for AI response
- Ensures quality, detailed responses

---

## Security Configuration

### IP Whitelisting

**Development:**
```yaml
ip-whitelist:
  enabled: false  # Disabled for ease of development
```

**Production:**
```yaml
ip-whitelist:
  enabled: true
  allowed-ips: "203.0.113.10,203.0.113.20,10.0.0.0/8"
```

**Formats Supported:**
- Single IP: `192.168.1.100`
- IPv6: `2001:db8::1`
- CIDR notation: `10.0.0.0/8`
- Wildcard: `*` (not recommended for production)

### Rate Limiting Strategy

1. **Per-Admin Limits:** Each admin gets their own quota
2. **Token Bucket Algorithm:** Smooth rate limiting with burst capacity
3. **Automatic Reset:** Quotas reset every minute/hour
4. **Graceful Degradation:** Returns 429 Too Many Requests

---

## Database Configuration
      port: 6379
```

### Rate Limit Too Strict
```yaml
# Temporarily increase limits
admin-insights:
  rate-limit:
    requests-per-minute: 60
    requests-per-hour: 300
```

### High Token Usage
```yaml
# Reduce context size
admin-insights:
  conversation:
    history-limit: 10
  token-budget:
    max-context-tokens: 2000
```

### IP Whitelist Blocking
```yaml
# Disable temporarily (development only!)
admin-insights:
  ip-whitelist:
    enabled: false
```

---

## Best Practices

### Development
- Keep cache TTL low (5 min) for faster iteration
- Disable IP whitelist for easier testing
- Use gpt-3.5-turbo to reduce costs
- Enable verbose logging

### Production
- Enable IP whitelist with specific IPs
- Increase cache TTL (10 min) for cost savings
- Use gpt-4-turbo for best quality
- Set appropriate rate limits based on admin count
- Monitor cache hit rates and optimize TTL
- Implement conversation cleanup job
- Track token usage for billing

---

## Sample .env Configurations

### Local Development
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-3.5-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=127.0.0.1,::1
SPRING_PROFILE=dev
```

### Staging
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-4-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=10.0.0.0/8,192.168.1.0/24
SPRING_PROFILE=prod
```

### Production
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-4-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=203.0.113.10,203.0.113.20
SPRING_PROFILE=prod
```

---

## Related Documentation
- Main README: `README.md`
- Security Guide: `ADMIN_INSIGHTS_SECURITY.md`
- Migration Guide: `db/migration/README_V6_ADMIN_INSIGHTS.md`
- API Documentation: Swagger UI at `/swagger-ui.html`

## Overview
This document explains all configuration options for the Admin Business Insights feature.

## Configuration Files

### 1. application.yml (Development)
Main configuration file with default development settings.

### 2. application-prod.yml (Production)
Production-specific overrides with stricter security settings.

### 3. .env
Environment-specific variables (should not be committed to git).

## Configuration Sections

### Spring AI - OpenAI Admin Configuration

```yaml
spring:
  ai:
    openai:
      admin:
        model: gpt-4-turbo              # AI model for admin insights
        temperature: 0.7                # Creativity level (0.0-1.0)
        max-tokens: 1500                # Maximum response length
        timeout: 30s                    # API call timeout
        retry:
          max-attempts: 3               # Retry failed API calls
          backoff:
            initial-interval: 1s        # Initial retry delay
            multiplier: 2               # Backoff multiplier
            max-interval: 10s           # Maximum retry delay
```

**Environment Variables:**
- `OPENAI_ADMIN_MODEL` - Override the AI model (default: gpt-4-turbo)

**Options:**
- `gpt-4-turbo` - Best quality, higher cost (recommended)
- `gpt-4` - High quality, moderate cost
- `gpt-3.5-turbo` - Fast, lower cost (development only)

---

### Admin Insights Main Configuration

```yaml
admin-insights:
  enabled: true                         # Enable/disable the feature
  
  cache:
    ttl: 300                           # Cache time-to-live in seconds
    max-size: 100                      # Maximum cached responses
    enabled: true                      # Enable Redis caching
  
  rate-limit:
    requests-per-minute: 30            # Max requests per admin per minute
    requests-per-hour: 100             # Max requests per admin per hour
    enabled: true                      # Enable rate limiting
  
  conversation:
    max-messages: 100                  # Maximum messages per conversation
    history-limit: 20                  # Messages included in AI context
    cleanup-days: 30                   # Delete inactive conversations after N days
    auto-summarize-interval: 10        # Generate summary every N messages
  
  stats:
    cache-duration: 300                # Platform stats cache duration (seconds)
    refresh-interval: 300              # Stats refresh interval (seconds)
  
  ip-whitelist:
    enabled: false                     # Enable IP whitelisting (production: true)
    allowed-ips: "127.0.0.1,::1"      # Comma-separated IP addresses
  
  token-budget:
    max-context-tokens: 3000           # Maximum tokens for conversation context
    reserved-for-response: 1500        # Tokens reserved for AI response
    warning-threshold: 2500            # Log warning when approaching limit
```

**Environment Variables:**
- `ADMIN_INSIGHTS_ENABLED` - Enable/disable feature (default: true)
- `ADMIN_ALLOWED_IPS` - Comma-separated list of allowed IP addresses

---

## Configuration by Environment

### Development Settings
```yaml
admin-insights:
  enabled: true
  cache:
    ttl: 300                           # 5 minutes
  rate-limit:
    requests-per-minute: 30            # More lenient for testing
    requests-per-hour: 100
  conversation:
    cleanup-days: 30                   # Shorter retention
  ip-whitelist:
    enabled: false                     # Disabled for easier testing
```

### Production Settings
```yaml
admin-insights:
  enabled: true
  cache:
    ttl: 300
    max-size: 200                      # Increased capacity
  rate-limit:
    requests-per-minute: 20            # Stricter limits
    requests-per-hour: 100
  conversation:
    cleanup-days: 90                   # Longer retention
  stats:
    cache-duration: 600                # 10 minutes (reduce API calls)
  ip-whitelist:
    enabled: true                      # MUST enable in production
    allowed-ips: ${ADMIN_ALLOWED_IPS}  # From environment
```

---

## Performance Tuning

### Cache Configuration

**TTL (Time-To-Live):**
- **Development:** 300s (5 min) - Frequent changes
- **Production:** 300-600s (5-10 min) - Balance freshness vs. API costs

**Max Size:**
- **Development:** 100 responses
- **Production:** 200-500 responses (based on admin count)

### Rate Limiting

**Requests per Minute:**
- **Development:** 30 (testing)
- **Production:** 10-20 (prevent abuse)

**Requests per Hour:**
- **Development:** 100
- **Production:** 50-100

### Token Budget

**Max Context Tokens:**
- 3000 tokens = ~2,250 words of context
- Includes: system prompt + history + current message

**Reserved for Response:**
- 1500 tokens = ~1,125 words for AI response
- Ensures quality, detailed responses

---

## Security Configuration

### IP Whitelisting

**Development:**
```yaml
ip-whitelist:
  enabled: false  # Disabled for ease of development
```

**Production:**
```yaml
ip-whitelist:
  enabled: true
  allowed-ips: "203.0.113.10,203.0.113.20,10.0.0.0/8"
```

**Formats Supported:**
- Single IP: `192.168.1.100`
- IPv6: `2001:db8::1`
- CIDR notation: `10.0.0.0/8`
- Wildcard: `*` (not recommended for production)

### Rate Limiting Strategy

1. **Per-Admin Limits:** Each admin gets their own quota
2. **Token Bucket Algorithm:** Smooth rate limiting with burst capacity
3. **Automatic Reset:** Quotas reset every minute/hour
4. **Graceful Degradation:** Returns 429 Too Many Requests

---

## Database Configuration

The admin insights feature uses the main application database.

**Tables:**
- `admin_conversations` - Chat sessions
- `admin_chat_messages` - Individual messages
- `admin_audit_logs` - Security audit trail

**Retention:**
```yaml
conversation:
  cleanup-days: 30  # Auto-delete inactive conversations
```

**Scheduled Cleanup:**
```sql
-- Automatic cleanup job (configure in application)
DELETE FROM admin_conversations 
WHERE is_active = false 
AND updated_at < NOW() - INTERVAL '30 days';
```

---

## Monitoring & Metrics

### Cache Metrics
Monitor via logs (every 5 minutes):
```
Admin Insights Cache Stats - Hits: 45, Misses: 12, Hit Rate: 78.95%, API Calls: 12, Calls Saved: 45
```

**Key Metrics:**
- **Hit Rate:** Target >60% for good caching
- **API Calls Saved:** Indicates cost savings
- **Cache Misses:** High misses = increase TTL

### Rate Limiting Metrics
Check audit logs for:
```sql
SELECT COUNT(*) FROM admin_audit_logs 
WHERE action = 'RATE_LIMIT_EXCEEDED' 
AND timestamp > NOW() - INTERVAL '1 hour';
```

### Token Usage Tracking
```sql
SELECT 
  admin_id,
  SUM(total_tokens) as total_tokens,
  COUNT(*) as conversations
FROM admin_conversations
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY admin_id;
```

---

## Cost Optimization

### Reducing OpenAI Costs

1. **Increase Cache TTL:**
   ```yaml
   cache:
     ttl: 600  # 10 minutes instead of 5
   ```

2. **Reduce Context Size:**
   ```yaml
   conversation:
     history-limit: 10  # Instead of 20
   ```

3. **Use Cheaper Model (Development):**
   ```yaml
   admin:
     model: gpt-3.5-turbo  # Instead of gpt-4-turbo
   ```

4. **Stricter Rate Limits:**
   ```yaml
   rate-limit:
     requests-per-hour: 50  # Instead of 100
   ```

### Estimated Costs

**With gpt-4-turbo:**
- Input: $0.01 per 1K tokens
- Output: $0.03 per 1K tokens
- Average query: ~2,500 tokens = $0.05
- 100 queries/day = $5/day = $150/month

**With Caching (60% hit rate):**
- 40 API calls (60 cached) = $2/day = $60/month
- **Savings: 60%**

---

## Troubleshooting

### Cache Not Working
```yaml
# Verify Redis is enabled
spring:
  cache:
    type: redis

# Check Redis connection
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Rate Limit Too Strict
```yaml
# Temporarily increase limits
admin-insights:
  rate-limit:
    requests-per-minute: 60
    requests-per-hour: 300
```

### High Token Usage
```yaml
# Reduce context size
admin-insights:
  conversation:
    history-limit: 10
  token-budget:
    max-context-tokens: 2000
```

### IP Whitelist Blocking
```yaml
# Disable temporarily (development only!)
admin-insights:
  ip-whitelist:
    enabled: false
```

---

## Best Practices

### Development
- Keep cache TTL low (5 min) for faster iteration
- Disable IP whitelist for easier testing
- Use gpt-3.5-turbo to reduce costs
- Enable verbose logging

### Production
- Enable IP whitelist with specific IPs
- Increase cache TTL (10 min) for cost savings
- Use gpt-4-turbo for best quality
- Set appropriate rate limits based on admin count
- Monitor cache hit rates and optimize TTL
- Implement conversation cleanup job
- Track token usage for billing

---

## Sample .env Configurations

### Local Development
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-3.5-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=127.0.0.1,::1
SPRING_PROFILE=dev
```

### Staging
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-4-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=10.0.0.0/8,192.168.1.0/24
SPRING_PROFILE=prod
```

### Production
```bash
OPENAI_API_KEY=sk-...
OPENAI_ADMIN_MODEL=gpt-4-turbo
ADMIN_INSIGHTS_ENABLED=true
ADMIN_ALLOWED_IPS=203.0.113.10,203.0.113.20
SPRING_PROFILE=prod
```

---

## Related Documentation
- Main README: `README.md`
- Security Guide: `ADMIN_INSIGHTS_SECURITY.md`
- Migration Guide: `db/migration/README_V6_ADMIN_INSIGHTS.md`
- API Documentation: Swagger UI at `/swagger-ui.html`
