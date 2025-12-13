# Spring AI Function Calling

This document describes the Spring AI function tools available in the Fulus Pay AI Assistant application.

## Overview

Spring AI function calling allows the AI model to automatically invoke backend functions to fetch real-time data and perform operations. When users ask questions that require data access, the AI automatically calls the appropriate function, retrieves the data, and incorporates it into the response.

The Fulus Pay AI Assistant provides 6 function tools:

1. **TransactionQueryFunction** - Query and analyze transaction history
2. **StatementGeneratorFunction** - Generate formatted account statements
3. **SavingsCalculatorFunction** - Calculate savings projections with compound interest
4. **BudgetAssistantFunction** - Create personalized budgets based on income and spending patterns
5. **SendMoneyFunction** ⚠️ - Execute real money transfers (CRITICAL: requires explicit user confirmation)
6. **PayBillFunction** ⚠️ - Execute real bill payments (CRITICAL: requires explicit user confirmation)

⚠️ **Important:** Functions 5 and 6 execute REAL financial transactions and must be used with extreme caution. See Security Considerations section for detailed safety requirements.

## Available Functions

### 1. TransactionQueryFunction

**Purpose:** Query and analyze user transactions with flexible filtering

**Function Name:** `queryTransactions`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

#### Parameters

```json
{
  "userId": "string (required)",
  "category": "string (optional)",
  "timePeriod": "string (optional)"
}
```

#### Parameter Descriptions

- **userId**: The unique identifier (UUID) of the user whose transactions to query
- **category**: Filter by transaction category. Valid values:
  - `BILL_PAYMENT`, `TRANSFER`, `SAVINGS`, `WITHDRAWAL`, `DEPOSIT`
  - `AIRTIME`, `DATA`, `SHOPPING`, `FOOD`, `TRANSPORT`
  - `ENTERTAINMENT`, `UTILITIES`, `HEALTHCARE`, `EDUCATION`, `OTHER`
  - Leave null for all categories
- **timePeriod**: Natural language time period. Supported formats:
  - `"today"`, `"yesterday"`
  - `"this week"`, `"last week"`
  - `"this month"`, `"last month"`
  - `"this year"`, `"last year"`
  - `"last 7 days"`, `"last 30 days"`, `"last 3 months"`
  - `"last N days"`, `"last N weeks"`, `"last N months"`
  - Leave null for all time

#### Return Value

Returns a formatted string containing:
- Transaction count
- Total income
- Total expenses
- Net amount
- Breakdown by category with amounts and counts

Example output:
```
Transaction Summary
==================

Period: Last 30 days
Total Transactions: 42
Total Income: ₦250,000.00
Total Expenses: ₦180,000.00
Net Amount: ₦70,000.00

Breakdown by Category:
----------------------
  • SHOPPING: ₦85,000.00 (15 transactions)
  • BILL_PAYMENT: ₦45,000.00 (8 transactions)
  • FOOD: ₦30,000.00 (12 transactions)
  • TRANSPORT: ₦20,000.00 (7 transactions)
```

### 2. StatementGeneratorFunction

**Purpose:** Generate detailed account statements with transaction history in a formatted table

**Function Name:** `generateStatement`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

#### Parameters

```json
{
  "userId": "string (required)",
  "transactionType": "string (optional)",
  "period": "string (optional)"
}
```

#### Parameter Descriptions

- **userId**: The unique identifier (UUID) of the user whose statement to generate
- **transactionType**: Type of transactions to include. Valid values:
  - `"credit"` - Incoming money only (deposits, received transfers)
  - `"debit"` - Outgoing money only (withdrawals, payments, transfers)
  - `"all"` - Both credit and debit transactions (default)
- **period**: Natural language time period (same format as queryTransactions)
  - `"today"`, `"yesterday"`, `"this week"`, `"last month"`, etc.
  - Leave null for all time

#### Return Value

Returns a formatted account statement as a string containing:
- Header with account holder info and period
- Opening balance
- Transaction table with columns: Date | Description | Amount | Balance
- Summary with totals (opening balance, credits, debits, closing balance)
- Transaction count

