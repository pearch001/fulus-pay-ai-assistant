# CI/CD Setup Guide - GitHub Actions

Automated deployment pipeline for Fulus Pay AI Assistant using GitHub Actions.

## Overview

This repository includes GitHub Actions workflows for:
- **Continuous Integration:** Build, test, and security scanning
- **Continuous Deployment:** Automated deployment to AWS EC2

## Workflows

### 1. Build and Test (`build-and-test.yml`)

Runs on every push and pull request to `main` and `develop` branches.

**Steps:**
- Checkout code
- Set up Java 17
- Run PostgreSQL test database
- Build with Maven
- Run unit and integration tests
- Security scanning with Trivy
- Upload build artifacts

**Triggers:**
```yaml
on:
  push:
    branches: [main, develop, feature/**]
  pull_request:
    branches: [main, develop]
```

### 2. Deploy to EC2 (`deploy-to-ec2.yml`)

Automatically deploys to EC2 on push to `main` or `production` branches.

**Steps:**
- Checkout code
- Setup SSH connection to EC2
- Pull latest code on EC2
- Build Docker images
- Deploy containers
- Run health checks

**Triggers:**
```yaml
on:
  push:
    branches: [main, production]
  workflow_dispatch:  # Manual trigger
```

## Required GitHub Secrets

Configure these in: **Settings → Secrets and variables → Actions**

### Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `EC2_HOST` | EC2 public DNS or IP | `ec2-xx-xx-xx-xx.compute-1.amazonaws.com` |
| `EC2_SSH_KEY` | Private SSH key (.pem content) | Contents of `fulus-ec2-key.pem` |
| `OPENAI_API_KEY` | OpenAI API key (for tests) | `sk-proj-xxxxx` |

### How to Add Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret:

**EC2_HOST:**
```
Name: EC2_HOST
Value: ec2-3-145-123-45.us-east-2.compute.amazonaws.com
```

**EC2_SSH_KEY:**
```
Name: EC2_SSH_KEY
Value: [Paste entire .pem file content]
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
-----END RSA PRIVATE KEY-----
```

**OPENAI_API_KEY:**
```
Name: OPENAI_API_KEY
Value: sk-proj-xxxxxxxxxxxxxxxxxxxxx
```

## Deployment Environments

### Free Tier Deployment (Default for `main` branch)

```yaml
- Branch: main
- Environment: free-tier
- Docker Compose: docker-compose.free-tier.yml
- Instance: t2.micro / t3.micro
```

### Production Deployment (`production` branch)

```yaml
- Branch: production
- Environment: production
- Docker Compose: docker-compose.prod.yml
- Instance: t3.medium or higher
```

## Manual Deployment

You can manually trigger deployment via GitHub Actions UI:

1. Go to **Actions** tab in your repository
2. Select **Deploy to AWS EC2** workflow
3. Click **Run workflow**
4. Select environment:
   - `production` - Full production setup
   - `free-tier` - Free tier optimized
   - `development` - Development setup
5. Click **Run workflow**

## Branch Strategy

```
main (free-tier)
  ├── develop
  │   ├── feature/user-auth
  │   ├── feature/ai-chat
  │   └── feature/payments
  └── production (production environment)
```

**Workflow:**
1. Create feature branch from `develop`
2. Push to feature branch → Runs tests
3. Merge to `develop` → Runs tests
4. Merge to `main` → Deploys to free tier
5. Merge to `production` → Deploys to production

## Local Testing Before Push

```bash
# Run tests locally
mvn clean test

# Build Docker image
docker build -t fulus-pay:local .

# Test Docker image
docker-compose up -d
curl http://localhost:8080/actuator/health
docker-compose down
```

## Monitoring Deployments

### View Workflow Runs

1. Go to **Actions** tab
2. Select a workflow run
3. View logs for each step
4. Check deployment status

### Deployment Logs

```bash
# On EC2 instance
sudo journalctl -u fulus-pay-free-tier -f

# Or view Docker logs
docker-compose -f docker-compose.free-tier.yml logs -f
```

## Rollback Strategy

If deployment fails, GitHub Actions will:
1. Stop the workflow
2. Send failure notification
3. Keep previous version running (containers not stopped)

