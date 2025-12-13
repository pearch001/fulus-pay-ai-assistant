# Deployment Checklist - Fulus Pay AI Assistant

Complete pre-deployment and post-deployment checklist.

## Pre-Deployment Checklist

### 1. Local Testing ✅

- [ ] All tests passing locally
  ```bash
  mvn clean test
  ```
- [ ] Application builds successfully
  ```bash
  mvn clean package
  ```
- [ ] Docker image builds without errors
  ```bash
  docker build -t fulus-pay:test .
  ```
- [ ] Application runs in local Docker
  ```bash
  docker-compose up -d
  curl http://localhost:8080/actuator/health
  ```

### 2. Code Quality ✅

- [ ] No compiler warnings
- [ ] Code reviewed (if team environment)
- [ ] All TODOs addressed or documented
- [ ] No hardcoded credentials or secrets
- [ ] .env.template updated with new variables
- [ ] README.md updated if needed

### 3. Configuration Files ✅

- [ ] `application.properties` / `application.yml` reviewed
- [ ] `docker-compose.yml` or variant selected
- [ ] `Dockerfile` optimized for target environment
- [ ] `.env` file prepared (not committed!)
- [ ] Environment-specific configs ready

### 4. Security ✅

- [ ] All secrets stored in environment variables
- [ ] JWT secret is strong (32+ characters)
  ```bash
  openssl rand -base64 32
  ```
- [ ] Database password is strong
- [ ] OpenAI API key is valid and has budget limits
- [ ] SSH keys have proper permissions (400)
- [ ] Security group rules reviewed
- [ ] UFW firewall configured

### 5. AWS Preparation ✅

- [ ] EC2 instance is running
- [ ] Instance type matches plan (t2.micro/t3.medium)
- [ ] Security groups properly configured
- [ ] Elastic IP allocated (if production)
- [ ] Domain DNS configured (if applicable)
- [ ] SSL certificate ready (if using HTTPS)
- [ ] Billing alarm set (especially for free tier)

### 6. Dependencies ✅

- [ ] EC2 has Docker installed
- [ ] EC2 has Docker Compose installed
- [ ] Git installed on EC2
- [ ] Swap space configured (critical for free tier)
- [ ] Required ports open (22, 80, 443, 8080)

## Deployment Execution Checklist

### Free Tier Deployment

```bash
# Step 1: SSH Connection Test
[ ] ssh -i ~/.ssh/key.pem ubuntu@EC2_HOST
[ ] Connection successful

# Step 2: Clone/Update Repository
[ ] cd ~ && git clone REPO_URL
[ ] Or: cd ~/app && git pull origin main

# Step 3: Configure Environment
[ ] cp .env.template .env
[ ] nano .env  # Update all values
[ ] chmod 600 .env

# Step 4: Deploy
[ ] docker-compose -f docker-compose.free-tier.yml build
[ ] docker-compose -f docker-compose.free-tier.yml up -d

# Step 5: Verify
[ ] docker ps  # Both containers running
[ ] curl http://localhost:8080/actuator/health
[ ] Response: {"status":"UP"}
```

### Production Deployment

```bash
# Using deployment script
[ ] export EC2_HOST="your-ec2-host"
[ ] export EC2_KEY="~/.ssh/key.pem"
[ ] ./deploy-to-ec2.sh production

# Manual deployment
[ ] docker-compose -f docker-compose.prod.yml down
[ ] docker-compose -f docker-compose.prod.yml build --no-cache
[ ] docker-compose -f docker-compose.prod.yml up -d
```

## Post-Deployment Verification

### 1. Health Checks ✅

- [ ] Application health endpoint
  ```bash
  curl http://EC2_HOST:8080/actuator/health
  # Expected: {"status":"UP"}
  ```
- [ ] Database health
  ```bash
  curl http://EC2_HOST:8080/actuator/health | jq '.components.db.status'
  # Expected: "UP"
  ```
- [ ] All containers running
  ```bash
  docker ps
  # Expected: 2 containers (app + postgres)
  ```

### 2. Functional Testing ✅

- [ ] User registration works
  ```bash
  curl -X POST http://EC2_HOST:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{...test user...}'
  ```
- [ ] User login works
  ```bash
  curl -X POST http://EC2_HOST:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"08012345678","pin":"1234"}'
  ```
- [ ] AI chat endpoint responds
  ```bash
  curl -X POST http://EC2_HOST:8080/api/v1/chat \
    -H "Authorization: Bearer TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"userId":"UUID","message":"Hello"}'
  ```
- [ ] Voice API works (if applicable)
- [ ] Transaction endpoints accessible

### 3. Performance Checks ✅

- [ ] Response time acceptable
  ```bash
  time curl http://EC2_HOST:8080/actuator/health
  # Free tier: < 500ms
  # Production: < 200ms
  ```
- [ ] Memory usage normal
  ```bash
  free -h
  docker stats --no-stream
  ```
- [ ] Disk space available
  ```bash
  df -h
  # Should have >5GB free
  ```
- [ ] CPU usage reasonable
  ```bash
  top
  # Should be <50% idle
  ```

### 4. Security Verification ✅

- [ ] .env file has correct permissions
  ```bash
  ls -la .env
  # Expected: -rw------- (600)
  ```
