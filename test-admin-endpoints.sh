#!/bin/bash

# Test script for Admin User Management
# This script tests the admin login and create admin endpoints

BASE_URL="http://localhost:8080/api/v1/auth"

echo "=========================================="
echo "Admin User Management Test Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Admin Login (should fail if no admin user exists)
echo -e "${YELLOW}Test 1: Admin Login${NC}"
echo "POST $BASE_URL/admin/login"
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@fuluspay.com",
    "password": "AdminPass123!"
  }')

echo "Response: $ADMIN_LOGIN_RESPONSE"
echo ""

# Extract access token if successful
ADMIN_TOKEN=$(echo $ADMIN_LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}❌ Admin login failed. Make sure you have created a SUPER_ADMIN user first.${NC}"
    echo -e "${YELLOW}Run the migration script and create a super admin user manually.${NC}"
    echo ""
else
    echo -e "${GREEN}✅ Admin login successful!${NC}"
    echo "Access Token: ${ADMIN_TOKEN:0:50}..."
    echo ""

    # Test 2: Create new admin user
    echo -e "${YELLOW}Test 2: Create New Admin User${NC}"
    echo "POST $BASE_URL/admin/create"
    CREATE_ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/create" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -d '{
        "phoneNumber": "08011111111",
        "fullName": "Test Admin User",
        "email": "testadmin@fuluspay.com",
        "password": "TestAdmin123!",
        "confirmPassword": "TestAdmin123!",
        "role": "ADMIN"
      }')

    echo "Response: $CREATE_ADMIN_RESPONSE"
    echo ""

    if echo "$CREATE_ADMIN_RESPONSE" | grep -q "accessToken"; then
        echo -e "${GREEN}✅ Admin user created successfully!${NC}"
        echo ""
    else
        echo -e "${RED}❌ Failed to create admin user${NC}"
        echo ""
    fi

    # Test 3: Login with the newly created admin
    echo -e "${YELLOW}Test 3: Login with Newly Created Admin${NC}"
    echo "POST $BASE_URL/admin/login"
    NEW_ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/admin/login" \
      -H "Content-Type: application/json" \
      -d '{
        "email": "testadmin@fuluspay.com",
        "password": "TestAdmin123!"
      }')

    echo "Response: $NEW_ADMIN_LOGIN"
    echo ""

    if echo "$NEW_ADMIN_LOGIN" | grep -q "accessToken"; then
        echo -e "${GREEN}✅ New admin login successful!${NC}"
        echo ""
    else
        echo -e "${RED}❌ New admin login failed${NC}"
        echo ""
    fi
fi

# Test 4: Regular user trying to access admin login (should succeed but with USER role)
echo -e "${YELLOW}Test 4: Regular User Attempting Admin Login${NC}"
echo "POST $BASE_URL/admin/login"
echo "Testing that regular users are rejected..."
REGULAR_USER_ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/admin/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "UserPassword123!"
  }')

echo "Response: $REGULAR_USER_ADMIN_LOGIN"
echo ""

if echo "$REGULAR_USER_ADMIN_LOGIN" | grep -q "Access denied"; then
    echo -e "${GREEN}✅ Regular user correctly rejected from admin login!${NC}"
else
    echo -e "${YELLOW}⚠️  Note: This test requires a regular user account to exist${NC}"
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo "Next Steps:"
echo "1. Run the database migration: migration-add-user-role.sql"
echo "2. Create your first SUPER_ADMIN user in the database"
echo "3. Use the admin endpoints to manage the system"
echo ""
echo "For more information, see ADMIN_USER_GUIDE.md"