Example output:
```
══════════════════════════════════════════════════════════════════════════════════════════
                         ACCOUNT STATEMENT
══════════════════════════════════════════════════════════════════════════════════════════

Account Holder: John Doe
Account ID: 123e4567-e89b-12d3-a456-426614174000
Period: Last 30 days
Transaction Type: ALL
Generated: 10/12/2025 14:30

Opening Balance: ₦150,000.00

──────────────────────────────────────────────────────────────────────────────────────────
Date                 | Description                    | Amount       | Balance
──────────────────────────────────────────────────────────────────────────────────────────
11/11/2025 09:15     | Salary Deposit                 | +₦250,000.00 | ₦400,000.00
12/11/2025 14:20     | Shopping at Mall               | -₦35,000.00  | ₦365,000.00
15/11/2025 10:45     | Electricity Bill Payment       | -₦15,000.00  | ₦350,000.00
18/11/2025 16:30     | Transfer to Jane Smith         | -₦50,000.00  | ₦300,000.00
25/11/2025 08:00     | ATM Withdrawal                 | -₦20,000.00  | ₦280,000.00
──────────────────────────────────────────────────────────────────────────────────────────

SUMMARY
──────────────────────────────────────────────────
Opening Balance:    ₦150,000.00
Total Credits:      ₦250,000.00 (+)
Total Debits:       ₦120,000.00 (-)
──────────────────────────────────────────────────
Closing Balance:    ₦280,000.00
Current Balance:    ₦280,000.00

Total Transactions: 5

══════════════════════════════════════════════════════════════════════════════════════════
```

**Use Cases:**
- "Show me my account statement for this month"
- "Generate my transaction history for last week"
- "I need a statement of all my credit transactions this year"
- "Can I see my statement with only debit transactions?"

### 3. SavingsCalculatorFunction

**Purpose:** Calculate savings projections with compound interest to help users plan their financial goals

**Function Name:** `calculateSavingsProjection`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

#### Parameters

```json
{
  "monthlyAmount": "number (required)",
  "months": "number (required)",
  "interestRate": "number (optional)"
}
```

#### Parameter Descriptions

- **monthlyAmount**: Monthly savings amount in currency (e.g., 10000 for ₦10,000 per month)
- **months**: Number of months to save for (e.g., 12 for 1 year, 24 for 2 years, 60 for 5 years)
- **interestRate**: Annual interest rate as a percentage (e.g., 5.0 for 5% per year). Defaults to 5.0% if not provided. This rate is compounded monthly.

#### Return Value

