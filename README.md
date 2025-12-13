# Fulus Pay AI Assistant

A fintech AI assistant application powered by Spring Boot 3.2+ and Spring AI (OpenAI).

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.5**
- **Spring Data JPA**
- **PostgreSQL**
- **Spring AI (OpenAI)**
- **Lombok**
- **Maven**

## Prerequisites

- JDK 17 or higher
- PostgreSQL 12+
- Maven 3.6+
- OpenAI API Key

## Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE fulus_ai_db;
CREATE USER fulus_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE fulus_ai_db TO fulus_user;
```

## Configuration

1. Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

2. Update `.env` with your configuration:
```properties
DB_URL=jdbc:postgresql://localhost:5432/fulus_ai_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
OPENAI_API_KEY=sk-your-openai-api-key-here
```

## Running the Application

### Development Mode
```bash
# Using Maven
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode
```bash
# Set environment variables first, then:
SPRING_PROFILE=prod java -jar target/assistant-1.0.0-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Building the Application

```bash
# Clean and package
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

## Project Structure

```
src/main/java/com/fulus/ai/assistant/
├── config/              # Configuration classes (OpenAI, WebConfig)
├── controller/          # REST controllers (AIChatController)
├── dto/                 # Data Transfer Objects (ChatRequest, ChatResponse, etc.)
├── entity/              # JPA entities (User, Transaction, Budget, Conversation, etc.)
├── enums/               # Enums (TransactionType, TransactionCategory, BillType, etc.)
├── exception/           # Custom exceptions
├── function/            # Spring AI function tools (6 functions)
├── repository/          # Spring Data JPA repositories
├── service/             # Business logic services
│   ├── chat/           # Chat memory implementations (PostgreSQLChatMemory)
│   ├── AIFinancialAssistantService.java
│   ├── PaymentService.java
│   ├── BillPaymentService.java
│   ├── SavingsService.java
│   └── ChatMemoryMaintenanceService.java
├── util/                # Utility classes (TimePeriodParser)
├── converter/           # JPA converters (JSON converters)
└── FulusPayAiAssistantApplication.java
```

## API Endpoints

### Authentication API

#### Register New User (with BVN Verification)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "fullName": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com",
    "pin": "1234",
    "bvn": "12345678902",
    "dateOfBirth": "1990-05-15"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "phoneNumber": "08012345678",
    "name": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com"
  }
}
```

**Registration Flow:**
1. Validate Nigerian phone number (070/080/081/090/091)
2. Verify BVN (11 digits, mock API with 500ms delay)
3. Cross-check name similarity with BVN records
4. Hash PIN with BCrypt
5. Create user account with ₦0 initial balance
6. Credit ₦1,000 welcome bonus
7. Generate JWT tokens

**BVN Mock Rules (for testing):**
- BVN ending in **even number** = APPROVED ✅
- BVN ending in **odd number** = REJECTED ❌
- Example approved: `12345678902`, `22222222220`
- Example rejected: `12345678901`, `11111111111`

#### Login with PIN
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "08012345678",
    "pin": "1234"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "phoneNumber": "08012345678",
    "name": "Chukwuemeka Okonkwo",
    "email": "chukwuemeka@example.com",
    "balance": 1000.00,
    "lastLoginAt": "2024-12-11T14:30:22"
  }
}
```

**Login Flow:**
1. Find user by phone number
2. Check if account is locked (failed attempts)
3. Check if account is active
4. Verify PIN using BCrypt
5. Reset failed login attempts
6. Update lastLoginAt timestamp
7. Generate JWT token pair
8. Return tokens + user details + balance

**Rate Limiting:**
- **5 failed attempts** = Account locked for **15 minutes**
- Security logging of all failed attempts
- Lock time displayed in error message

#### Refresh Access Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

#### Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer your-access-token-here"
```

#### Get Current User Info
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer your-access-token-here"
```

**Security Features:**
- **JWT-based authentication:** Stateless, secure tokens
- **Access tokens:** 24-hour expiry
- **Refresh tokens:** 7-day expiry, stored in database
- **PIN-based login:** 4-6 digit PIN with BCrypt encryption
- **Account locking:** 5 failed attempts = 15-minute lockout
- **Protected endpoints:** All `/api/**` routes require authentication

