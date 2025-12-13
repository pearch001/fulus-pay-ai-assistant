# Deployment Infrastructure Summary

Complete overview of all deployment files and configurations for Fulus Pay AI Assistant.

## 📁 File Structure

```
offline-transactions/
├── .github/
│   └── workflows/
│       └── deploy-to-ec2.yml          # GitHub Actions CI/CD
│
├── Docker Files
│   ├── Dockerfile                      # Production Dockerfile
│   ├── Dockerfile.free-tier            # Free tier optimized Dockerfile
│   ├── docker-compose.yml              # Development setup
│   ├── docker-compose.prod.yml         # Production setup (t3.medium)
│   ├── docker-compose.free-tier.yml    # Free tier setup (t2.micro)
│   └── .dockerignore                   # Docker build exclusions
│
├── Deployment Scripts
│   ├── deploy-to-ec2.sh                # Automated deployment script
│   └── setup-ec2.sh                    # Initial EC2 setup script
│
├── Systemd Services
│   ├── fulus-pay.service               # Production systemd service
│   └── fulus-pay-free-tier.service     # Free tier systemd service
│
├── Environment Configuration
│   ├── .env.template                   # Environment variables template
│   └── .env                            # Actual environment (gitignored)
│
└── Documentation
    ├── README.md                       # Main project README
    ├── README-AWS-SETUP.md             # Full AWS setup guide
    ├── README-AWS-FREE-TIER.md         # Free tier specific guide
    ├── DEPLOYMENT.md                   # General deployment guide
    ├── DEPLOYMENT-FREE-TIER.md         # Free tier quick guide
    ├── DEPLOYMENT-CHECKLIST.md         # Pre/post deployment checklist
    ├── DEPLOYMENT-SUMMARY.md           # This file
    └── README-CICD.md                  # CI/CD setup guide
```

## 🚀 Deployment Options

### Option 1: Free Tier (AWS Free Tier - $0/month)

**Instance:** t2.micro / t3.micro (1GB RAM, 1 vCPU)

**Use Case:** Development, testing, MVP, portfolio projects

**Configuration:**
```bash
# Deployment
./deploy-to-ec2.sh free-tier

# Or manually
docker-compose -f docker-compose.free-tier.yml up -d
```

**Resource Limits:**
- PostgreSQL: 256MB RAM
- Spring Boot: 512MB JVM heap
- Total: ~900MB (100MB for OS)
- Swap: 2-4GB required

**Performance:**
- Startup: 3-5 minutes
- API Response: 200-500ms
- AI Response: 3-10 seconds
- Concurrent Users: 5-10 max

**Monthly Cost:** $0 (within AWS Free Tier limits)

**Documentation:** `README-AWS-FREE-TIER.md`

---

### Option 2: Production (t3.medium - $30-44/month)

**Instance:** t3.medium (4GB RAM, 2 vCPUs)

**Use Case:** Production, 50-100+ concurrent users, customer-facing

**Configuration:**
```bash
# Deployment
./deploy-to-ec2.sh production

# Or manually
docker-compose -f docker-compose.prod.yml up -d
```

**Resource Limits:**
- PostgreSQL: 1GB RAM
- Spring Boot: 2GB JVM heap
- Total: ~3GB (1GB for OS)

**Performance:**
- Startup: 1-2 minutes
- API Response: 50-200ms
- AI Response: 1-5 seconds
- Concurrent Users: 50-100+

**Monthly Cost:** ~$30-44 (AWS) + $20-100 (OpenAI)

**Documentation:** `README-AWS-SETUP.md`

---

### Option 3: Development (Local Docker)

**Use Case:** Local development and testing

**Configuration:**
```bash
docker-compose up -d
```

**Documentation:** `README.md`

---

## 🛠️ Key Files Explained

### 1. Docker Compose Files

#### `docker-compose.yml` (Development)
- Local development setup
- No resource limits
- Hot reload support
- Volume mounting for source code

#### `docker-compose.prod.yml` (Production)
- Production-optimized settings
- JVM heap: 2GB
- PostgreSQL optimizations
- Resource limits enforced
- Health checks configured
- Log rotation

#### `docker-compose.free-tier.yml` (Free Tier)
- Aggressive memory optimization
- JVM heap: 512MB
- PostgreSQL: 256MB
- SerialGC garbage collector
- Lazy initialization
- Critical for 1GB RAM systems

### 2. Dockerfiles

#### `Dockerfile` (Production)
- Multi-stage build
- Maven 3.9 + Java 17
- Alpine Linux (minimal)
- Non-root user
- Optimized layers

#### `Dockerfile.free-tier` (Free Tier)
- Same as Dockerfile but with memory-focused JVM settings
- Smaller final image
- Faster startup optimizations

### 3. Deployment Scripts

#### `deploy-to-ec2.sh`
**Features:**
- Automatic environment detection
- SSH connection validation
- Backup before deployment
- Git pull latest code
- Docker build and deploy
- Health check verification
- Automatic rollback on failure

**Usage:**
```bash
# Free tier
./deploy-to-ec2.sh free-tier

# Production
./deploy-to-ec2.sh production

# Development
./deploy-to-ec2.sh development
```