Returns a conversational, easy-to-understand explanation containing:
- Scenario description (how much, for how long, at what rate)
- Total contributions (amount you save)
- Interest earned (extra money from compound interest)
- Final amount (total you'll have)
- Effective annual yield (actual return per year)
- Month-by-month growth projection (sample showing first, middle, and last months)
- Advice and encouragement

Example output:
```
💰 Savings Projection Calculator
════════════════════════════════════════

If you save ₦10,000.00 every month for 2 years at 5.00% annual interest, here's what happens:

📊 THE RESULTS
────────────────────────────────────────
Your Total Contributions:  ₦240,000.00
Interest You'll Earn:      ₦13,468.55 💸
Your Final Amount:         ₦253,468.55 ✨

Effective Annual Yield:    2.81%

💡 WHAT THIS MEANS
────────────────────────────────────────
Your money grows by 5.6% thanks to compound interest! That means your money earns interest,
and then that interest earns interest too.

📈 MONTH-BY-MONTH GROWTH
────────────────────────────────────────
Month   1: ₦ 10,041.67
Month   2: ₦ 20,125.17
Month   3: ₦ 30,250.56
    ...
Month  12: ₦121,655.82
    ...
Month  22: ₦224,012.95
Month  23: ₦234,466.83
Month  24: ₦253,468.55

✅ BOTTOM LINE
────────────────────────────────────────
You'll earn an extra ₦13,468.55 just by being patient and consistent with your savings.
That's a decent return on your savings!

Start saving today and watch your money grow! 🌱
```

**Use Cases:**
- "If I save ₦10,000 every month for 2 years, how much will I have?"
- "Calculate my savings projection for ₦5,000 monthly for 12 months"
- "How much will I earn if I save ₦20,000 per month with 6% interest?"
- "Show me how my money will grow if I save for 5 years"
- "What's the return on saving ₦15,000 monthly?"

**Key Features:**
- 💰 Uses compound interest formula (interest on interest)
- 📈 Shows month-by-month growth
- 🎯 Calculates effective annual yield
- 💬 Conversational, easy-to-understand format
- 🌱 Includes motivation and financial advice
- ✨ Perfect for goal planning and financial education

### 4. BudgetAssistantFunction

**Purpose:** Create personalized monthly budgets based on income and spending history

**Function Name:** `createBudget`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

#### Parameters

```json
{
  "userId": "string (required)",
  "monthlyIncome": "number (required)",
  "preferencesJson": "string (optional)"
}
```

#### Parameter Descriptions

- **userId**: The unique identifier (UUID) of the user for whom to create the budget
- **monthlyIncome**: User's monthly income in currency (e.g., 150000 for ₦150,000 per month)
  - Must be greater than ₦10,000
  - Used as the basis for budget allocation
- **preferencesJson**: Optional JSON string with user preferences for budget allocation
  - **Balanced style** (default): `{"style": "balanced"}` - 50/30/20 split (50% needs, 30% wants, 20% savings)
  - **Aggressive saver**: `{"style": "aggressive_saver"}` - 70/15/15 split (70% needs, 15% wants, 15% savings)
  - **Flexible**: `{"style": "flexible"}` - 60/30/10 split (60% needs, 30% wants, 10% savings)
  - **Custom savings goal**: `{"savingsGoal": 25}` - Specify custom savings percentage (10-50%)
  - Leave null for default balanced style

#### Return Value

Returns a comprehensive, formatted budget plan containing:

**Main Sections:**
1. **Budget Breakdown** - 50/30/20 rule application
   - Needs allocation (bills, food, transport, utilities, healthcare)
   - Wants allocation (entertainment, shopping, dining)
   - Savings allocation

2. **Category Allocations** - Detailed breakdown by expense category
   - Amount allocated per category
   - Percentage of total income
   - Sorted by allocation amount

3. **Spending Analysis** (if transaction history exists)
   - Last 3 months transaction count
   - Total amount spent
   - Average monthly spending
   - Top spending categories with percentages

4. **Personalized Recommendations** - Based on actual spending vs budget
   - Overspending alerts
   - Savings rate feedback
   - Category-specific advice
   - Nigerian context tips (transport, bills, utilities)
   - Action items

5. **Budget Confirmation** - Saved to database for tracking

Example output:
```
💰 PERSONALIZED BUDGET PLAN
══════════════════════════════════════════════════════════

Hi John Doe! Here's your budget for 2025-12
Monthly Income: ₦150,000.00

📊 BUDGET BREAKDOWN (50/30/20 Rule)
──────────────────────────────────────────────────────────
Needs (50%):        ₦75,000.00
Wants (30%):        ₦45,000.00
Savings (20%):      ₦30,000.00 💎

📋 CATEGORY ALLOCATIONS
──────────────────────────────────────────────────────────
  • Bill payment          ₦ 25,000.00  (16.7%)
  • Food                  ₦ 22,000.00  (14.7%)
  • Transport             ₦ 18,000.00  (12.0%)
  • Shopping              ₦ 15,000.00  (10.0%)
  • Entertainment         ₦ 12,000.00  ( 8.0%)
  • Utilities             ₦ 10,000.00  ( 6.7%)
  • Healthcare            ₦  8,000.00  ( 5.3%)

📈 YOUR SPENDING ANALYSIS (Last 3 Months)
──────────────────────────────────────────────────────────
Total Transactions: 142
Total Spent:        ₦420,000.00
Avg Monthly:        ₦140,000.00

Top Spending Categories:
  • Food                  ₦ 35,000.00/month  (25.0% of spending)
  • Bill payment          ₦ 28,000.00/month  (20.0% of spending)
  • Transport             ₦ 22,000.00/month  (15.7% of spending)
  • Shopping              ₦ 18,000.00/month  (12.9% of spending)
  • Entertainment         ₦ 15,000.00/month  (10.7% of spending)

💡 PERSONALIZED RECOMMENDATIONS
──────────────────────────────────────────────────────────
1. 💰 Consider reducing your food expenses. You're currently spending ₦35,000
   but should aim for ₦22,000.
2. 🌟 Excellent! You're already saving well. Consider investing some savings
   for better returns.
3. 📊 Bills take up a large portion of your budget. Look for ways to reduce
   utility costs (e.g., energy-efficient appliances, prepaid plans).
4. ✅ Review your budget weekly and adjust as needed.
5. 📱 Track all expenses using Fulus Pay to stay within your budget.

✅ Budget saved successfully for 2025-12!
Track your spending and stay within budget to achieve your financial goals. 🎯
```

#### How It Works

1. **Historical Analysis**: Queries last 3 months of transactions
2. **Category Grouping**: Groups expenses by category (food, bills, transport, etc.)
3. **Budget Rule Application**: Applies 50/30/20 or custom split
4. **Proportional Allocation**: Distributes category budgets based on historical spending ratios
5. **Recommendation Generation**: Compares actual spending to budget and generates advice
6. **Database Storage**: Saves budget to `budgets` table for current month (updates if exists)

#### Budget Allocation Logic

**Needs Categories (50%):**
- BILL_PAYMENT
- FOOD
- TRANSPORT
- UTILITIES
- HEALTHCARE

**Wants Categories (30%):**
- ENTERTAINMENT
- SHOPPING

**Savings (20%):**
- Recommended for emergency fund, investments, or savings goals

#### Use Cases

- "Create a budget for me, my income is ₦150,000"
- "Help me make a budget plan"
- "I earn ₦200,000 per month, how should I allocate it?"
- "Create an aggressive savings budget for ₦250,000 income"
- "Make a budget with 25% savings goal"
- "I need help managing my ₦180,000 salary"

#### Key Features

- 📊 Analyzes 3 months of spending history
- 🎯 Applies proven 50/30/20 budgeting rule
- 🇳🇬 Adapted for Nigerian context (transport, bills, utilities)
- 💡 Personalized recommendations based on actual behavior
- 📈 Compares current spending to suggested budget
- 💾 Saves budget to database for tracking
- 🔄 Updates existing budget if one already exists for current month
- 🎨 Multiple budget styles (balanced, aggressive saver, flexible)
- ⚙️ Custom savings goals (10-50%)
- 📋 Category-wise breakdown with percentages

#### Nigerian Context Adaptations

1. **Transport Costs**: Recognizes high transport costs in Nigerian cities and provides specific advice
2. **Bill Payments**: Acknowledges utility bill challenges and suggests prepaid options
3. **Food Budgeting**: Adapts to Nigerian food shopping patterns
4. **Cash Flow**: Considers irregular income patterns common in Nigeria
5. **Savings Culture**: Encourages savings with specific percentage targets

### 5. SendMoneyFunction ⚠️

**Purpose:** Execute real money transfers to other users by phone number or name

**Function Name:** `sendMoney`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

⚠️ **CRITICAL WARNING:** This function executes REAL financial transactions. Use extreme caution and ALWAYS confirm transaction details with the user before executing.

#### Parameters

```json
{
  "senderId": "string (required)",
  "recipientIdentifier": "string (required)",
  "amount": "number (required)",
  "note": "string (optional)"
}
```

#### Parameter Descriptions

- **senderId**: The unique identifier (UUID) of the user sending the money
- **recipientIdentifier**: The recipient identifier - can be:
  - Phone number (e.g., '08012345678', '+2348012345678')
  - Full name (e.g., 'John Doe')
  - Partial name match is supported
- **amount**: Amount to send in currency (e.g., 5000 for ₦5,000)
  - Must be greater than 0
  - Maximum limit: ₦1,000,000 per transaction
- **note**: Optional note or message to include with the transfer (e.g., 'Lunch money', 'Payment for rent')

#### Return Value

Returns a formatted confirmation or error message containing:

**Success Response:**
- ✅ Success indicator
- Amount transferred
- Recipient name and phone
- Transaction ID and reference
- New balance
- Timestamp
- Notification confirmation

**Error Response:**
- ❌ Error indicator
- Detailed error message
- Guidance on next steps

Example success output:
```
✅ MONEY SENT SUCCESSFULLY!
═══════════════════════════════════════

Amount:         ₦5,000.00
Recipient:      John Doe
Phone:          +2348012345678
Note:           Lunch money

Transaction ID: 123e4567-e89b-12d3-a456-426614174000
Reference:      TXN-ABC123DEF456
New Balance:    ₦45,000.00
Time:           2025-12-10T14:30:00

The recipient has been notified of this transfer.
Thank you for using Fulus Pay! 💚
```

#### Safety Features

1. **Recipient Resolution:**
   - Tries phone number first (with cleaning/normalization)
   - Falls back to exact name match
   - Falls back to partial name match
   - Returns clear error if recipient not found

2. **Validation:**
   - Sender and recipient existence checks
   - Amount validation (positive, within limits)
   - Self-transfer prevention
   - Sender account balance verification (via PaymentService)

3. **Transaction Safety:**
   - Uses @Transactional PaymentService for atomicity
   - Generates unique transaction reference
   - Logs all transaction attempts with WARNING level
   - Returns safe error messages (never exposes sensitive data)

4. **Account Privacy:**
   - Only returns recipient's name and phone (no account details)
   - Masked transaction reference in responses

#### Use Cases

⚠️ **ONLY use when user EXPLICITLY requests money transfer:**
- "Send ₦5,000 to John Doe"
- "Transfer ₦10,000 to 08012345678"
- "Send money to Jane with note 'Lunch payment'"
- "Pay ₦15,000 to my friend Michael"

❌ **NEVER suggest or auto-execute transfers:**
- User asks "How much can I send?" → Provide info only, don't execute
- User says "I need to pay John" → Ask for confirmation of amount and details
- User browses transactions → Don't suggest sending money

### 6. PayBillFunction ⚠️

**Purpose:** Execute real bill payments for electricity, water, airtime, data, and cable TV

**Function Name:** `payBill`

**Auto-Discovery:** Yes - automatically registered with Spring AI via `@Component` and function callback wrapper

⚠️ **CRITICAL WARNING:** This function executes REAL financial transactions. Use extreme caution and ALWAYS confirm payment details with the user before executing.

#### Parameters

```json
{
  "userId": "string (required)",
  "billType": "string (required)",
  "amount": "number (required)",
  "accountNumber": "string (required)"
}
```

#### Parameter Descriptions

- **userId**: The unique identifier (UUID) of the user paying the bill
- **billType**: Type of bill to pay. Valid values:
  - `ELECTRICITY` - Electricity utility payments
  - `WATER` - Water utility payments
  - `AIRTIME` - Mobile phone airtime recharge
  - `DATA` - Mobile data bundle purchases
  - `CABLE_TV` - Cable TV subscription payments
- **amount**: Amount to pay in currency (e.g., 5000 for ₦5,000)
  - Must be greater than 0
  - Maximum limit: ₦1,000,000 per payment
- **accountNumber**: Bill account number or meter number
  - For electricity: meter number (e.g., '1234567890')
  - For water: account number
  - For airtime/data: phone number
  - For cable TV: smartcard number or account number

#### Return Value

Returns a formatted confirmation or error message containing:

**Success Response:**
- ✅ Success indicator with payment details
- Bill type and provider name
- Account number (masked for privacy)
- Amount paid
- Payment token (for provider verification)
- Transaction ID and reference
- New balance
- Timestamp
- Important instructions for token usage

**Error Response:**
- ❌ Error indicator
- Detailed error message
- Troubleshooting guidance

Example success output:
```
✅ BILL PAYMENT SUCCESSFUL!
═══════════════════════════════════════

Bill Type:      Electricity
Provider:       PowerCo Electric
Account:        ****7890
Amount:         ₦5,000.00

PAYMENT DETAILS
───────────────────────────────────────
Payment Token:  ELEC-A1B2C3D4E5F6
Transaction ID: 123e4567-e89b-12d3-a456-426614174000
Reference:      TXN-XYZ789ABC123
New Balance:    ₦45,000.00
Time:           2025-12-10T14:30:00

⚡ Important: Please save your payment token for future reference.
   You may need it to verify the transaction with the service provider.

Thank you for using Fulus Pay! 💚
```

#### Safety Features

1. **Bill Type Validation:**
   - Validates against enum: ELECTRICITY, WATER, AIRTIME, DATA, CABLE_TV
   - Case-insensitive matching with normalization
   - Clear error message for invalid types

2. **Validation:**
   - User existence check
   - Amount validation (positive, within limits)
   - Account number presence check
   - Bill type parsing with error handling

3. **Payment Safety:**
   - Uses @Transactional BillPaymentService
   - Mock provider integration (simulated API calls)
   - Generates unique payment token
   - Logs all payment attempts with WARNING level
   - Returns safe error messages

4. **Privacy:**
   - Account numbers masked (shows only last 4 digits)
   - Payment token for verification
   - Transaction reference for tracking

5. **Provider Integration:**
   - Mock providers simulate real API calls
   - Provider-specific validation (e.g., meter number format)
   - Random success/failure simulation for testing
   - Provider name returned in confirmation

#### Use Cases

⚠️ **ONLY use when user EXPLICITLY requests bill payment:**
- "Pay ₦5,000 electricity bill for meter 1234567890"
- "Recharge ₦1,000 airtime on 08012345678"
- "Buy ₦2,000 data bundle"
- "Pay my DSTV subscription, account 9876543210"
- "Pay water bill ₦3,000 for account ABC123"

❌ **NEVER suggest or auto-execute payments:**
- User asks "How much is my electricity bill?" → Provide info only, don't pay
- User says "I need to pay bills" → Ask which bill type, amount, and account number
- User checks balance → Don't suggest making payments

#### Bill Type Providers

The system integrates with mock providers for testing:

| Bill Type | Example Providers | Account Number Format |
|-----------|------------------|----------------------|
| ELECTRICITY | PowerCo Electric, Ikeja Electric | Meter number (10-11 digits) |
| WATER | AquaFlow Water, Lagos Water | Account number (alphanumeric) |
| AIRTIME | MTN, Glo, Airtel, 9mobile | Phone number (11 digits) |
| DATA | MTN Data, Glo Data, Airtel Data | Phone number (11 digits) |
| CABLE_TV | DSTV, GOtv, Startimes | Smartcard/Account number |

## How It Works

### 1. User Query
User asks a natural language question:
```
"How much did I spend on food last month?"
```

### 2. AI Model Detection
GPT-4 Turbo analyzes the query and detects it needs transaction data. It automatically decides to call the `queryTransactions` function with appropriate parameters:
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "category": "FOOD",
  "timePeriod": "last month"
}
```

### 3. Function Execution
Spring AI executes the `TransactionQueryFunction.apply()` method:
1. Parses the time period into date range
2. Queries the database for matching transactions
3. Calculates totals and breakdowns
4. Formats the result

### 4. Response Generation
The AI receives the function result and incorporates it into a natural response:
```
"You spent ₦30,000.00 on food last month across 12 transactions.
This is part of your total expenses of ₦180,000.00 for the period."
```

## Implementation Details

### Function Registration

All functions are automatically registered in `OpenAIConfig.java`:

```java
// Read-only functions
FunctionCallback transactionQueryCallback = FunctionCallbackWrapper.builder(transactionQueryFunction)
    .withName("queryTransactions")
    .withDescription("Query and analyze user transactions...")
    .withInputType(TransactionQueryRequest.class)
    .build();