### AI Chat API

#### Send Message (Blocking)
```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "How much did I spend on food last month?"
  }'
```

#### Stream Response (SSE)
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "Create a budget for my ₦200,000 salary"
  }' \
  --no-buffer
```

#### Get Conversation History
```bash
curl -X GET http://localhost:8080/api/v1/chat/history/{userId}
```

#### Clear Conversation History
```bash
curl -X DELETE http://localhost:8080/api/v1/chat/history/{userId}
```

See [AI_CHAT_API.md](AI_CHAT_API.md) for complete API documentation with streaming examples (JavaScript, Python, React, Node.js).

### Voice Assistant API

#### Process Voice Request
```bash
curl -X POST http://localhost:8080/api/v1/voice \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -F "audioFile=@recording.wav" \
  -F "generateAudio=true" \
  -F "voice=nova"
```

**Response:**
```json
{
  "success": true,
  "transcribedText": "How much did I spend on food?",
  "aiResponse": "Based on your transactions...",
  "audioResponseUrl": "/api/v1/voice/audio/response_xyz.mp3",
  "processingTimeMs": 3450
}
```

#### Download Audio Response
```bash
curl -X GET http://localhost:8080/api/v1/voice/audio/{filename} --output response.mp3
```

**Features:**
- **Speech-to-Text:** OpenAI Whisper transcription
- **AI Processing:** Full AI assistant capabilities (all 6 function tools)
- **Text-to-Speech:** OpenAI TTS with 6 voice options
- **Supported Formats:** MP3, WAV, M4A, MP4, WebM
- **Max File Size:** 10MB
- **Automatic Cleanup:** Files deleted after 24 hours

See [VOICE_API.md](VOICE_API.md) for complete Voice API documentation with React and Python examples.

### Transaction API

#### Get Paginated Transactions
```bash
curl -X GET "http://localhost:8080/api/v1/transactions/{userId}?page=0&size=20&category=FOOD&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59"
```

**Response:**
```json
{
  "transactions": [...],
  "currentPage": 0,
  "totalPages": 5,
  "totalElements": 100,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

#### Get Transaction Summary
```bash
curl -X GET "http://localhost:8080/api/v1/transactions/{userId}/summary?period=this_month"
```

**Supported periods:** `this_month`, `last_month`, `last_3_months`, `last_6_months`, `this_year`, `last_year`, `all_time`, `custom`

**Response:**
```json
{
  "period": "This Month",
  "totalIncome": 250000.00,
  "totalExpenses": 125000.00,
  "netAmount": 125000.00,
  "totalTransactions": 45,
  "categoryBreakdown": [
    {
      "category": "FOOD",
      "amount": 35000.00,
      "transactionCount": 12,
      "percentage": 28.0
    }
  ],
  "topCategory": {
    "category": "FOOD",
    "amount": 35000.00,
    "percentage": 28.0
  }
}
```

#### Export Transactions
```bash
# Export as CSV
curl -X GET "http://localhost:8080/api/v1/transactions/{userId}/export?format=csv&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59" --output statement.csv

# Export as PDF
curl -X GET "http://localhost:8080/api/v1/transactions/{userId}/export?format=pdf&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59" --output statement.pdf
```

**Features:**
- **Pagination:** Supports page and size parameters (max 100 per page)
- **Filtering:** Filter by category, date range
- **Summary:** Spending analysis by category with percentages
- **Export:** Download transactions as CSV or PDF with formatted statements

### Health Check
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/chat/health
curl http://localhost:8080/api/v1/voice/health
curl http://localhost:8080/api/v1/transactions/health
```

### Metrics (Production)
```bash
curl http://localhost:8080/actuator/metrics
```

## Environment Profiles

- **dev** (default) - Development with verbose logging and SQL debugging
- **prod** - Production-ready with optimized settings for AWS EC2

## Key Features

### Core Banking Features
- User management with balance tracking
- Transaction processing with multiple categories
- **Transaction export to CSV and PDF with formatted statements**
- **Paginated transaction history with filtering (by category, date range)**
- **Transaction summary and analytics by category**
- Savings account management with compound interest calculation
- Monthly budgeting with category-wise allocation
- Bill payment integration (Electricity, Water, Airtime, Data, Cable TV)

### AI & Voice Features
- **AI-powered financial assistance with Spring AI & OpenAI GPT-4 Turbo**
- **Voice interaction with OpenAI Whisper (speech-to-text) and TTS (text-to-speech)**
- **Real-time streaming responses via Server-Sent Events (SSE)**
- **PostgreSQL-backed conversation memory with automatic pruning**
- **Intelligent chat context management (sliding window of 20 messages)**
- **Backpressure handling and error recovery for streaming**
- **Multipart file upload support for voice recordings (MP3, WAV, M4A, MP4, WebM)**

### Security Features
- **JWT-based stateless authentication (Access + Refresh tokens)**
- **PIN-based authentication with BCrypt encryption (12 rounds)**
- **Account lockout protection (5 attempts = 15-minute lockout)**
- **Role-based access control with Spring Security**
- **Secure password/PIN storage with BCrypt**
- **Token revocation and refresh token management**
- **CORS configuration for mobile app integration**

### Technical Features
- RESTful API design with blocking and streaming endpoints
- Comprehensive logging and monitoring
- Retry logic with exponential backoff for external services
- Database migrations with Hibernate auto-update
- Production-ready configuration for AWS EC2

## Database Entities

1. **User** - User accounts with phone number, email, balance, PIN, account locking
2. **Transaction** - Financial transactions with categorization
3. **SavingsAccount** - Savings goals with interest calculation
4. **Budget** - Monthly budget planning with JSON category storage
5. **Conversation** - AI chat conversations with token tracking
6. **ChatMessage** - Individual chat messages with sequence ordering and JSONB metadata
7. **RefreshToken** - JWT refresh tokens with expiry and revocation support

## AI Chat Configuration

### OpenAI Setup

The application uses **Spring AI with OpenAI GPT-4 Turbo** for intelligent financial assistance.

**Configuration:**
- Model: `gpt-4-turbo` (configurable via `OPENAI_MODEL`)
- Temperature: `0.7` (balanced creativity and consistency)
- Max Tokens: `1000` per response
- Retry Logic: 3 attempts with exponential backoff

**Chat Memory:**
- **Type:** PostgreSQL-backed persistent memory
- **Max Messages:** 20 (sliding window)
- **Cache TTL:** 1 hour (in-memory cache for active conversations)
- **Auto-Pruning:** Messages older than 30 days (configurable)
- **Token Tracking:** Automatic estimation and conversation-level tracking

**Features:**
- Conversation history per user
- Automatic context management
- Graceful degradation on API failures
- Cache eviction for inactive conversations
- Scheduled maintenance (daily at 2 AM)
- **Function Calling:** AI can automatically query transaction data
- **Streaming Support:** Real-time token delivery via SSE
- **Backpressure Handling:** 1000-token buffer with overflow protection
- **Retry Logic:** 3 attempts with exponential backoff for streaming
- **Chat Memory During Streaming:** Full response saved after completion

**Spring AI Function Tools (6 Available):**

1. **queryTransactions** - Query user transactions with natural language time periods
   - Supports: "this month", "last week", "last 30 days", etc.
   - Returns: Formatted summary with totals and category breakdown
   - Auto-discovers and executes based on user queries

2. **generateStatement** - Generate detailed account statements
   - Parameters: userId, transactionType (credit/debit/all), period
   - Returns: Formatted table with Date | Description | Amount | Balance
   - Includes: Opening balance, total credits/debits, closing balance

3. **calculateSavingsProjection** - Calculate savings with compound interest
   - Parameters: monthlyAmount, months, interestRate (optional, defaults to 5%)
   - Returns: Conversational explanation with totals, interest earned, and month-by-month growth
   - Perfect for financial planning and goal setting

4. **createBudget** - Create personalized monthly budgets
   - Analyzes last 3 months of spending history
   - Applies 50/30/20 budgeting rule adapted for Nigerian context
   - Parameters: userId, monthlyIncome, preferencesJson (optional)
   - Returns: Detailed budget breakdown with personalized recommendations
   - Supports multiple budget styles: balanced, aggressive_saver, flexible

5. **sendMoney** ⚠️ - Execute real money transfers (CRITICAL)
   - Requires explicit user confirmation before execution
   - Parameters: senderId, recipientIdentifier (phone or name), amount, note
   - Smart recipient resolution by phone number or name
   - Maximum limit: ₦1,000,000 per transaction
   - Returns: Transaction confirmation with reference and new balance

6. **payBill** ⚠️ - Execute real bill payments (CRITICAL)
   - Requires explicit user confirmation before execution
   - Parameters: userId, billType, amount, accountNumber
   - Supports: ELECTRICITY, WATER, AIRTIME, DATA, CABLE_TV
   - Maximum limit: ₦1,000,000 per payment
   - Returns: Payment confirmation with token and transaction reference

⚠️ **Security Note:** Functions 5 and 6 execute REAL financial transactions and include multiple safety checks:
- Amount validation and limits
- User confirmation required
- Comprehensive audit logging
- Error handling with safe messages

See [SPRING_AI_FUNCTIONS.md](SPRING_AI_FUNCTIONS.md) for detailed function documentation and security considerations.

**System Prompt:**
The AI assistant uses a comprehensive system prompt defined in `src/main/resources/ai-system-prompt.txt`:
- Defines "Fulus AI" personality and role
- Nigerian financial context awareness
- Transaction safety rules (confirmation for >₦10,000)
- Financial literacy guidelines
- Tool usage instructions
- Dynamic variables: {{USER_NAME}}, {{USER_BALANCE}}, {{CURRENT_DATE}}, etc.

**Environment Variables:**
```bash
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4-turbo
```

## Security Notes

### Authentication & Authorization
- **JWT Secret Key:** Must be at least 256 bits (32 characters). Change default in production!
- **Access Tokens:** Short-lived (24 hours), include user info in claims
- **Refresh Tokens:** Long-lived (7 days), stored in database, can be revoked
- **PIN Security:** 4-6 digits, encrypted with BCrypt (12 rounds)
- **Account Lockout:** 5 failed login attempts = 15-minute lockout
- **Protected Endpoints:** All `/api/**` routes require `Authorization: Bearer <token>` header

### Example Authenticated Request
```bash
# Login first to get token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"08012345678","pin":"1234"}' \
  | jq -r '.accessToken')

# Use token for authenticated requests
curl -X GET "http://localhost:8080/api/v1/transactions/{userId}" \
  -H "Authorization: Bearer $TOKEN"
```

### General Security
- Never commit `.env` file to version control
- Use strong passwords for database
- **Keep OpenAI API key secure and rotate regularly**
- **Rotate JWT secret key regularly in production**
- Enable SSL in production (`REQUIRE_SSL=true`)
- Monitor token usage to control costs
- Use environment variables for all secrets

## AWS EC2 Deployment

1. Install Java 17+ on EC2 instance
2. Install PostgreSQL or use RDS
3. Set environment variables
4. Update security groups (port 8080)
5. Run with production profile:
```bash
SPRING_PROFILE=prod java -jar assistant-1.0.0-SNAPSHOT.jar
```

## Logging

- **Development**: Logs to console and `logs/fulus-pay-ai-assistant.log`
- **Production**: Logs to `/var/log/fulus-pay-ai-assistant/application.log`

## Additional Resources

- **[Security & Authentication API](./SECURITY_API.md)** - JWT authentication, PIN login, token management
- **[AI Chat API Documentation](./AI_CHAT_API.md)** - REST endpoints, streaming, client examples
- **[Voice API Documentation](./VOICE_API.md)** - Speech-to-text, TTS, voice interaction examples
- **[Transaction API Documentation](./TRANSACTION_API.md)** - Pagination, filtering, analytics, CSV/PDF export
- **[Spring AI Functions Documentation](./SPRING_AI_FUNCTIONS.md)** - All 6 function tools
- **[AI System Prompt Guide](./AI_SYSTEM_PROMPT_GUIDE.md)** - Modifying AI personality and behavior
- **[System Prompt File](./src/main/resources/ai-system-prompt.txt)** - Comprehensive AI instructions

## Support

For issues and questions, please contact the development team.

---

**Generated with Spring Boot 3.2+ and Spring AI**
