# Fulus Pay AI Assistant - Deployment Guide

Complete guide for deploying Fulus Pay AI Assistant to AWS EC2 Ubuntu 22.04.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Initial EC2 Setup](#initial-ec2-setup)
- [Environment Configuration](#environment-configuration)
- [Deployment Process](#deployment-process)
- [Systemd Service Management](#systemd-service-management)
- [Monitoring and Maintenance](#monitoring-and-maintenance)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Local Machine Requirements
- SSH access to EC2 instance
- SSH key file (`.pem`)
- Git installed
- Bash shell

### EC2 Instance Requirements
- **Instance Type:** t3.medium or better (2 vCPU, 4GB RAM minimum)
- **OS:** Ubuntu 22.04 LTS
- **Storage:** 30GB+ EBS volume
- **Security Group:** Open ports 22 (SSH), 8080 (Application)
- **Elastic IP:** Recommended for production

### AWS Configuration
1. Create EC2 instance with Ubuntu 22.04
2. Configure security group:
   ```
   Port 22   - SSH (your IP only)
   Port 8080 - Application (0.0.0.0/0 or load balancer)
   Port 5432 - PostgreSQL (optional, internal only)
   ```
3. Allocate and associate Elastic IP
4. Download SSH key pair (`.pem` file)

## Initial EC2 Setup

### Step 1: Connect to EC2 Instance

```bash
# Set proper permissions on SSH key
chmod 400 ~/.ssh/fulus-ec2-key.pem

# Connect to EC2 instance
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@YOUR-EC2-PUBLIC-IP
```

### Step 2: Run Setup Script

```bash
# Download setup script
wget https://raw.githubusercontent.com/yourusername/fulus-pay-ai-assistant/main/setup-ec2.sh

# Make executable
chmod +x setup-ec2.sh

# Run setup (installs Docker, dependencies, and configures system)
./setup-ec2.sh
```

The setup script will:
- Update system packages
- Install Docker and Docker Compose
- Configure firewall (UFW)
- Setup systemd service
- Configure log rotation
- Create backup scripts
- Apply system optimizations

### Step 3: Clone Repository (if not done by setup script)

```bash
cd /home/ubuntu
git clone https://github.com/yourusername/fulus-pay-ai-assistant.git
cd fulus-pay-ai-assistant
```

## Environment Configuration

### Update .env File

```bash
cd /home/ubuntu/fulus-pay-ai-assistant
nano .env
```

Required variables:

```bash
# Database Configuration
DB_NAME=fulus_ai_db
DB_USERNAME=fulus_user
DB_PASSWORD=your_strong_password_here

# OpenAI Configuration
OPENAI_API_KEY=sk-your-real-openai-api-key
OPENAI_MODEL=gpt-4-turbo

# JWT Configuration (generate with: openssl rand -base64 32)
JWT_SECRET=your-super-secret-jwt-key-at-least-32-characters-long
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Environment
SPRING_PROFILE=prod
```

### Generate Strong JWT Secret

```bash
openssl rand -base64 32
```

### Secure .env File

```bash
chmod 600 .env
chown ubuntu:ubuntu .env
```

## Deployment Process

### Option 1: Deploy from Local Machine (Recommended)

```bash
# Set environment variables
export EC2_HOST="ec2-xx-xx-xx-xx.compute-1.amazonaws.com"
export EC2_USER="ubuntu"
export EC2_KEY="~/.ssh/fulus-ec2-key.pem"
export GIT_REPO="https://github.com/yourusername/fulus-pay-ai-assistant.git"
export GIT_BRANCH="main"

# Run deployment script
./deploy-to-ec2.sh production
```

The deployment script will:
1. Validate SSH connection
2. Create backup of current deployment
3. Pull latest code from repository
4. Stop existing containers
5. Build new Docker images
6. Start containers
7. Run health checks
8. Verify deployment
9. Rollback on failure (optional)

### Option 2: Deploy Directly on EC2

```bash
# SSH into EC2
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@YOUR-EC2-IP

# Navigate to app directory
cd /home/ubuntu/fulus-pay-ai-assistant

# Pull latest changes
git pull origin main

# Build and start containers
docker-compose -f docker-compose.prod.yml up -d --build

# Check status
docker-compose -f docker-compose.prod.yml ps
```

### Manual Deployment Steps

If you need to deploy manually:

```bash
# 1. Stop existing containers
docker-compose -f docker-compose.prod.yml down

# 2. Build images
docker-compose -f docker-compose.prod.yml build --no-cache

# 3. Start containers
docker-compose -f docker-compose.prod.yml up -d

# 4. Check health
curl http://localhost:8080/actuator/health

# 5. View logs
docker-compose -f docker-compose.prod.yml logs -f
```

## Systemd Service Management

The application runs as a systemd service for automatic startup on reboot.

### Service Commands

```bash
# Start service
sudo systemctl start fulus-pay

# Stop service
sudo systemctl stop fulus-pay

# Restart service
sudo systemctl restart fulus-pay

# Check status
sudo systemctl status fulus-pay

# Enable auto-start on boot
sudo systemctl enable fulus-pay

# Disable auto-start
sudo systemctl disable fulus-pay

# View service logs
sudo journalctl -u fulus-pay -f
```

### Service Configuration

Location: `/etc/systemd/system/fulus-pay.service`

```ini
[Unit]
Description=Fulus Pay AI Assistant Docker Compose Service
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ubuntu/fulus-pay-ai-assistant
User=ubuntu
Group=ubuntu
EnvironmentFile=/home/ubuntu/fulus-pay-ai-assistant/.env
ExecStart=/usr/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/bin/docker-compose -f docker-compose.prod.yml down
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
```

### Reload Service After Configuration Changes

```bash
sudo systemctl daemon-reload
sudo systemctl restart fulus-pay
```

## Monitoring and Maintenance

### Check Application Health

```bash
# Health endpoint
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Application info
curl http://localhost:8080/actuator/info
```

### View Logs

```bash
# Application logs
docker-compose -f docker-compose.prod.yml logs -f app

# PostgreSQL logs
docker-compose -f docker-compose.prod.yml logs -f postgres

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100

# Filter by service
docker logs fulus-app-prod -f
```

### Monitor Resource Usage

```bash
# Container stats
docker stats

# Disk usage
df -h

# Memory usage
free -h

# CPU usage
htop
```

### Database Backup

Automated daily backups are configured via cron:

```bash
# Manual backup
./backup-database.sh

# View backups
ls -lh postgres-backup/

# Restore from backup
gunzip < postgres-backup/backup_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i fulus-postgres-prod psql -U fulus_user -d fulus_ai_db
```

### Database Maintenance

```bash
# Connect to PostgreSQL
docker exec -it fulus-postgres-prod psql -U fulus_user -d fulus_ai_db

# Vacuum database
VACUUM ANALYZE;

# Check database size
SELECT pg_size_pretty(pg_database_size('fulus_ai_db'));

# Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Update Application

```bash
# Using deployment script (recommended)
./deploy-to-ec2.sh production

# Manual update
cd /home/ubuntu/fulus-pay-ai-assistant
git pull origin main
docker-compose -f docker-compose.prod.yml up -d --build
```

### Rollback Deployment

If deployment script detects failure, it will prompt for rollback. Manual rollback:

```bash
# List backups
ls -la backups/

# Restore specific backup
BACKUP_NAME="backup_20241211_143022"
cp backups/$BACKUP_NAME/docker-compose.yml .
cp backups/$BACKUP_NAME/.env .
docker-compose -f docker-compose.prod.yml up -d
```

## Troubleshooting

### Container Won't Start

```bash
# Check container status
docker-compose -f docker-compose.prod.yml ps

# View error logs
docker-compose -f docker-compose.prod.yml logs app

# Check environment variables
docker-compose -f docker-compose.prod.yml config

# Rebuild from scratch
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml build --no-cache
docker-compose -f docker-compose.prod.yml up -d
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Test database connection
docker exec fulus-postgres-prod pg_isready -U fulus_user

# Check database logs
docker logs fulus-postgres-prod

# Verify credentials
cat .env | grep DB_
```

### Out of Memory Errors

```bash
# Check memory usage
free -h
docker stats

# Increase swap (if needed)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Reduce JVM heap (in docker-compose.prod.yml)
# Change JAVA_OPTS: -Xmx1g (instead of -Xmx2g)
```

### Port Already in Use

```bash
# Check what's using port 8080
sudo lsof -i :8080

# Kill process if needed
sudo kill -9 <PID>

# Or change port in docker-compose.prod.yml
ports:
  - "8081:8080"
```

### SSL/TLS Certificate Issues

```bash
# For production, use Let's Encrypt with nginx reverse proxy
# Install certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d yourdomain.com
```

### Performance Issues

```bash
# Check system load
uptime
top

# Check disk I/O
iostat -x 2

# Analyze slow queries (PostgreSQL)
docker exec fulus-postgres-prod psql -U fulus_user -d fulus_ai_db -c \
  "SELECT query, calls, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Clear old logs
find logs/ -name "*.log" -mtime +7 -delete
```

### OpenAI API Errors

```bash
# Verify API key
echo $OPENAI_API_KEY

# Test API connectivity
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# Check application logs for rate limits
docker logs fulus-app-prod | grep -i "openai\|rate limit"
```

## Security Best Practices

1. **Keep system updated:**
   ```bash
   sudo apt-get update && sudo apt-get upgrade -y
   ```

2. **Use strong passwords** for database and JWT secret

3. **Restrict SSH access** to specific IP addresses in security group

4. **Enable CloudWatch monitoring** for production

5. **Regular backups** - verify backup script runs daily

6. **Rotate credentials** periodically (database password, JWT secret, API keys)

7. **Use HTTPS** in production with reverse proxy (nginx + Let's Encrypt)

8. **Monitor logs** for suspicious activity

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [AWS EC2 Best Practices](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-best-practices.html)

## Support

For issues and questions, please contact the development team or create an issue in the repository.

---

**Last Updated:** 2024-12-11
