#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# Fulus Pay AI Assistant - EC2 Initial Setup Script
# ═══════════════════════════════════════════════════════════════════════════════
# This script prepares a fresh Ubuntu 22.04 EC2 instance for deployment
# Run this script once on the EC2 instance to install dependencies
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Configuration
APP_NAME="fulus-pay-ai-assistant"
APP_DIR="/home/ubuntu/$APP_NAME"
GIT_REPO="${GIT_REPO:-https://github.com/yourusername/fulus-pay-ai-assistant.git}"
GIT_BRANCH="${GIT_BRANCH:-main}"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Fulus Pay AI Assistant - EC2 Initial Setup"
echo "  Ubuntu 22.04 LTS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Update system packages
log_info "Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq
log_success "System packages updated"

# Install essential tools
log_info "Installing essential tools..."
sudo apt-get install -y -qq \
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
    jq
log_success "Essential tools installed"

# Install Docker
log_info "Installing Docker..."
if ! command -v docker &> /dev/null; then
    # Add Docker's official GPG key
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Add Docker repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker Engine
    sudo apt-get update -qq
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Add ubuntu user to docker group
    sudo usermod -aG docker ubuntu

    log_success "Docker installed successfully"
else
    log_success "Docker is already installed"
fi

# Install Docker Compose (standalone)
log_info "Installing Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_VERSION="v2.24.1"
    sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" \
        -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    log_success "Docker Compose installed successfully"
else
    log_success "Docker Compose is already installed"
fi

# Verify Docker installation
log_info "Verifying Docker installation..."
docker --version
docker-compose --version
log_success "Docker verification completed"

# Configure Docker daemon for production
log_info "Configuring Docker daemon..."
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
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

sudo systemctl restart docker
log_success "Docker daemon configured"

# Setup firewall (UFW)
log_info "Configuring firewall..."
sudo ufw --force enable
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 8080/tcp  # Application port
sudo ufw allow 5432/tcp  # PostgreSQL (only if exposing externally)
sudo ufw status
log_success "Firewall configured"

# Create application directory
log_info "Creating application directory..."
mkdir -p "$APP_DIR"
cd "$APP_DIR"
log_success "Application directory created at $APP_DIR"

# Clone repository if git repo is provided
if [ -n "$GIT_REPO" ] && [ "$GIT_REPO" != "https://github.com/yourusername/fulus-pay-ai-assistant.git" ]; then
    log_info "Cloning repository from $GIT_REPO..."
    git clone -b "$GIT_BRANCH" "$GIT_REPO" .
    log_success "Repository cloned"
else
    log_warning "Git repository not configured. Please clone manually or update GIT_REPO variable."
fi

# Create .env file template
log_info "Creating .env file template..."
if [ ! -f "$APP_DIR/.env" ]; then
    cat > "$APP_DIR/.env" <<'EOF'
# Database Configuration
DB_NAME=fulus_ai_db
DB_USERNAME=fulus_user
DB_PASSWORD=CHANGE_ME_STRONG_PASSWORD

# OpenAI Configuration
OPENAI_API_KEY=sk-your-openai-api-key-here
OPENAI_MODEL=gpt-4-turbo

# JWT Configuration
JWT_SECRET=CHANGE_ME_GENERATE_STRONG_SECRET_32_CHARS_MIN
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Environment
SPRING_PROFILE=prod
EOF
    chmod 600 "$APP_DIR/.env"
    log_warning "Please update $APP_DIR/.env with actual credentials!"
else
    log_success ".env file already exists"
fi

# Create directories for volumes
log_info "Creating volume directories..."
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/audio_uploads"
mkdir -p "$APP_DIR/audio_responses"
mkdir -p "$APP_DIR/backups"
mkdir -p "$APP_DIR/postgres-backup"
log_success "Volume directories created"

# Setup systemd service
log_info "Setting up systemd service..."
sudo cp "$APP_DIR/fulus-pay.service" /etc/systemd/system/fulus-pay.service 2>/dev/null || {
    log_warning "fulus-pay.service file not found. You'll need to set it up manually."
}

if [ -f "/etc/systemd/system/fulus-pay.service" ]; then
    # Update WorkingDirectory in service file
    sudo sed -i "s|WorkingDirectory=.*|WorkingDirectory=$APP_DIR|g" /etc/systemd/system/fulus-pay.service
    sudo sed -i "s|EnvironmentFile=.*|EnvironmentFile=$APP_DIR/.env|g" /etc/systemd/system/fulus-pay.service

    sudo systemctl daemon-reload
    sudo systemctl enable fulus-pay.service
    log_success "Systemd service configured and enabled"
else
    log_warning "Systemd service setup skipped"
fi

# Setup log rotation
log_info "Setting up log rotation..."
sudo tee /etc/logrotate.d/fulus-pay > /dev/null <<EOF
$APP_DIR/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 ubuntu ubuntu
    sharedscripts
    postrotate
        docker-compose -f $APP_DIR/docker-compose.prod.yml restart app > /dev/null 2>&1 || true
    endscript
}
EOF
log_success "Log rotation configured"

# Setup automatic security updates
log_info "Configuring automatic security updates..."
sudo apt-get install -y -qq unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
log_success "Automatic security updates enabled"

# Install CloudWatch agent (optional for AWS monitoring)
log_info "Installing CloudWatch agent (optional)..."
if [ -f /etc/ec2-instance-metadata ]; then
    wget -q https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
    sudo dpkg -i -E amazon-cloudwatch-agent.deb
    rm amazon-cloudwatch-agent.deb
    log_success "CloudWatch agent installed"
else
    log_warning "Not running on EC2, skipping CloudWatch agent"
fi

# Create backup script
log_info "Creating backup script..."
cat > "$APP_DIR/backup-database.sh" <<'BACKUP_EOF'
#!/bin/bash
BACKUP_DIR="/home/ubuntu/fulus-pay-ai-assistant/postgres-backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_$TIMESTAMP.sql.gz"

mkdir -p "$BACKUP_DIR"

docker exec fulus-postgres-prod pg_dump -U fulus_user fulus_ai_db | gzip > "$BACKUP_FILE"

# Keep only last 7 days of backups
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
BACKUP_EOF

chmod +x "$APP_DIR/backup-database.sh"

# Add daily backup cron job
(crontab -l 2>/dev/null || true; echo "0 2 * * * $APP_DIR/backup-database.sh") | crontab -
log_success "Database backup script created and scheduled"

# System optimization
log_info "Applying system optimizations..."
sudo tee -a /etc/sysctl.conf > /dev/null <<EOF

# Fulus Pay AI Assistant optimizations
vm.swappiness=10
net.core.somaxconn=1024
net.ipv4.tcp_max_syn_backlog=2048
EOF

sudo sysctl -p > /dev/null
log_success "System optimizations applied"

# Display summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log_success "EC2 Setup Completed Successfully! 🚀"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next Steps:"
echo "1. Update credentials in: $APP_DIR/.env"
echo "2. Review configuration files"
echo "3. Run deployment: ./deploy-to-ec2.sh"
echo ""
echo "Useful Commands:"
echo "  Start:   sudo systemctl start fulus-pay"
echo "  Stop:    sudo systemctl stop fulus-pay"
echo "  Status:  sudo systemctl status fulus-pay"
echo "  Logs:    docker-compose -f docker-compose.prod.yml logs -f"
echo "  Backup:  ./backup-database.sh"
echo ""
echo "Application will be available at: http://YOUR-EC2-PUBLIC-IP:8080"
echo ""
log_warning "IMPORTANT: Update the .env file with production credentials!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