**Environment Variables:**
```bash
export EC2_HOST="your-ec2-host"
export EC2_USER="ubuntu"
export EC2_KEY="~/.ssh/key.pem"
export GIT_REPO="https://github.com/user/repo.git"
export GIT_BRANCH="main"
```

#### `setup-ec2.sh`
**One-time setup script for fresh EC2 instance**

Installs:
- Docker & Docker Compose
- System dependencies
- Firewall (UFW)
- Swap space
- Systemd service
- Database backup cron job
- Log rotation
- CloudWatch agent (optional)
- Security optimizations

**Usage:**
```bash
# On EC2 instance
./setup-ec2.sh
```

### 4. Systemd Services

#### `fulus-pay.service` (Production)
- Auto-start on boot
- Uses `docker-compose.prod.yml`
- Restart on failure
- Proper dependency ordering

**Commands:**
```bash
sudo systemctl start fulus-pay
sudo systemctl stop fulus-pay
sudo systemctl status fulus-pay
sudo systemctl enable fulus-pay  # Auto-start
```

#### `fulus-pay-free-tier.service` (Free Tier)
- Same as above but uses `docker-compose.free-tier.yml`
- Additional resource limits
- Longer restart delay (30s)

**Commands:**
```bash
sudo systemctl start fulus-pay-free-tier
sudo systemctl stop fulus-pay-free-tier
sudo systemctl status fulus-pay-free-tier
```

### 5. Environment Configuration

#### `.env.template`
Template with all required variables and helpful comments

**Required Variables:**
```bash
DB_PASSWORD=         # Database password
OPENAI_API_KEY=      # OpenAI API key
OPENAI_MODEL=        # gpt-3.5-turbo or gpt-4-turbo
JWT_SECRET=          # Generate with: openssl rand -base64 32
JWT_EXPIRATION=      # 86400000 (24 hours)
JWT_REFRESH_EXPIRATION=  # 604800000 (7 days)
SPRING_PROFILE=      # dev or prod
```

#### `.env` (Create from template)
```bash
cp .env.template .env
nano .env  # Update values
chmod 600 .env  # Secure permissions
```

### 6. CI/CD (GitHub Actions)

#### `.github/workflows/deploy-to-ec2.yml`

**Triggers:**
- Push to `main` → Deploy to free tier
- Push to `production` → Deploy to production
- Manual trigger → Select environment

**Steps:**
1. Checkout code
2. Setup SSH to EC2
3. Pull latest code on EC2
4. Build Docker images
5. Deploy containers
6. Health check
7. Cleanup

**Required Secrets:**
- `EC2_HOST` - EC2 public DNS/IP
- `EC2_SSH_KEY` - Private SSH key content
- `OPENAI_API_KEY` - For tests

**Documentation:** `README-CICD.md`

## 📊 Comparison Table

| Feature | Free Tier | Production | Local Dev |
|---------|-----------|------------|-----------|
| **Instance Type** | t2.micro | t3.medium | N/A |
| **RAM** | 1GB | 4GB | Variable |
| **vCPU** | 1 | 2 | Variable |
| **PostgreSQL Memory** | 256MB | 1GB | 512MB |
| **JVM Heap** | 512MB | 2GB | 1GB |
| **Startup Time** | 3-5 min | 1-2 min | 30-60s |
| **API Response** | 200-500ms | 50-200ms | 10-50ms |
| **AI Response** | 3-10s | 1-5s | 1-3s |
| **Concurrent Users** | 5-10 | 50-100+ | N/A |
| **Monthly Cost (AWS)** | $0 | $30-44 | $0 |
| **Monthly Cost (OpenAI)** | $5-20 | $20-100 | $0 |
| **Recommended For** | MVP, Testing | Production | Development |
| **OpenAI Model** | gpt-3.5-turbo | gpt-4-turbo | gpt-3.5-turbo |
| **Docker Compose** | free-tier.yml | prod.yml | docker-compose.yml |
| **Systemd Service** | fulus-pay-free-tier | fulus-pay | N/A |

## 🔧 Common Deployment Scenarios

### Scenario 1: First Time Deployment (Free Tier)

```bash
# 1. Launch t2.micro EC2 instance
# 2. SSH into instance
ssh -i ~/.ssh/key.pem ubuntu@EC2_HOST

# 3. Run setup script
wget https://raw.githubusercontent.com/user/repo/main/setup-ec2.sh
chmod +x setup-ec2.sh
./setup-ec2.sh

# 4. Clone repository
cd ~
git clone https://github.com/user/repo.git
cd repo

# 5. Configure environment
cp .env.template .env
nano .env  # Update values

# 6. Deploy
docker-compose -f docker-compose.free-tier.yml up -d

# 7. Verify
curl http://localhost:8080/actuator/health
```

### Scenario 2: Automated Deployment from Local Machine