FunctionCallback statementGeneratorCallback = FunctionCallbackWrapper.builder(statementGeneratorFunction)
    .withName("generateStatement")
    .withDescription("Generate a detailed account statement...")
    .withInputType(StatementGenerateRequest.class)
    .build();

FunctionCallback savingsCalculatorCallback = FunctionCallbackWrapper.builder(savingsCalculatorFunction)
    .withName("calculateSavingsProjection")
    .withDescription("Calculate savings projections with compound interest...")
    .withInputType(SavingsCalculatorRequest.class)
    .build();

FunctionCallback budgetAssistantCallback = FunctionCallbackWrapper.builder(budgetAssistantFunction)
    .withName("createBudget")
    .withDescription("Create a personalized monthly budget based on user's income and spending history...")
    .withInputType(BudgetRequest.class)
    .build();

// ⚠️ Transaction executor functions (CRITICAL: Execute real financial transactions)
FunctionCallback sendMoneyCallback = FunctionCallbackWrapper.builder(sendMoneyFunction)
    .withName("sendMoney")
    .withDescription("⚠️ CRITICAL: Send money to another user. This executes REAL financial transactions. " +
        "ONLY use when the user EXPLICITLY requests to send money or transfer funds...")
    .withInputType(SendMoneyRequest.class)
    .build();