- [ ] No secrets in logs
  ```bash
  docker logs fulus-app | grep -i "password\|secret\|key"
  # Should not expose secrets
  ```
- [ ] Firewall active
  ```bash
  sudo ufw status
  # Expected: Status: active
  ```
- [ ] SSH access restricted
  ```bash
  # Security group should limit SSH to your IP
  ```
- [ ] HTTPS working (if configured)
  ```bash
  curl -I https://yourdomain.com
  # Expected: HTTP/2 200
  ```

### 5. Logging & Monitoring ✅

- [ ] Application logs accessible
  ```bash
  docker logs fulus-app --tail=50
  ```
- [ ] No critical errors in logs
  ```bash
  docker logs fulus-app | grep -i error
  ```
- [ ] PostgreSQL logs clean
  ```bash
  docker logs fulus-postgres --tail=50
  ```
- [ ] Systemd service enabled
  ```bash
  sudo systemctl status fulus-pay
  # Expected: enabled
  ```
- [ ] CloudWatch agent running (if configured)
  ```bash
  sudo systemctl status amazon-cloudwatch-agent
  ```

### 6. Backup & Recovery ✅

- [ ] Database backup script works
  ```bash
  ./backup-database.sh
  ls -lh postgres-backup/
  ```
- [ ] Backup cron job scheduled
  ```bash
  crontab -l | grep backup
  ```
- [ ] Restore procedure tested
  ```bash
  # Test restore from backup (on test database)
  ```

### 7. Auto-Start Verification ✅

- [ ] Systemd service configured
  ```bash
  sudo systemctl is-enabled fulus-pay
  # Expected: enabled
  ```
- [ ] Test auto-start
  ```bash
  sudo reboot
  # Wait 2 minutes, reconnect
  docker ps
  # Both containers should be running
  ```

## Rollback Checklist

If deployment fails:

- [ ] Stop failed deployment
  ```bash
  docker-compose -f docker-compose.*.yml down
  ```
- [ ] Identify backup to restore
  ```bash
  ls -lt backups/
  ```
- [ ] Restore previous version
  ```bash
  cp backups/BACKUP_NAME/* .
  docker-compose -f docker-compose.*.yml up -d
  ```
- [ ] Verify rollback successful
  ```bash
  curl http://localhost:8080/actuator/health
  ```
- [ ] Document failure reason
- [ ] Fix issues locally
- [ ] Re-deploy when ready

## Free Tier Specific Checks

- [ ] Instance type is t2.micro or t3.micro
- [ ] Storage ≤ 30GB
- [ ] Billing alarm set at $1
- [ ] CloudWatch detailed monitoring DISABLED
- [ ] Using gpt-3.5-turbo (not gpt-4)
- [ ] Memory limits configured in docker-compose
- [ ] Swap enabled (2-4GB)
- [ ] Free tier usage < 750 hours/month
- [ ] Data transfer < 15GB/month

## Production Specific Checks

- [ ] Instance type is t3.medium or higher
- [ ] Elastic IP allocated and associated
- [ ] Domain properly configured
- [ ] SSL/TLS certificate valid
- [ ] CloudWatch monitoring enabled
- [ ] Alarms configured (CPU, Memory, Disk)
- [ ] Backup retention policy set
- [ ] Log rotation configured
- [ ] Rate limiting enabled (if applicable)
- [ ] Load balancer configured (if using)

## Maintenance Schedule Setup

- [ ] Daily: Database backups (automated via cron)
- [ ] Weekly: Log review
- [ ] Weekly: Security updates
- [ ] Monthly: Free tier usage check
- [ ] Monthly: Cost review
- [ ] Quarterly: SSH key rotation
- [ ] Quarterly: Dependency updates

## Documentation Updates

- [ ] README.md updated with deployment details
- [ ] Environment variables documented
- [ ] Known issues documented
- [ ] Troubleshooting guide updated
- [ ] API endpoints verified in documentation

## Stakeholder Communication

- [ ] Notify team of deployment
- [ ] Share deployment URL
- [ ] Share test credentials (if staging)
- [ ] Document any breaking changes
- [ ] Update project status

## Sign-Off

**Deployment Details:**
- Date: _______________
- Environment: [ ] Free Tier  [ ] Production  [ ] Staging
- Deployed by: _______________
- Git commit: _______________
- Docker compose file: _______________

**Verification:**
- [ ] All health checks passed
- [ ] All functional tests passed
- [ ] Performance acceptable
- [ ] Security verified
- [ ] Monitoring active
- [ ] Backups working

**Issues Found:**
- _______________________________________________
- _______________________________________________
- _______________________________________________

**Next Steps:**
- _______________________________________________
- _______________________________________________
- _______________________________________________

**Approved by:** _______________
**Date:** _______________

---

## Quick Reference Commands

```bash
# Check everything
docker ps && \
free -h && \
df -h && \
docker stats --no-stream && \
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f

# Restart application
sudo systemctl restart fulus-pay

# Emergency stop
docker-compose down

# Full system status
~/monitor.sh  # or ~/monitor-free-tier.sh
```

---

**Use this checklist for every deployment to ensure consistency and reliability!**

**Last Updated:** 2024-12-11
