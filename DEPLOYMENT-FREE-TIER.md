# Free Tier Deployment Quick Guide

Quick reference for deploying to AWS Free Tier t2.micro/t3.micro instances.

## Prerequisites

- AWS Free Tier account (12 months free)
- t2.micro or t3.micro EC2 instance running
- SSH key configured
- OpenAI API key

## Quick Deployment

### From Your Local Machine

```bash
# Set environment variables
export EC2_HOST="your-ec2-public-dns-or-ip"
export EC2_USER="ubuntu"
export EC2_KEY="~/.ssh/fulus-free-tier-key.pem"
export GIT_REPO="https://github.com/yourusername/fulus-pay-ai-assistant.git"
export GIT_BRANCH="main"

# Deploy to free tier
./deploy-to-ec2.sh free-tier
```

### On EC2 Instance Directly

```bash
# 1. SSH into instance
ssh -i ~/.ssh/fulus-free-tier-key.pem ubuntu@YOUR-EC2-HOST

# 2. Navigate to app directory
cd ~/fulus-pay-ai-assistant

# 3. Pull latest changes
git pull origin main

# 4. Update .env file with your credentials
nano .env

# 5. Deploy with free tier configuration
docker-compose -f docker-compose.free-tier.yml down
docker-compose -f docker-compose.free-tier.yml build --no-cache
docker-compose -f docker-compose.free-tier.yml up -d

# 6. Monitor startup (takes 3-5 minutes)
docker-compose -f docker-compose.free-tier.yml logs -f

# 7. Check health (wait for startup to complete)
curl http://localhost:8080/actuator/health
```

## Setup Systemd Service (Auto-start on Reboot)

```bash
# Copy free tier service file
sudo cp fulus-pay-free-tier.service /etc/systemd/system/

# Reload systemd
sudo systemctl daemon-reload

# Enable auto-start
sudo systemctl enable fulus-pay-free-tier

# Start service
sudo systemctl start fulus-pay-free-tier

# Check status
sudo systemctl status fulus-pay-free-tier
```

## Environment Configuration (.env)

```bash
# Critical for free tier: Use gpt-3.5-turbo (cheaper!)
DB_PASSWORD=YourStrongPassword123
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-3.5-turbo
JWT_SECRET=$(openssl rand -base64 32)
SPRING_PROFILE=dev
```

## Resource Limits (Free Tier)

The `docker-compose.free-tier.yml` is pre-configured with:

- **PostgreSQL:** 256MB RAM, 0.3 CPU
- **Spring Boot App:** 512MB JVM heap, 0.7 CPU
- **Total System:** ~900MB used (leaving 100MB for OS)
- **Swap:** 2GB (configured during setup)

## Monitoring

```bash
# Check memory usage
free -h

# Check Docker container stats
docker stats

# Check disk space
df -h

# Check application logs
docker logs fulus-app --tail=50

# Check if service is running
sudo systemctl status fulus-pay-free-tier
```

## Troubleshooting

### Out of Memory

```bash
# Check swap
swapon --show

# Increase swap if needed
sudo swapoff /swapfile
sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
sudo mkswap /swapfile
sudo swapon /swapfile
```

### Slow Startup

```bash
# Free tier startup takes 3-5 minutes - this is normal!
# Monitor progress
docker logs fulus-app -f

# Check JVM memory settings
docker exec fulus-app java -XX:+PrintFlagsFinal -version | grep -i heap
```

### Application Not Starting

```bash
# Check container logs
docker-compose -f docker-compose.free-tier.yml logs

# Restart containers
docker-compose -f docker-compose.free-tier.yml restart

# Full rebuild
docker-compose -f docker-compose.free-tier.yml down
docker system prune -a
docker-compose -f docker-compose.free-tier.yml up -d --build
```

### Disk Space Issues