FunctionCallback payBillCallback = FunctionCallbackWrapper.builder(payBillFunction)
    .withName("payBill")
    .withDescription("⚠️ CRITICAL: Pay bills for electricity, water, airtime, data, or cable TV. " +
        "This executes REAL financial transactions. ONLY use when the user EXPLICITLY requests to pay a bill...")
    .withInputType(PayBillRequest.class)
    .build();

OpenAiChatOptions options = OpenAiChatOptions.builder()
    .withModel("gpt-4-turbo")
    .withTemperature(0.7)
    .withMaxTokens(1000)
    .withFunctionCallbacks(List.of(
        transactionQueryCallback,
        statementGeneratorCallback,
        savingsCalculatorCallback,
        budgetAssistantCallback,
        sendMoneyCallback,
        payBillCallback))
    .build();
```

### JSON Schema Generation

Spring AI automatically generates the JSON schema from the `TransactionQueryRequest` DTO using Jackson annotations:

- `@JsonProperty` - marks fields and specifies if required
- `@JsonPropertyDescription` - provides field descriptions to the AI model

### Time Period Parsing

The `TimePeriodParser` utility converts natural language to date ranges:

```java
TimePeriodParser.DateRange range = TimePeriodParser.parse("last 30 days");
// Returns: start = 30 days ago, end = now
```

Supported patterns:
- Fixed periods: today, yesterday, this week, last month, etc.
- Relative periods: "last N days/weeks/months" (regex-based parsing)
- Default: all time if unrecognized

## Usage Examples

### Example 1: General Spending Query
**User:** "What are my expenses this month?"

**AI Function Call:**
```json
{
  "userId": "user-uuid",
  "category": null,
  "timePeriod": "this month"
}
```

**Result:** Summary of all transactions for current month with category breakdown

### Example 2: Category-Specific Query
**User:** "How much did I spend on bills last week?"

**AI Function Call:**
```json
{
  "userId": "user-uuid",
  "category": "BILL_PAYMENT",
  "timePeriod": "last week"
}
```

**Result:** Only bill payment transactions from last week

### Example 3: Custom Time Range
**User:** "Show my transportation expenses for the last 3 months"

**AI Function Call:**
```json
{
  "userId": "user-uuid",
  "category": "TRANSPORT",
  "timePeriod": "last 3 months"
}
```

**Result:** Transportation transactions from last 90 days

## Adding New Functions

To add a new Spring AI function:

1. **Create Function Class:**
```java
@Component("myFunction")
@Description("What this function does")
public class MyFunction implements Function<MyRequest, String> {
    @Override
    public String apply(MyRequest request) {
        // Implementation
    }
}
```

2. **Create Request DTO:**
```java
@Data
public class MyRequest {
    @JsonProperty(required = true)
    @JsonPropertyDescription("Parameter description")
    private String param;
}
```

3. **Register in OpenAIConfig:**
```java
FunctionCallback callback = FunctionCallbackWrapper.builder(myFunction)
    .withName("myFunction")
    .withDescription("Function description")
    .withInputType(MyRequest.class)
    .build();
