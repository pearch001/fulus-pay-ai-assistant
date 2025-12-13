#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# Fulus Pay AI Assistant - EC2 Deployment Script
# ═══════════════════════════════════════════════════════════════════════════════
# This script automates deployment to AWS EC2 Ubuntu 22.04
# Usage: ./deploy-to-ec2.sh [environment]
# Example: ./deploy-to-ec2.sh production
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail  # Exit on error, undefined variable, or pipe failure

# ═══════════════════════════════════════════════════════════════════════════════
# Configuration
# ═══════════════════════════════════════════════════════════════════════════════

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Environment (default: production)
# Options: production, free-tier, development
ENVIRONMENT="${1:-production}"

# EC2 Configuration (set these via environment variables or update here)
EC2_HOST="${EC2_HOST:-}"
EC2_USER="${EC2_USER:-ubuntu}"
EC2_KEY="${EC2_KEY:-~/.ssh/fulus-ec2-key.pem}"
EC2_PORT="${EC2_PORT:-22}"

# Application Configuration
APP_NAME="fulus-pay-ai-assistant"
APP_DIR="/home/$EC2_USER/$APP_NAME"
GIT_REPO="${GIT_REPO:-https://github.com/yourusername/fulus-pay-ai-assistant.git}"
GIT_BRANCH="${GIT_BRANCH:-main}"

# Docker Configuration - Select based on environment
case "$ENVIRONMENT" in
    free-tier)
        DOCKER_COMPOSE_FILE="docker-compose.free-tier.yml"
        ;;
    production)
        DOCKER_COMPOSE_FILE="docker-compose.prod.yml"
        ;;
    *)
        DOCKER_COMPOSE_FILE="docker-compose.yml"
        ;;
esac

BACKUP_DIR="$APP_DIR/backups"

# Health Check Configuration
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
# Adjust timeout for free tier (slower startup)
if [ "$ENVIRONMENT" = "free-tier" ]; then
    HEALTH_CHECK_TIMEOUT=300  # 5 minutes for free tier
    HEALTH_CHECK_INTERVAL=10
else
    HEALTH_CHECK_TIMEOUT=120  # 2 minutes for production
    HEALTH_CHECK_INTERVAL=5
fi

# ═══════════════════════════════════════════════════════════════════════════════
# Helper Functions
# ═══════════════════════════════════════════════════════════════════════════════

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate prerequisites
validate_prerequisites() {
    log_info "Validating prerequisites..."

    # Check if EC2_HOST is set
    if [ -z "$EC2_HOST" ]; then
        log_error "EC2_HOST is not set. Please set it via environment variable or update the script."
        log_info "Example: export EC2_HOST='ec2-xx-xx-xx-xx.compute.amazonaws.com'"
        exit 1
    fi

    # Check if SSH key exists
    if [ ! -f "$EC2_KEY" ]; then
        log_error "SSH key not found at $EC2_KEY"
        exit 1
    fi

    # Check SSH key permissions
    KEY_PERMS=$(stat -f "%A" "$EC2_KEY" 2>/dev/null || stat -c "%a" "$EC2_KEY" 2>/dev/null)
    if [ "$KEY_PERMS" != "400" ] && [ "$KEY_PERMS" != "600" ]; then
        log_warning "SSH key permissions are $KEY_PERMS. Setting to 400..."
        chmod 400 "$EC2_KEY"
    fi

    # Check if SSH connection works
    log_info "Testing SSH connection to $EC2_HOST..."
    if ! ssh -i "$EC2_KEY" -p "$EC2_PORT" -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST" "echo 'SSH connection successful'" &>/dev/null; then
        log_error "Cannot connect to EC2 instance via SSH"
        exit 1
    fi

    log_success "Prerequisites validated"
}

# Execute command on EC2 instance
ssh_exec() {
    ssh -i "$EC2_KEY" -p "$EC2_PORT" -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST" "$@"
}