```bash
# Clean Docker
docker system prune -a --volumes

# Remove old logs
find logs/ -name "*.log" -mtime +3 -delete

# Check what's using space
du -sh ~/fulus-pay-ai-assistant/*
```

## Cost Monitoring

```bash
# Check AWS Free Tier usage
# Go to: AWS Console → Billing Dashboard → Free Tier

# Set billing alarm (if not done)
# CloudWatch → Alarms → Create Alarm → Billing → $1 threshold
```

## Performance Expectations

| Metric | Free Tier Performance |
|--------|----------------------|
| Startup Time | 3-5 minutes |
| API Response | 200-500ms |
| AI Response | 3-10 seconds |
| Max Users | 5-10 concurrent |
| Monthly Cost | $0 (within limits) |

## API Documentation (Swagger)

The application includes interactive API documentation using Swagger/OpenAPI:

### Access Swagger UI

```bash
# Local development
http://localhost:8080/swagger-ui.html

# Production (replace with your EC2 IP or domain)
http://YOUR-EC2-IP:8080/swagger-ui.html
```

### OpenAPI JSON Specification

```bash
# OpenAPI JSON endpoint
http://localhost:8080/v3/api-docs
```

### Using Swagger UI

1. **Authenticate**:
   - First, call `/api/v1/auth/login` or `/api/v1/auth/register` to get a JWT token
   - Click the "Authorize" button (green lock icon) at the top right
   - Enter: `Bearer YOUR_JWT_TOKEN` (include the word "Bearer")
   - Click "Authorize"

2. **Test APIs**:
   - All endpoints are now authenticated with your JWT token
   - Expand any endpoint and click "Try it out"
   - Fill in parameters and click "Execute"
   - View the response directly in the browser

### Available API Endpoints

- **Auth Controller**: User registration, login, PIN management
- **AI Chat Controller**: Intelligent chat conversations with memory
- **Transaction Controller**: Financial transactions and history
- **User Profile Controller**: User management and KYC
- **Offline Transaction Controller**: Offline transaction chains and sync
- **QR Code Controller**: QR code generation for payments
- **User Device Controller**: Device registration and management

### Security Note

Swagger UI is publicly accessible for development. For production:

```java
// Update SecurityConfig.java to restrict Swagger access
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
    .hasRole("ADMIN") // Only admins can access docs
```

## Useful Commands

```bash
# Start application
sudo systemctl start fulus-pay-free-tier

# Stop application
sudo systemctl stop fulus-pay-free-tier

# Restart application
sudo systemctl restart fulus-pay-free-tier

# View logs
sudo journalctl -u fulus-pay-free-tier -f

# Check health
curl http://localhost:8080/actuator/health

# Access Swagger UI (from EC2)
curl http://localhost:8080/swagger-ui.html

# Manual start (without systemd)
cd ~/fulus-pay-ai-assistant
docker-compose -f docker-compose.free-tier.yml up -d

# Manual stop
docker-compose -f docker-compose.free-tier.yml down
```

## Upgrade to Production

When you outgrow free tier:

```bash
# Deploy to t3.small or t3.medium with production config
./deploy-to-ec2.sh production

# Or manually on EC2
docker-compose -f docker-compose.prod.yml up -d
```

## Key Differences: Free Tier vs Production

| Feature | Free Tier | Production |
|---------|-----------|------------|
| Instance Type | t2.micro/t3.micro | t3.medium |
| RAM | 1GB | 4GB |
| JVM Heap | 512MB | 2GB |
| PostgreSQL | 256MB | 1GB |
| Startup Time | 3-5 min | 1-2 min |
| Concurrent Users | 5-10 | 50-100+ |
| OpenAI Model | gpt-3.5-turbo | gpt-4-turbo |
| Monthly Cost | $0 | ~$44 |

---

For detailed setup instructions, see [README-AWS-FREE-TIER.md](./README-AWS-FREE-TIER.md)

**Last Updated:** 2024-12-11