```bash
# 1. Set environment variables
export EC2_HOST="ec2-xx-xx-xx-xx.compute-1.amazonaws.com"
export EC2_KEY="~/.ssh/key.pem"

# 2. Run deployment script
./deploy-to-ec2.sh free-tier

# 3. Script handles everything:
#    - SSH connection
#    - Backup
#    - Git pull
#    - Docker build
#    - Deploy
#    - Health check
#    - Rollback if failed
```

### Scenario 3: GitHub Actions CI/CD

```bash
# 1. Configure GitHub Secrets
#    - EC2_HOST
#    - EC2_SSH_KEY
#    - OPENAI_API_KEY

# 2. Push to main branch
git push origin main

# 3. GitHub Actions automatically:
#    - Runs tests
#    - Deploys to EC2
#    - Verifies health
#    - Sends notification
```

### Scenario 4: Upgrade from Free Tier to Production

```bash
# 1. Launch t3.medium EC2 instance
# 2. Run setup script
./setup-ec2.sh

# 3. Clone repository and configure
git clone https://github.com/user/repo.git
cd repo
cp .env.template .env
nano .env

# 4. Deploy with production config
docker-compose -f docker-compose.prod.yml up -d

# 5. Setup systemd service
sudo cp fulus-pay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable fulus-pay
sudo systemctl start fulus-pay
```

## 📖 Documentation Quick Links

| Topic | File | Purpose |
|-------|------|---------|
| **Getting Started** | `README.md` | Main project documentation |
| **Local Development** | `README.md` | Docker setup for local dev |
| **AWS Setup (Full)** | `README-AWS-SETUP.md` | Complete AWS deployment guide |
| **Free Tier Guide** | `README-AWS-FREE-TIER.md` | Free tier specific instructions |
| **Quick Deploy** | `DEPLOYMENT-FREE-TIER.md` | Quick reference for free tier |
| **General Deployment** | `DEPLOYMENT.md` | General deployment procedures |
| **Deployment Checklist** | `DEPLOYMENT-CHECKLIST.md` | Pre/post deployment checks |
| **CI/CD Setup** | `README-CICD.md` | GitHub Actions configuration |
| **This Summary** | `DEPLOYMENT-SUMMARY.md` | Overview of all deployment files |

## 🎯 Quick Command Reference

```bash
# === DEPLOYMENT ===
# Free tier (local machine)
./deploy-to-ec2.sh free-tier

# Production (local machine)
./deploy-to-ec2.sh production

# Manual on EC2 (free tier)
docker-compose -f docker-compose.free-tier.yml up -d

# Manual on EC2 (production)
docker-compose -f docker-compose.prod.yml up -d

# === SYSTEMD ===
sudo systemctl start fulus-pay
sudo systemctl stop fulus-pay
sudo systemctl restart fulus-pay
sudo systemctl status fulus-pay

# === MONITORING ===
# Health check
curl http://localhost:8080/actuator/health

# Container stats
docker stats

# View logs
docker logs fulus-app -f
docker-compose logs -f

# System resources
free -h
df -h
top

# === MAINTENANCE ===
# Backup database
./backup-database.sh

# Clean Docker
docker system prune -a

# Update application
git pull origin main
docker-compose -f docker-compose.*.yml up -d --build

# === TROUBLESHOOTING ===
# Restart everything
sudo systemctl restart fulus-pay

# View recent logs
docker-compose logs --tail=100

# Check environment
docker-compose config

# Interactive shell in container
docker exec -it fulus-app sh
```

## 💰 Cost Summary

### Free Tier (12 months)
```
AWS EC2 (t2.micro):          $0/month
AWS EBS (30GB):              $0/month
Data Transfer (15GB):        $0/month
OpenAI (gpt-3.5-turbo):      $5-20/month
───────────────────────────────────────
Total:                       $5-20/month
```

### Production
```
AWS EC2 (t3.medium):         $30.37/month
AWS EBS (30GB):              $2.40/month
Data Transfer (100GB):       $9.00/month
CloudWatch:                  $2.10/month
OpenAI (gpt-4-turbo):        $20-100/month
───────────────────────────────────────
Total:                       $63.87-143.87/month
```

## ✅ Next Steps

1. **Choose your deployment path:**
   - [ ] Free Tier - See `README-AWS-FREE-TIER.md`
   - [ ] Production - See `README-AWS-SETUP.md`
   - [ ] Local Dev - See `README.md`

2. **Complete setup:**
   - [ ] Launch EC2 instance
   - [ ] Run `setup-ec2.sh`
   - [ ] Configure `.env` file
   - [ ] Deploy application

3. **Verify deployment:**
   - [ ] Use `DEPLOYMENT-CHECKLIST.md`
   - [ ] Run health checks
   - [ ] Test functionality

4. **Setup CI/CD (optional):**
   - [ ] Configure GitHub Secrets
   - [ ] Enable GitHub Actions
   - [ ] See `README-CICD.md`

5. **Ongoing maintenance:**
   - [ ] Monitor free tier usage
   - [ ] Review logs weekly
   - [ ] Update dependencies monthly
   - [ ] Rotate secrets quarterly

---

**Need Help?**
- Check specific documentation files above
- Review deployment checklist
- Test locally first
- Use deployment script for automation

**Last Updated:** 2024-12-11