# Create backup of current deployment
create_backup() {
    log_info "Creating backup of current deployment..."

    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_NAME="backup_${TIMESTAMP}"

    ssh_exec << EOF
        set -e
        mkdir -p $BACKUP_DIR

        # Backup docker-compose file and .env if they exist
        if [ -d "$APP_DIR" ]; then
            cd $APP_DIR

            # Create backup directory
            mkdir -p $BACKUP_DIR/$BACKUP_NAME

            # Backup docker-compose file
            if [ -f "$DOCKER_COMPOSE_FILE" ]; then
                cp $DOCKER_COMPOSE_FILE $BACKUP_DIR/$BACKUP_NAME/
            fi

            # Backup .env file
            if [ -f .env ]; then
                cp .env $BACKUP_DIR/$BACKUP_NAME/
            fi

            # Export current container IDs
            docker-compose ps -q > $BACKUP_DIR/$BACKUP_NAME/container_ids.txt 2>/dev/null || true

            echo "$BACKUP_NAME" > $BACKUP_DIR/latest_backup.txt

            # Keep only last 5 backups
            cd $BACKUP_DIR
            ls -t | tail -n +6 | xargs -r rm -rf
        fi
EOF

    log_success "Backup created: $BACKUP_NAME"
    echo "$BACKUP_NAME"
}

# Pull latest code from repository
pull_latest_code() {
    log_info "Pulling latest code from repository..."

    ssh_exec << EOF
        set -e

        # Create app directory if it doesn't exist
        mkdir -p $APP_DIR
        cd $APP_DIR

        # Clone repository if it doesn't exist
        if [ ! -d ".git" ]; then
            echo "Cloning repository..."
            git clone -b $GIT_BRANCH $GIT_REPO .
        else
            echo "Fetching latest changes..."
            git fetch origin
            git checkout $GIT_BRANCH
            git pull origin $GIT_BRANCH
        fi

        # Show current commit
        echo "Current commit: \$(git log -1 --oneline)"
EOF

    log_success "Code pulled successfully"
}

# Stop existing containers
stop_containers() {
    log_info "Stopping existing containers..."

    ssh_exec << EOF
        set -e
        cd $APP_DIR

        if [ -f "$DOCKER_COMPOSE_FILE" ]; then
            docker-compose -f $DOCKER_COMPOSE_FILE down || true
        fi

        # Stop any running containers with app name
        docker ps -q --filter "name=$APP_NAME" | xargs -r docker stop || true
EOF

    log_success "Containers stopped"
}

# Build Docker images
build_images() {
    log_info "Building Docker images..."

    ssh_exec << EOF
        set -e
        cd $APP_DIR

        # Build with no cache to ensure fresh build
        docker-compose -f $DOCKER_COMPOSE_FILE build --no-cache

        # Clean up dangling images
        docker image prune -f
EOF

    log_success "Docker images built"
}

# Start containers
start_containers() {
    log_info "Starting containers..."

    ssh_exec << EOF
        set -e
        cd $APP_DIR

        # Start containers in detached mode
        docker-compose -f $DOCKER_COMPOSE_FILE up -d

        # Show running containers
        docker-compose -f $DOCKER_COMPOSE_FILE ps
EOF

    log_success "Containers started"
}

# Wait for application to be healthy
wait_for_health() {
    log_info "Waiting for application to be healthy (timeout: ${HEALTH_CHECK_TIMEOUT}s)..."

    if [ "$ENVIRONMENT" = "free-tier" ]; then
        log_warning "Free tier detected - startup may take 3-5 minutes..."
    fi

    local elapsed=0
    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        if ssh_exec "curl -f $HEALTH_CHECK_URL -s > /dev/null 2>&1"; then
            log_success "Application is healthy (took ${elapsed}s)"
            return 0
        fi

        echo -n "."
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    done

    echo ""
    log_error "Health check failed after ${HEALTH_CHECK_TIMEOUT}s"
    return 1
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."

    # Check health endpoint
    local health_response
    health_response=$(ssh_exec "curl -s $HEALTH_CHECK_URL" || echo '{"status":"DOWN"}')

    if echo "$health_response" | grep -q '"status":"UP"'; then
        log_success "Health check passed"
    else
        log_error "Health check failed: $health_response"
        return 1
    fi

    # Check if containers are running
    local running_containers
    running_containers=$(ssh_exec "cd $APP_DIR && docker-compose -f $DOCKER_COMPOSE_FILE ps --services --filter 'status=running' | wc -l")

    if [ "$running_containers" -ge 2 ]; then
        log_success "All containers are running"
    else
        log_error "Not all containers are running"
        return 1
    fi

    # Check application logs for errors
    log_info "Checking recent logs..."
    ssh_exec "cd $APP_DIR && docker-compose -f $DOCKER_COMPOSE_FILE logs --tail=50 app" || true

    return 0
}