```

4. **Add to ChatOptions:**
```java
.withFunctionCallbacks(List.of(existingCallbacks, callback))
```

## Best Practices

1. **Descriptive Annotations:** Use detailed `@JsonPropertyDescription` to help the AI understand when and how to use parameters

2. **Return Formatted Strings:** Return human-readable strings rather than JSON objects for better AI integration

3. **Error Handling:** Always handle errors gracefully and return informative error messages

4. **Logging:** Log function calls for debugging and monitoring

5. **Validation:** Validate all inputs in the function implementation

6. **Performance:** Keep functions fast (<2 seconds) to avoid timeout issues

7. **Idempotent:** Functions should be idempotent - calling multiple times with same params yields same result

## Debugging

Enable debug logging to see function calls:

```yaml
logging:
  level:
    org.springframework.ai: DEBUG
    com.fulus.ai.assistant.function: DEBUG
```

Look for log messages:
- "Executing transaction query: userId=..."
- "Parsed date range: ... to ..."
- "Registered function callback: queryTransactions"

## Testing

Test the function directly:
```java
@Test
void testTransactionQueryFunction() {
    TransactionQueryRequest request = TransactionQueryRequest.builder()
        .userId("test-uuid")
        .category("FOOD")
        .timePeriod("last 30 days")
        .build();

    String result = transactionQueryFunction.apply(request);

    assertThat(result).contains("Transaction Summary");
}
```

Test via AI:
```java
String response = chatClient.prompt()
    .user("How much did I spend on food last month?")
    .call()
    .content();

