# AWS EC2 Setup Guide - Fulus Pay AI Assistant

Complete step-by-step guide for deploying Fulus Pay AI Assistant on AWS EC2 from scratch.

## Table of Contents
- [Overview](#overview)
- [AWS Prerequisites](#aws-prerequisites)
- [Step 1: Launch EC2 Instance](#step-1-launch-ec2-instance)
- [Step 2: Configure Security Groups](#step-2-configure-security-groups)
- [Step 3: Connect to EC2 Instance](#step-3-connect-to-ec2-instance)
- [Step 4: Initial Server Setup](#step-4-initial-server-setup)
- [Step 5: Deploy Application](#step-5-deploy-application)
- [Step 6: Configure Domain & SSL (Optional)](#step-6-configure-domain--ssl-optional)
- [Step 7: Setup Monitoring](#step-7-setup-monitoring)
- [Step 8: Verify Deployment](#step-8-verify-deployment)
- [Maintenance & Updates](#maintenance--updates)
- [Cost Estimation](#cost-estimation)

---

## Overview

This guide will help you deploy Fulus Pay AI Assistant on AWS EC2 with:
- ✅ Ubuntu 22.04 LTS
- ✅ Docker & Docker Compose
- ✅ PostgreSQL 15 (containerized)
- ✅ Auto-start on reboot
- ✅ SSL/TLS with Let's Encrypt
- ✅ CloudWatch monitoring

**Estimated Setup Time:** 30-45 minutes

---

## AWS Prerequisites

Before starting, ensure you have:

1. **AWS Account** with billing enabled
2. **AWS CLI** installed (optional but recommended)
3. **SSH Client** (Terminal on Mac/Linux, PuTTY on Windows)
4. **Domain Name** (optional, for SSL)
5. **OpenAI API Key** from https://platform.openai.com

---

## Step 1: Launch EC2 Instance

### 1.1 Login to AWS Console

1. Go to https://console.aws.amazon.com
2. Navigate to **EC2 Dashboard**
3. Click **Launch Instance**

### 1.2 Configure Instance

#### Basic Details
- **Name:** `fulus-pay-production`
- **Application and OS Images:** Ubuntu Server 22.04 LTS
- **Architecture:** 64-bit (x86)

#### Instance Type
- **Instance Type:** `t3.medium`
  - 2 vCPUs
  - 4 GB RAM
  - Up to 5 Gigabit network

> **Note:** For development/testing, you can use `t3.small` (2GB RAM), but `t3.medium` is recommended for production.

#### Key Pair (Login)
1. Click **Create new key pair**
2. **Key pair name:** `fulus-ec2-key`
3. **Key pair type:** RSA
4. **Private key file format:** `.pem` (Mac/Linux) or `.ppk` (Windows/PuTTY)
5. Click **Create key pair** (downloads automatically)
6. **Save this file securely** - you'll need it to connect!

#### Network Settings
- **VPC:** Default VPC
- **Subnet:** No preference
- **Auto-assign public IP:** Enable
- **Firewall (security groups):** Create new security group
  - Name: `fulus-pay-sg`
  - Description: Security group for Fulus Pay AI Assistant

> We'll configure detailed rules in Step 2

#### Configure Storage
- **Storage:** 30 GB gp3 SSD
  - **IOPS:** 3000
  - **Throughput:** 125 MB/s
- **Delete on termination:** Yes (uncheck for production data persistence)

#### Advanced Details (Optional)
- **Monitoring:** Enable CloudWatch detailed monitoring ($2.10/month extra)
- **User data:** Leave empty (we'll setup manually)

### 1.3 Launch Instance

1. Review all settings in the **Summary** panel
2. Click **Launch instance**
3. Wait 2-3 minutes for instance to start
4. Note down the **Public IPv4 address**

### 1.4 Allocate Elastic IP (Recommended for Production)

An Elastic IP ensures your server's IP address doesn't change on restart.

1. In EC2 Dashboard, go to **Elastic IPs** (left sidebar)
2. Click **Allocate Elastic IP address**
3. Click **Allocate**
4. Select the newly allocated IP
5. Click **Actions** → **Associate Elastic IP address**
6. **Instance:** Select `fulus-pay-production`
7. Click **Associate**

> **Note:** Elastic IPs are free when associated with a running instance, but cost $0.005/hour when unused.

---

## Step 2: Configure Security Groups

Security groups act as a virtual firewall for your EC2 instance.

### 2.1 Edit Security Group

1. In EC2 Dashboard, go to **Security Groups**
2. Select `fulus-pay-sg`
3. Click **Edit inbound rules**

### 2.2 Add Inbound Rules

| Type  | Protocol | Port Range | Source          | Description          |
|-------|----------|------------|-----------------|----------------------|
| SSH   | TCP      | 22         | My IP           | SSH access           |
| HTTP  | TCP      | 80         | 0.0.0.0/0       | HTTP web traffic     |
| HTTPS | TCP      | 443        | 0.0.0.0/0       | HTTPS web traffic    |
| Custom TCP | TCP | 8080    | 0.0.0.0/0       | Application port     |

### 2.3 Security Best Practices

**For Production:**
- Restrict SSH (port 22) to **your IP only** or use a VPN
- Consider using **AWS Systems Manager Session Manager** (no SSH needed)
- Use a **Load Balancer** and only allow traffic from the load balancer to port 8080

**For Development:**
- You can allow SSH from "Anywhere" temporarily (0.0.0.0/0)
- Remember to restrict it later!

### 2.4 Outbound Rules

Default outbound rule (All traffic to 0.0.0.0/0) is sufficient.

Click **Save rules**

---

## Step 3: Connect to EC2 Instance

### 3.1 Set SSH Key Permissions (Mac/Linux)

```bash
# Navigate to where you saved the key
cd ~/Downloads

# Set proper permissions (required for SSH)
chmod 400 fulus-ec2-key.pem

# Move to a secure location
mkdir -p ~/.ssh
mv fulus-ec2-key.pem ~/.ssh/
```

### 3.2 Connect via SSH

```bash
# Replace YOUR-EC2-PUBLIC-IP with your actual IP address
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@YOUR-EC2-PUBLIC-IP
```

**Example:**
```bash
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@54.123.45.67
```

### 3.3 For Windows Users

**Using PuTTY:**
1. Open PuTTYgen
2. Load your `.pem` file
3. Save as `.ppk` format
4. Open PuTTY
5. Enter `ubuntu@YOUR-EC2-PUBLIC-IP` as hostname
6. Under Connection → SSH → Auth, browse to your `.ppk` file
7. Click Open

### 3.4 First Connection

On first connection, you'll see:
```
The authenticity of host '54.123.45.67 (54.123.45.67)' can't be established.
Are you sure you want to continue connecting (yes/no)?
```

Type `yes` and press Enter.

You should see the Ubuntu welcome screen! 🎉

---

## Step 4: Initial Server Setup

Now that you're connected to your EC2 instance, let's set it up.

### 4.1 Update System Packages

```bash
# Update package list
sudo apt-get update

# Upgrade installed packages
sudo apt-get upgrade -y

# This may take 5-10 minutes
```

### 4.2 Install Essential Tools

```bash
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    unzip \
    vim \
    htop \
    net-tools \
    jq \
    software-properties-common
```

### 4.3 Install Docker

```bash
# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Verify installation
docker --version
# Expected: Docker version 24.x.x, build xxxxx
```

### 4.4 Install Docker Compose (Standalone)

```bash
# Install latest version
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.1/docker-compose-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/docker-compose

# Make executable
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker-compose --version
# Expected: Docker Compose version v2.24.1
```

### 4.5 Add User to Docker Group

```bash
# Add ubuntu user to docker group
sudo usermod -aG docker ubuntu

# Apply group changes (logout and login again)
# Method 1: Reconnect SSH session
exit
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@YOUR-EC2-PUBLIC-IP

# Method 2: Apply group without logout
newgrp docker

# Verify (should work without sudo)
docker ps
```

### 4.6 Configure Docker Daemon

```bash
# Create Docker daemon configuration
sudo tee /etc/docker/daemon.json > /dev/null <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "dns": ["8.8.8.8", "8.8.4.4"]
}
EOF

# Restart Docker to apply changes
sudo systemctl restart docker
sudo systemctl enable docker
```

### 4.7 Install Java 17 (Optional - for non-Docker deployment)

> **Skip this if using Docker** (Docker already includes Java)

```bash
# Install OpenJDK 17
sudo apt-get install -y openjdk-17-jdk

# Verify installation
java -version
# Expected: openjdk version "17.x.x"
```

### 4.8 Configure Firewall (UFW)

```bash
# Enable UFW
sudo ufw --force enable

# Set default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (IMPORTANT: Do this first!)
sudo ufw allow ssh
sudo ufw allow 22/tcp

# Allow HTTP & HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow application port
sudo ufw allow 8080/tcp

# Verify rules
sudo ufw status verbose

# Expected output:
# Status: active
# To                         Action      From
# --                         ------      ----
# 22/tcp                     ALLOW       Anywhere
# 80/tcp                     ALLOW       Anywhere
# 443/tcp                    ALLOW       Anywhere
# 8080/tcp                   ALLOW       Anywhere
```

### 4.9 Setup Swap Space (4GB)

Swap prevents out-of-memory errors for memory-intensive operations.

```bash
# Check current swap
free -h

# Create 4GB swap file
sudo fallocate -l 4G /swapfile

# Set proper permissions
sudo chmod 600 /swapfile

# Make it a swap file
sudo mkswap /swapfile

# Enable swap
sudo swapon /swapfile

# Verify swap is active
free -h
# Should show 4.0Gi in Swap row

# Make swap permanent (persist after reboot)
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Optimize swap usage (swappiness = 10)
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
```

### 4.10 Set Timezone

```bash
# Set to Nigeria timezone (or your preferred timezone)
sudo timedatectl set-timezone Africa/Lagos

# Verify
timedatectl
# Should show: Time zone: Africa/Lagos (WAT, +0100)
```

### 4.11 Configure System Limits

```bash
# Increase file descriptor limits
sudo tee -a /etc/security/limits.conf > /dev/null <<'EOF'

# Fulus Pay AI Assistant limits
ubuntu soft nofile 65536
ubuntu hard nofile 65536
EOF

# Apply network optimizations
sudo tee -a /etc/sysctl.conf > /dev/null <<'EOF'

# Fulus Pay AI Assistant optimizations
vm.swappiness=10
net.core.somaxconn=1024
net.ipv4.tcp_max_syn_backlog=2048
net.ipv4.ip_local_port_range=10000 65535
EOF

# Apply changes
sudo sysctl -p
```

### 4.12 Setup Automatic Security Updates

```bash
# Install unattended-upgrades
sudo apt-get install -y unattended-upgrades

# Configure automatic updates
sudo dpkg-reconfigure -plow unattended-upgrades
# Select "Yes" when prompted

# Verify configuration
cat /etc/apt/apt.conf.d/20auto-upgrades
# Should show:
# APT::Periodic::Update-Package-Lists "1";
# APT::Periodic::Unattended-Upgrade "1";
```

---

## Step 5: Deploy Application

### 5.1 Clone Repository

```bash
# Navigate to home directory
cd ~

# Clone the repository
git clone https://github.com/yourusername/fulus-pay-ai-assistant.git

# Or download setup script if repo is not ready
wget https://raw.githubusercontent.com/yourusername/fulus-pay-ai-assistant/main/setup-ec2.sh
chmod +x setup-ec2.sh
./setup-ec2.sh
```

### 5.2 Navigate to Project Directory

```bash
cd ~/fulus-pay-ai-assistant
ls -la
# You should see: docker-compose.yml, Dockerfile, pom.xml, etc.
```

### 5.3 Create Environment File

```bash
# Create .env file from template
cp .env.template .env

# Edit .env file
nano .env
```

**Required Configuration:**

```bash
# ===========================================
# Database Configuration
# ===========================================
DB_NAME=fulus_ai_db
DB_USERNAME=fulus_user
DB_PASSWORD=YourStrongDatabasePassword123!

# ===========================================
# OpenAI API Configuration
# ===========================================
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxx
OPENAI_MODEL=gpt-4-turbo

# ===========================================
# JWT Security Configuration
# ===========================================
# Generate with: openssl rand -base64 32
JWT_SECRET=abcdef1234567890abcdef1234567890abcdefghijklmnopqrstuvwxyz
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# ===========================================
# Application Profile
# ===========================================
SPRING_PROFILE=prod
```

**Generate Strong JWT Secret:**
```bash
openssl rand -base64 32
```

Copy the output and paste it as your `JWT_SECRET`.

**Save and Exit:**
- Press `Ctrl + X`
- Press `Y` to confirm
- Press `Enter` to save

### 5.4 Secure Environment File

```bash
# Set restrictive permissions
chmod 600 .env

# Verify
ls -la .env
# Should show: -rw------- (only owner can read/write)
```

### 5.5 Build and Start Application

```bash
# Build Docker images (first time - takes 5-10 minutes)
docker-compose -f docker-compose.prod.yml build

# Start services
docker-compose -f docker-compose.prod.yml up -d

# Check container status
docker-compose -f docker-compose.prod.yml ps

# Expected output:
# NAME                  IMAGE                         STATUS
# fulus-app-prod        fulus-pay-ai-assistant:latest Up
# fulus-postgres-prod   postgres:15-alpine            Up (healthy)
```

### 5.6 View Logs

```bash
# Follow all logs
docker-compose -f docker-compose.prod.yml logs -f

# View app logs only
docker-compose -f docker-compose.prod.yml logs -f app

# View last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100

# Press Ctrl+C to stop following logs
```

Wait for the message: `Started FulusPayAiAssistantApplication in X.XXX seconds`

### 5.7 Test Application

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# Test from outside EC2 (from your local machine)
curl http://YOUR-EC2-PUBLIC-IP:8080/actuator/health
```

### 5.8 Setup Systemd Service (Auto-start on Reboot)

```bash
# Create systemd service file
sudo tee /etc/systemd/system/fulus-pay.service > /dev/null <<'EOF'
[Unit]
Description=Fulus Pay AI Assistant Docker Compose Service
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ubuntu/fulus-pay-ai-assistant
User=ubuntu
Group=ubuntu
EnvironmentFile=/home/ubuntu/fulus-pay-ai-assistant/.env
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
Restart=on-failure
RestartSec=10s
StandardOutput=journal
StandardError=journal
SyslogIdentifier=fulus-pay
NoNewPrivileges=yes
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd
sudo systemctl daemon-reload

# Enable auto-start on boot
sudo systemctl enable fulus-pay.service

# Check status
sudo systemctl status fulus-pay.service
```

**Test Auto-start:**
```bash
# Stop containers
docker-compose -f docker-compose.prod.yml down

# Start via systemd
sudo systemctl start fulus-pay

# Verify
docker ps
```

### 5.9 Setup Database Backup Script

```bash
# Create backup script
cat > ~/fulus-pay-ai-assistant/backup-database.sh <<'EOF'
#!/bin/bash
BACKUP_DIR="$HOME/fulus-pay-ai-assistant/postgres-backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_$TIMESTAMP.sql.gz"

mkdir -p "$BACKUP_DIR"

docker exec fulus-postgres-prod pg_dump -U fulus_user fulus_ai_db | gzip > "$BACKUP_FILE"

# Keep only last 7 days of backups
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
EOF

# Make executable
chmod +x ~/fulus-pay-ai-assistant/backup-database.sh

# Test backup
~/fulus-pay-ai-assistant/backup-database.sh

# Schedule daily backups (2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * $HOME/fulus-pay-ai-assistant/backup-database.sh") | crontab -

# Verify cron job
crontab -l
```

---

## Step 6: Configure Domain & SSL (Optional)

### 6.1 Point Domain to EC2

In your domain registrar (GoDaddy, Namecheap, etc.):

1. Add an **A Record**:
   - **Host:** `@` (or `api` for subdomain)
   - **Value:** Your EC2 Elastic IP
   - **TTL:** 3600

2. Wait 5-10 minutes for DNS propagation

3. Test DNS resolution:
   ```bash
   nslookup yourdomain.com
   dig yourdomain.com
   ```

### 6.2 Install Nginx (Reverse Proxy)

```bash
# Install Nginx
sudo apt-get install -y nginx

# Start and enable Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Verify
curl http://localhost:80
# Should show "Welcome to nginx!"
```

### 6.3 Configure Nginx Reverse Proxy

```bash
# Create Nginx configuration
sudo tee /etc/nginx/sites-available/fulus-pay > /dev/null <<'EOF'
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    # Increase buffer sizes for large requests
    client_max_body_size 10M;
    proxy_buffers 16 16k;
    proxy_buffer_size 16k;

    # Application proxy
    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;

        # Timeouts for AI requests
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
EOF

# Replace 'yourdomain.com' with your actual domain
sudo sed -i 's/yourdomain.com/your-actual-domain.com/g' /etc/nginx/sites-available/fulus-pay

# Enable site
sudo ln -s /etc/nginx/sites-available/fulus-pay /etc/nginx/sites-enabled/

# Remove default site
sudo rm /etc/nginx/sites-enabled/default

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

### 6.4 Install SSL with Let's Encrypt

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Obtain SSL certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Follow prompts:
# 1. Enter email address
# 2. Agree to Terms of Service (Y)
# 3. Share email with EFF (Y/N - your choice)
# 4. Redirect HTTP to HTTPS (Select 2)

# Test auto-renewal
sudo certbot renew --dry-run

# Certificate will auto-renew via cron
```

### 6.5 Update Firewall for Nginx

```bash
# Allow Nginx through UFW
sudo ufw allow 'Nginx Full'

# Optionally remove direct access to 8080
sudo ufw delete allow 8080/tcp

# Verify
sudo ufw status
```

### 6.6 Test HTTPS

```bash
# From your local machine
curl https://yourdomain.com/actuator/health

# Should return: {"status":"UP"}
```

---

## Step 7: Setup Monitoring

### 7.1 Install CloudWatch Agent

```bash
# Download CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb

# Install
sudo dpkg -i amazon-cloudwatch-agent.deb

# Clean up
rm amazon-cloudwatch-agent.deb
```

### 7.2 Create IAM Role for CloudWatch

In AWS Console:

1. Go to **IAM** → **Roles**
2. Click **Create role**
3. **Trusted entity type:** AWS service
4. **Use case:** EC2
5. Click **Next**
6. **Permissions policies:**
   - `CloudWatchAgentServerPolicy`
   - `AmazonSSMManagedInstanceCore`
7. **Role name:** `EC2-CloudWatch-Role`
8. Click **Create role**

### 7.3 Attach IAM Role to EC2

1. Go to **EC2 Dashboard**
2. Select your instance
3. **Actions** → **Security** → **Modify IAM role**
4. **IAM role:** Select `EC2-CloudWatch-Role`
5. Click **Update IAM role**

### 7.4 Configure CloudWatch Agent

```bash
# Create configuration file
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/config.json > /dev/null <<'EOF'
{
  "metrics": {
    "namespace": "FulusPayAI",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          {
            "name": "cpu_usage_idle",
            "rename": "CPU_IDLE",
            "unit": "Percent"
          },
          "cpu_usage_iowait"
        ],
        "metrics_collection_interval": 60,
        "totalcpu": false
      },
      "disk": {
        "measurement": [
          {
            "name": "used_percent",
            "rename": "DISK_USED",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "diskio": {
        "measurement": ["io_time"],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "mem": {
        "measurement": [
          {
            "name": "mem_used_percent",
            "rename": "MEM_USED",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60
      },
      "swap": {
        "measurement": [
          {
            "name": "swap_used_percent",
            "rename": "SWAP_USED",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60
      }
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/home/ubuntu/fulus-pay-ai-assistant/logs/*.log",
            "log_group_name": "/fulus-pay/application",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/nginx/access.log",
            "log_group_name": "/fulus-pay/nginx-access",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/nginx/error.log",
            "log_group_name": "/fulus-pay/nginx-error",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  }
}
EOF

# Start CloudWatch agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -s \
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json

# Verify agent is running
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a query \
    -m ec2 \
    -c default
```

### 7.5 Create CloudWatch Alarms

In AWS Console:

1. Go to **CloudWatch** → **Alarms**
2. Click **Create alarm**

**CPU Alarm:**
- **Metric:** EC2 → Per-Instance Metrics → CPUUtilization
- **Statistic:** Average
- **Period:** 5 minutes
- **Threshold:** > 80% for 2 consecutive periods
- **Action:** Send SNS notification

**Memory Alarm:**
- **Metric:** FulusPayAI → MEM_USED
- **Threshold:** > 85%

**Disk Alarm:**
- **Metric:** FulusPayAI → DISK_USED
- **Threshold:** > 80%

### 7.6 Setup Application Monitoring

```bash
# Create monitoring script
cat > ~/monitor.sh <<'EOF'
#!/bin/bash
echo "=== System Resources ==="
free -h
echo ""
df -h
echo ""
echo "=== Docker Containers ==="
docker ps
echo ""
echo "=== Application Health ==="
curl -s http://localhost:8080/actuator/health | jq .
echo ""
echo "=== Recent Logs ==="
docker logs --tail=20 fulus-app-prod
EOF

chmod +x ~/monitor.sh

# Run monitoring script
~/monitor.sh
```

---

## Step 8: Verify Deployment

### 8.1 Health Check

```bash
# Test health endpoint
curl http://YOUR-EC2-IP:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 8.2 Test User Registration

```bash
curl -X POST http://YOUR-EC2-IP:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "fullName": "Test User",
    "email": "test@example.com",
    "pin": "1234",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }'
```

### 8.3 Test AI Chat

```bash
# First, login to get token
TOKEN=$(curl -s -X POST http://YOUR-EC2-IP:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"08012345678","pin":"1234"}' \
  | jq -r '.accessToken')

# Test AI chat
curl -X POST http://YOUR-EC2-IP:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": "USER-ID-HERE",
    "message": "Hello, what can you help me with?"
  }'
```

### 8.4 Check Container Status

```bash
# View running containers
docker ps

# Check logs for errors
docker-compose -f docker-compose.prod.yml logs --tail=100 | grep -i error

# Check resource usage
docker stats --no-stream
```

### 8.5 Verify Auto-Start

```bash
# Reboot instance
sudo reboot

# Wait 2 minutes, then reconnect
ssh -i ~/.ssh/fulus-ec2-key.pem ubuntu@YOUR-EC2-IP

# Check if containers started automatically
docker ps

# Should show both containers running
```

---

## Maintenance & Updates

### Daily Tasks

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check disk space
df -h

# Check memory usage
free -h

# View recent logs
docker-compose -f docker-compose.prod.yml logs --tail=50
```

### Weekly Tasks

```bash
# Check for security updates
sudo apt-get update
sudo apt-get upgrade -y

# Check backup files
ls -lh ~/fulus-pay-ai-assistant/postgres-backup/

# Review CloudWatch metrics in AWS Console
```

### Deploying Updates

**Method 1: Using Deployment Script (Recommended)**

From your local machine:
```bash
export EC2_HOST="your-ec2-ip"
export EC2_KEY="~/.ssh/fulus-ec2-key.pem"
./deploy-to-ec2.sh production
```

**Method 2: Manual Update**

On EC2 instance:
```bash
cd ~/fulus-pay-ai-assistant

# Pull latest code
git pull origin main

# Rebuild and restart
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml build --no-cache
docker-compose -f docker-compose.prod.yml up -d

# Verify
curl http://localhost:8080/actuator/health
```

### Troubleshooting Commands

```bash
# View all logs
docker-compose -f docker-compose.prod.yml logs

# Restart application
sudo systemctl restart fulus-pay

# Check systemd status
sudo systemctl status fulus-pay

# Check disk usage
du -sh ~/fulus-pay-ai-assistant/*

# Clean up Docker
docker system prune -a --volumes
```

---

## Cost Estimation

### Monthly AWS Costs (US East - Ohio)

| Resource | Type | Monthly Cost |
|----------|------|--------------|
| EC2 Instance | t3.medium | $30.37 |
| EBS Storage | 30GB gp3 | $2.40 |
| Elastic IP | 1 IP | $0.00 (if attached) |
| Data Transfer | 100GB out | $9.00 |
| CloudWatch | Detailed monitoring | $2.10 |
| **Total** | | **~$43.87/month** |

### Additional Costs (External Services)

| Service | Estimated Monthly Cost |
|---------|----------------------|
| OpenAI API | $20-100 (varies by usage) |
| Domain Name | $10-15/year |
| SSL Certificate | $0 (Let's Encrypt) |
| **Total Additional** | **~$22-102/month** |

### Cost Optimization Tips

1. **Use Reserved Instances** - Save up to 40% with 1-year commitment
2. **Stop instance during development** - Only pay when running
3. **Use Spot Instances** - Save up to 90% for non-critical workloads
4. **Monitor OpenAI usage** - Set API usage limits
5. **Enable AWS Cost Explorer** - Track spending patterns

---

## Security Checklist

- ✅ SSH key has 400 permissions
- ✅ Security group restricts SSH to your IP
- ✅ UFW firewall is enabled
- ✅ Strong passwords in .env file
- ✅ .env file has 600 permissions
- ✅ Automatic security updates enabled
- ✅ SSL/TLS certificate installed
- ✅ CloudWatch monitoring configured
- ✅ Daily database backups scheduled
- ✅ Docker containers run as non-root user

---

## Support & Resources

### Official Documentation
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Docker Documentation](https://docs.docker.com/)
- [AWS EC2 User Guide](https://docs.aws.amazon.com/ec2/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Useful Commands Reference

```bash
# System
sudo systemctl status fulus-pay
sudo journalctl -u fulus-pay -f
htop
df -h
free -h

# Docker
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f
docker-compose -f docker-compose.prod.yml restart
docker stats

# Database
docker exec -it fulus-postgres-prod psql -U fulus_user -d fulus_ai_db
./backup-database.sh

# Nginx
sudo systemctl status nginx
sudo nginx -t
sudo tail -f /var/log/nginx/error.log
```

---

## What's Next?

After successful deployment:

1. **Setup monitoring alerts** - Configure SNS notifications
2. **Configure backup retention** - Adjust backup scripts for your needs
3. **Setup CI/CD pipeline** - Automate deployments with GitHub Actions
4. **Add load balancer** - For high availability (AWS ALB)
5. **Setup staging environment** - Separate instance for testing
6. **Configure logging** - Centralized logging with AWS CloudWatch Logs
7. **Add API rate limiting** - Protect against abuse
8. **Setup database replication** - For high availability

---

## Conclusion

You've successfully deployed Fulus Pay AI Assistant on AWS EC2! 🎉

Your application is now running with:
- ✅ Docker containerization
- ✅ PostgreSQL database
- ✅ SSL/TLS encryption
- ✅ Auto-start on reboot
- ✅ Automated backups
- ✅ CloudWatch monitoring
- ✅ Production-ready configuration

For issues or questions, refer to the [main README](./README.md) or [DEPLOYMENT.md](./DEPLOYMENT.md).

---

**Last Updated:** 2024-12-11
**Version:** 1.0.0