# Rollback to previous version
rollback() {
    log_warning "Rolling back to previous version..."

    local latest_backup
    latest_backup=$(ssh_exec "cat $BACKUP_DIR/latest_backup.txt" || echo "")

    if [ -z "$latest_backup" ]; then
        log_error "No backup found for rollback"
        return 1
    fi

    ssh_exec << EOF
        set -e
        cd $APP_DIR

        # Stop current containers
        docker-compose -f $DOCKER_COMPOSE_FILE down || true

        # Restore backup
        if [ -f "$BACKUP_DIR/$latest_backup/$DOCKER_COMPOSE_FILE" ]; then
            cp $BACKUP_DIR/$latest_backup/$DOCKER_COMPOSE_FILE .
        fi

        if [ -f "$BACKUP_DIR/$latest_backup/.env" ]; then
            cp $BACKUP_DIR/$latest_backup/.env .
        fi

        # Start previous containers
        docker-compose -f $DOCKER_COMPOSE_FILE up -d
EOF

    log_success "Rollback completed"
}

# Show deployment summary
show_summary() {
    log_info "Deployment Summary:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    ssh_exec << EOF
        cd $APP_DIR
        echo "Environment: $ENVIRONMENT"
        echo "Docker Compose File: $DOCKER_COMPOSE_FILE"
        echo "Git Branch: \$(git rev-parse --abbrev-ref HEAD)"
        echo "Git Commit: \$(git log -1 --oneline)"
        echo ""
        echo "Running Containers:"
        docker-compose -f $DOCKER_COMPOSE_FILE ps
        echo ""
        echo "Container Resource Usage:"
        docker stats --no-stream
        echo ""
        echo "Disk Usage:"
        df -h $APP_DIR | tail -n 1
        echo ""
        echo "Memory Usage:"
        free -h | grep -E "Mem:|Swap:"
EOF

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# ═══════════════════════════════════════════════════════════════════════════════
# Main Deployment Flow
# ═══════════════════════════════════════════════════════════════════════════════

main() {
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Fulus Pay AI Assistant - EC2 Deployment"
    echo "  Environment: $ENVIRONMENT"
    echo "  Docker Compose: $DOCKER_COMPOSE_FILE"
    echo "  Target: $EC2_HOST"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Show free tier warning
    if [ "$ENVIRONMENT" = "free-tier" ]; then
        log_warning "Deploying to FREE TIER instance"
        log_warning "Expected startup time: 3-5 minutes"
        log_warning "Recommended for development/testing only"
        echo ""
    fi

    # Validate prerequisites
    validate_prerequisites

    # Create backup
    BACKUP_NAME=$(create_backup)

    # Deployment steps
    if pull_latest_code && \
       stop_containers && \
       build_images && \
       start_containers && \
       wait_for_health && \
       verify_deployment; then

        log_success "Deployment completed successfully! 🚀"
        show_summary
        exit 0
    else
        log_error "Deployment failed!"

        # Ask for rollback confirmation
        read -p "Do you want to rollback to previous version? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rollback

            if wait_for_health; then
                log_success "Rollback successful"
                exit 1
            else
                log_error "Rollback failed. Manual intervention required."
                exit 2
            fi
        else
            log_warning "Deployment failed without rollback. Please investigate."
            exit 1
        fi
    fi
}

# ═══════════════════════════════════════════════════════════════════════════════
# Script Entry Point
# ═══════════════════════════════════════════════════════════════════════════════

# Handle Ctrl+C gracefully
trap 'echo -e "\n${RED}Deployment interrupted by user${NC}"; exit 130' INT

# Run main function
main