// The AI should automatically call queryTransactions and incorporate the result
```

## Monitoring

Monitor function usage:
- Check application logs for function execution
- Track OpenAI token usage (function calls consume tokens)
- Monitor response times
- Alert on function failures

## Security Considerations

### General Security

1. **User Context:** Always validate userId matches the authenticated user
2. **Data Access:** Functions should respect user permissions
3. **Rate Limiting:** Consider rate limiting function calls per user
4. **Input Validation:** Sanitize all inputs to prevent injection attacks
5. **Audit Logging:** Log all function executions for audit trail

### Transaction Executor Functions (SendMoney & PayBill) ⚠️

**CRITICAL SECURITY REQUIREMENTS:**

#### 1. Confirmation Required
- **NEVER** execute sendMoney or payBill without explicit user confirmation
- The AI system prompt includes strict rules to only use these when user EXPLICITLY requests
- Always confirm: amount, recipient/bill type, and account number
- Consider implementing a two-factor authentication step for large amounts

#### 2. Transaction Limits
- **Maximum per transaction:** ₦1,000,000
- **Recommended:** Implement daily/monthly limits per user
- **Consider:** Velocity checks (number of transactions per time period)
- **Alert:** Flag suspicious patterns (many small transactions, rapid succession)

#### 3. Audit Trail
- All transaction attempts logged with WARNING level
- Log includes: userId, recipient/billType, amount, timestamp, result
- Store logs in secure, immutable storage for forensic analysis
- Implement real-time monitoring and alerts for:
  - Failed transactions (potential fraud)
  - Large amount transfers
  - Unusual recipient patterns
  - Rapid transaction sequences

#### 4. Authorization
- Verify user is authenticated before ANY transaction
- Check user session is valid and not expired
- Implement OAuth2/JWT token validation
- Consider device fingerprinting for additional security

#### 5. Validation
- **Amount validation:**
  - Positive numbers only
  - Reasonable decimal places (2 for currency)
  - Within allowed limits
  - Sufficient balance checks
- **Recipient validation:**
  - Exists in system
  - Not on blocklist
  - Not the same as sender (no self-transfers)
  - Account is active (not suspended/closed)
- **Bill validation:**
  - Valid bill type enum
  - Valid account number format
  - Provider exists and is active

#### 6. Error Handling
- **NEVER** expose sensitive data in error messages
- Don't reveal whether accounts exist (prevents enumeration attacks)
- Use generic error messages for security failures
- Log detailed errors server-side only
- Example: "Transaction failed" instead of "Insufficient balance: ₦100 short"

#### 7. Fraud Prevention
- Implement velocity checks (max N transactions per hour)
- Flag new accounts attempting large transfers
- Require additional verification for first-time recipients
- Monitor for circular transaction patterns
- Implement machine learning anomaly detection

#### 8. Rollback & Recovery
- Use @Transactional annotations for atomicity
- Implement compensating transactions for failed provider calls
- Keep transaction state machine (PENDING → PROCESSING → COMPLETED/FAILED)
- Support manual review and reversal for disputed transactions

#### 9. Provider Integration Security
- Secure API keys in environment variables (not in code)
- Use HTTPS for all provider communications
- Implement request signing for provider APIs
- Validate provider responses (don't trust external data)
- Implement circuit breakers for provider failures
- Handle provider timeouts gracefully

#### 10. Testing Considerations
- **NEVER** test transaction functions in production with real money
- Use mock providers for development and testing
- Implement feature flags to disable real transactions
- Use separate test accounts with isolated balances
- Clear transaction history between test runs

#### 11. Compliance & Regulations
- Implement KYC (Know Your Customer) verification
- Comply with AML (Anti-Money Laundering) regulations
- Report suspicious transactions to relevant authorities
- Maintain transaction records per regulatory requirements (typically 7+ years)
- Implement transaction limits based on KYC verification levels

#### 12. AI-Specific Security
- **Prompt injection protection:** Sanitize user inputs to prevent AI manipulation
- **Function call validation:** Verify function parameters before execution
- **Context verification:** Ensure transaction context matches conversation history
- **Rate limit AI function calls:** Prevent automated abuse via AI API
- **Monitor AI behavior:** Alert on unusual function calling patterns

### Recommended Implementation Checklist

Before deploying transaction executor functions to production:

- [ ] Implement user authentication and authorization
- [ ] Add two-factor authentication for transactions
- [ ] Set up comprehensive audit logging
- [ ] Configure transaction limits (per-transaction, daily, monthly)
- [ ] Implement velocity checks and fraud detection
- [ ] Set up real-time monitoring and alerts
- [ ] Test rollback scenarios thoroughly
- [ ] Conduct security penetration testing
- [ ] Review and comply with financial regulations
- [ ] Implement circuit breakers for external providers
- [ ] Set up disaster recovery procedures
- [ ] Train support team on transaction dispute handling
- [ ] Document incident response procedures
- [ ] Implement automated anomaly detection
- [ ] Set up secure backup of transaction logs

---

**Note:** This function calling system requires OpenAI models that support function calling (GPT-3.5-Turbo, GPT-4, GPT-4-Turbo, etc.)