Manual rollback on EC2:
```bash
# SSH into EC2
ssh -i ~/.ssh/key.pem ubuntu@EC2_HOST

# Navigate to backups
cd ~/fulus-pay-ai-assistant/backups

# List backups
ls -lt

# Restore previous version
BACKUP_NAME="backup_20241211_143022"
cp backups/$BACKUP_NAME/docker-compose.*.yml .
cp backups/$BACKUP_NAME/.env .

# Restart with previous version
docker-compose -f docker-compose.free-tier.yml up -d
```

## Troubleshooting CI/CD

### Build Fails

```bash
# Check Maven logs in GitHub Actions
# Common issues:
- Missing dependencies
- Test failures
- Compilation errors

# Fix locally first
mvn clean install
```

### Deployment Fails

```bash
# Check deployment logs in GitHub Actions
# Common issues:
- SSH key issues → Verify EC2_SSH_KEY secret
- Connection timeout → Check EC2 security group
- Docker errors → SSH to EC2 and check Docker

# Test SSH connection locally
ssh -i ~/.ssh/key.pem ubuntu@$EC2_HOST
```

### Health Check Fails

```bash
# SSH into EC2
ssh -i ~/.ssh/key.pem ubuntu@EC2_HOST

# Check application logs
docker logs fulus-app -f

# Check if containers are running
docker ps

# Test health endpoint
curl http://localhost:8080/actuator/health
```

## Advanced Configuration

### Custom Deployment Timeout

Edit `.github/workflows/deploy-to-ec2.yml`:

```yaml
- name: Health Check
  run: |
    TIMEOUT=600  # Increase to 10 minutes
    # ... rest of script
```

### Deploy to Multiple Environments

```yaml
strategy:
  matrix:
    environment: [staging, production]
    include:
      - environment: staging
        ec2_host: ${{ secrets.STAGING_EC2_HOST }}
      - environment: production
        ec2_host: ${{ secrets.PROD_EC2_HOST }}
```

### Slack/Discord Notifications

Add notification step:

```yaml
- name: Notify deployment
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## Security Best Practices

1. **Never commit secrets** - Use GitHub Secrets
2. **Rotate SSH keys** regularly (every 90 days)
3. **Use least privilege** - EC2 SSH key should only have deploy access
4. **Enable branch protection** - Require PR reviews before merging
5. **Scan for vulnerabilities** - Trivy scans on every build

### Branch Protection Rules

Settings → Branches → Add rule:

```yaml
Branch name pattern: main
☑ Require pull request reviews before merging
☑ Require status checks to pass before merging
  - Build and Test
☑ Require branches to be up to date before merging
```

## Cost Optimization

### Free Tier Tips

```yaml
# Run tests only on PR, not every push
on:
  pull_request:
    branches: [main, develop]

# Cache dependencies
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
```

### GitHub Actions Minutes

- **Free tier:** 2,000 minutes/month
- **Typical build:** 5-10 minutes
- **Typical deployment:** 3-5 minutes
- **Monthly capacity:** ~150-200 builds

## Example Workflow

Complete workflow from feature to production:

```bash
# 1. Create feature branch
git checkout -b feature/new-payment-method

# 2. Make changes and test locally
mvn clean test

# 3. Commit and push
git add .
git commit -m "Add new payment method"
git push origin feature/new-payment-method

# 4. Create Pull Request (triggers build-and-test.yml)
# GitHub Actions runs tests automatically

# 5. Merge to develop (triggers tests again)
# Tests pass ✅

# 6. Merge to main (triggers deploy to free tier)
# Automatic deployment to free tier EC2 ✅

# 7. When ready for production, merge to production branch
# Automatic deployment to production EC2 ✅
```

## Monitoring & Alerts

### GitHub Action Status Badge

Add to README.md:

```markdown
![Build Status](https://github.com/username/repo/workflows/Build%20and%20Test/badge.svg)
![Deployment Status](https://github.com/username/repo/workflows/Deploy%20to%20AWS%20EC2/badge.svg)
```

### Email Notifications

GitHub automatically sends emails for:
- ❌ Failed workflow runs
- ✅ Fixed workflow runs (after failure)

Configure in: **Settings → Notifications**

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Actions Marketplace](https://github.com/marketplace?type=actions)
- [Workflow Syntax Reference](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)

---

**Questions?** Open an issue or check the main [README.md](./README.md)

**Last Updated:** 2024-12-11
