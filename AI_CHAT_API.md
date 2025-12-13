# AI Financial Assistant Chat API

This document describes the REST API endpoints for the Fulus Pay AI Financial Assistant.

## Overview

The AI Financial Assistant provides a conversational interface for users to:
- Query transaction history and spending patterns
- Generate account statements
- Calculate savings projections
- Create personalized budgets
- Execute money transfers (with confirmation)
- Pay bills (with confirmation)

The assistant uses OpenAI GPT-4 Turbo with automatic function calling and PostgreSQL-backed conversation memory.

## Base URL

```
http://localhost:8080/api/v1/chat
```

## Endpoints

### 1. Send Message to AI Assistant (Blocking)

Send a message to the AI assistant and receive a conversational response.

**Endpoint:** `POST /api/v1/chat`

**Request Body:**

```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "How much did I spend on food last month?",
  "useMemory": true,
  "includeHistory": false
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | String (UUID) | Yes | The user's unique identifier |
| message | String | Yes | The user's message/question (1-2000 characters) |
| useMemory | Boolean | No | Whether to use conversation memory (default: true) |
| includeHistory | Boolean | No | Include conversation history in response for debugging (default: false) |

**Response (Success):**

```json
{
  "success": true,
  "message": "Response generated successfully",
  "response": "Based on your transactions, you spent ₦35,000 on food last month across 12 transactions. Here's the breakdown:\n\n• Groceries: ₦18,000\n• Restaurants: ₦12,000\n• Fast food: ₦5,000\n\nThis is 23% of your total spending for the month.",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "messageCount": 5,
  "timestamp": "2025-12-10T14:30:00"
}
```

**Response (Error):**

```json
{
  "success": false,
  "message": "Invalid request: User ID is required",
  "timestamp": "2025-12-10T14:30:00"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| success | Boolean | Whether the request was successful |
| message | String | Status message or error description |
| response | String | The AI assistant's response |
| conversationId | String | The conversation ID (typically the user ID) |
| messageCount | Integer | Number of messages in conversation history (if includeHistory=true) |
| history | Array | Conversation history (if includeHistory=true) |
| timestamp | DateTime | When the response was generated |

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "How much did I spend on food last month?"
  }'
```

### 2. Clear Conversation History

Clear all conversation history for a user. Useful for starting fresh or testing.

**Endpoint:** `DELETE /api/v1/chat/history/{userId}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| userId | String (UUID) | The user's unique identifier |

**Response:**

```json
{
  "success": true,
  "message": "Conversation history cleared successfully",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2025-12-10T14:30:00"
}
```

**Example cURL:**

```bash
curl -X DELETE http://localhost:8080/api/v1/chat/history/123e4567-e89b-12d3-a456-426614174000
```

### 3. Stream AI Response (SSE - POST)

Stream AI response tokens in real-time using Server-Sent Events (SSE).

**Endpoint:** `POST /api/v1/chat/stream`

**Request Body:**

```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "How much did I spend on food last month?"
}
```

**Response Format:** `text/event-stream`

**SSE Events:**

The endpoint emits Server-Sent Events with the following format:

```
id: 1702392000000
event: message
data: Based

id: 1702392000010
event: message
data:  on

id: 1702392000020
event: message
data:  your

id: 1702392000030
event: message
data:  transactions

...

id: 1702392005000
event: complete
data: [DONE]
```

**Event Types:**

| Event | Description | Data |
|-------|-------------|------|
| message | Token from AI response | Text token |
| complete | Stream completed successfully | "[DONE]" |
| error | Error occurred during streaming | Error message |

**Example JavaScript Client:**

```javascript
const eventSource = new EventSource('http://localhost:8080/api/v1/chat/stream', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    userId: '123e4567-e89b-12d3-a456-426614174000',
    message: 'How much did I spend on food last month?'
  })
});

eventSource.addEventListener('message', (event) => {
  console.log('Token:', event.data);
  // Append token to UI
});

eventSource.addEventListener('complete', (event) => {
  console.log('Stream completed');
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  console.error('Stream error:', event.data);
  eventSource.close();
});
```

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "How much did I spend on food last month?"
  }' \
  --no-buffer
```

**Features:**
- ✅ Real-time token streaming as AI generates response
- ✅ Automatic retry with exponential backoff (3 attempts)
- ✅ Backpressure handling (1000 token buffer)
- ✅ 30-second timeout per token
- ✅ Conversation memory maintained during streaming
- ✅ Function calls handled mid-stream
- ✅ Graceful error recovery

### 4. Stream AI Response (SSE - GET)

Stream AI response using GET request with query parameters. Includes progress indicators.

**Endpoint:** `GET /api/v1/chat/stream/{userId}?message={message}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| userId | String (UUID) | The user's unique identifier |

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| message | String | The user's message (URL-encoded) |

**Response Format:** `text/event-stream`

**Example cURL:**

```bash
curl -X GET "http://localhost:8080/api/v1/chat/stream/123e4567-e89b-12d3-a456-426614174000?message=How%20much%20did%20I%20spend%20this%20month" \
  -H "Accept: text/event-stream" \
  --no-buffer
```

**Example Browser:**

```javascript
const userId = '123e4567-e89b-12d3-a456-426614174000';
const message = encodeURIComponent('How much did I spend this month?');
const eventSource = new EventSource(
  `http://localhost:8080/api/v1/chat/stream/${userId}?message=${message}`
);

let fullResponse = '';

eventSource.addEventListener('message', (event) => {
  fullResponse += event.data;
  document.getElementById('response').innerText = fullResponse;
});

eventSource.addEventListener('complete', (event) => {
  console.log('Stream completed');
  eventSource.close();
});
```

### 5. Stream AI Response (Plain Text)

Stream AI response as plain text without SSE formatting. Simpler for basic clients.

**Endpoint:** `POST /api/v1/chat/stream/text`

**Request Body:**

```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Create a budget for my ₦200,000 salary"
}
```

**Response Format:** `text/plain`

**Response:** Streaming plain text tokens without SSE headers

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/v1/chat/stream/text \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "Create a budget for my ₦200,000 salary"
  }' \
  --no-buffer
```

**Use Case:** Simple command-line tools, scripts, or clients that don't support SSE

### 6. Get Conversation History (Debug)

Retrieve the conversation history for a user. Useful for debugging and testing.

**Endpoint:** `GET /api/v1/chat/history/{userId}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| userId | String (UUID) | The user's unique identifier |

**Response:**

```json
{
  "success": true,
  "message": "Conversation history retrieved successfully",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "messageCount": 8,
  "history": [
    {
      "role": "user",
      "content": "How much did I spend on food last month?",
      "timestamp": "2025-12-10T14:30:00"
    },
    {
      "role": "assistant",
      "content": "Based on your transactions, you spent ₦35,000 on food...",
      "timestamp": "2025-12-10T14:30:05"
    }
  ],
  "timestamp": "2025-12-10T14:35:00"
}
```

**Example cURL:**

```bash
curl -X GET http://localhost:8080/api/v1/chat/history/123e4567-e89b-12d3-a456-426614174000
```

### 7. Health Check

Check if the AI Chat Service is running.

**Endpoint:** `GET /api/v1/chat/health`

**Response:**

```
AI Chat Service is running
```

**Example cURL:**

```bash
curl -X GET http://localhost:8080/api/v1/chat/health
```

## Streaming vs Blocking

### When to Use Streaming

**Use Streaming (SSE) when:**
- Building real-time chat interfaces
- Want to show response as it's being generated
- Need typing indicator effect
- Long responses expected (budgets, statements)
- Mobile or web applications with live UI updates

**Use Blocking (standard POST) when:**
- Simple request/response pattern needed
- Batch processing
- Backend-to-backend communication
- Testing or debugging
- Simple command-line tools

### Streaming Benefits

1. **Better UX**: Users see response immediately, not after full generation
2. **Perceived Performance**: Feels faster even if total time is same
3. **Function Call Visibility**: Can show progress when functions are being called
4. **Cancellation**: Client can close connection to stop generation
5. **Reduced Timeout Risk**: Long responses don't hit HTTP timeouts

### Streaming Technical Details

**Backpressure Handling:**
- Buffer capacity: 1000 tokens
- Overflow strategy: Drop oldest tokens, log warning
- Prevents memory overflow on slow clients

**Error Recovery:**
- Automatic retry: 3 attempts with exponential backoff (1s, 2s, 4s)
- Timeout: 30 seconds per token
- Graceful degradation: Emits error message and completes
- Chat memory still saved on partial responses

**Memory Management:**
- Tokens accumulated during streaming
- Complete response saved to chat memory on completion
- Works same as blocking mode for conversation history
- Function calls resolved mid-stream

**Performance:**
- Non-blocking: Uses Project Reactor (Flux)
- Bounded elastic scheduler: Prevents thread pool exhaustion
- Efficient for concurrent users

## Example Conversations

### Example 1: Query Spending (Blocking)

**Request:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "How much have I spent this month?"
}
```

**Response:**
```json
{
  "success": true,
  "response": "This month, you've spent a total of ₦145,000 across 42 transactions.\n\n📊 Breakdown by category:\n• Food: ₦35,000 (24%)\n• Transport: ₦28,000 (19%)\n• Bills: ₦25,000 (17%)\n• Shopping: ₦20,000 (14%)\n• Entertainment: ₦15,000 (10%)\n• Other: ₦22,000 (15%)\n\nYou're averaging about ₦5,000 per day. Need help creating a budget?"
}
```

### Example 1b: Query Spending (Streaming)

**Request (SSE):**
```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "How much have I spent this month?"
  }' \
  --no-buffer
```

**Response (Streamed):**
```
event: message
data: This

event: message
data:  month

event: message
data: ,

event: message
data:  you

event: message
data: 've

event: message
data:  spent

event: message
data:  a

event: message
data:  total

event: message
data:  of

event: message
data:  ₦

event: message
data: 145

event: message
data: ,

event: message
data: 000

...

event: complete
data: [DONE]
```

**Client receives tokens in real-time and can render them progressively:**

```
This month, you've spent a total of ₦145,000 across 42 transactions.

📊 Breakdown by category:
• Food: ₦35,000 (24%)
• Transport: ₦28,000 (19%)
...
```

### Example 2: Create Budget (Streaming)

**Request:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Help me create a budget. My monthly income is ₦200,000"
}
```

**Response:**
```json
{
  "success": true,
  "response": "💰 PERSONALIZED BUDGET PLAN\n══════════════════════════════════════════════════════════\n\nHi John! Here's your budget for 2025-12\nMonthly Income: ₦200,000.00\n\n📊 BUDGET BREAKDOWN (50/30/20 Rule)\n──────────────────────────────────────────────────────────\nNeeds (50%):        ₦100,000.00\nWants (30%):        ₦60,000.00\nSavings (20%):      ₦40,000.00 💎\n\n[... full budget breakdown ...]\n\n✅ Budget saved successfully!"
}
```

### Example 3: Send Money

**Request:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Send ₦5,000 to Jane Doe"
}
```

**Response (Confirmation Request):**
```json
{
  "success": true,
  "response": "I can help you send ₦5,000 to Jane Doe. Let me confirm the details:\n\n• Amount: ₦5,000.00\n• Recipient: Jane Doe (08012345678)\n• Your new balance will be: ₦45,000.00\n\nIs this correct? Please confirm if I should proceed with this transfer."
}
```

**Follow-up Request:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Yes, proceed"
}
```

**Response (After Execution):**
```json
{
  "success": true,
  "response": "✅ MONEY SENT SUCCESSFULLY!\n═══════════════════════════════════════\n\nAmount:         ₦5,000.00\nRecipient:      Jane Doe\nPhone:          08012345678\n\nTransaction ID: 456e7890-e89b-12d3-a456-426614174001\nReference:      TXN-ABC123DEF456\nNew Balance:    ₦45,000.00\n\nThe recipient has been notified of this transfer.\nThank you for using Fulus Pay! 💚"
}
```

### Example 4: Pay Bill

**Request:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Pay ₦3,000 electricity bill for meter 1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "response": "✅ BILL PAYMENT SUCCESSFUL!\n═══════════════════════════════════════\n\nBill Type:      Electricity\nProvider:       PowerCo Electric\nAccount:        ****7890\nAmount:         ₦3,000.00\n\nPAYMENT DETAILS\n───────────────────────────────────────\nPayment Token:  ELEC-A1B2C3D4E5F6\nTransaction ID: 789e0123-e89b-12d3-a456-426614174002\n\n⚡ Important: Please save your payment token for future reference."
}
```

## Conversation Memory

The AI assistant maintains conversation history for each user:

- **Storage**: PostgreSQL database
- **Window Size**: Last 20 messages per conversation
- **Cache TTL**: 1 hour in-memory cache
- **Auto-Pruning**: Older messages automatically removed
- **Persistence**: Survives application restarts

This enables multi-turn conversations:

```
User: "How much did I spend last month?"
AI: [Provides spending summary]

User: "What about the month before?"
AI: [Understands context, provides previous month's data]

User: "Create a budget based on these two months"
AI: [Uses conversation context to understand "these two months"]
```

## Available AI Functions

The AI assistant has access to 6 function tools:

1. **queryTransactions** - Query and analyze transaction history
2. **generateStatement** - Generate formatted account statements
3. **calculateSavingsProjection** - Calculate compound interest projections
4. **createBudget** - Create personalized monthly budgets
5. **sendMoney** ⚠️ - Execute money transfers (requires confirmation)
6. **payBill** ⚠️ - Execute bill payments (requires confirmation)

The AI automatically calls these functions based on user intent. See [SPRING_AI_FUNCTIONS.md](./SPRING_AI_FUNCTIONS.md) for detailed function documentation.

## Nigerian Context Awareness

The AI assistant is optimized for Nigerian users:

- **Currency**: All amounts in Nigerian Naira (₦)
- **Bill Payments**: Electricity (PHCN/Disco), Airtime (MTN/Glo/Airtel/9mobile), Data, Cable TV (DSTV/GOtv)
- **Transport**: Acknowledges high transport costs in Nigerian cities
- **Language**: Uses Nigerian English (e.g., "NEPA bill", "recharge airtime")
- **Financial Context**: Understands irregular income, utility challenges, savings culture

## Error Handling

### Validation Errors (400 Bad Request)

```json
{
  "success": false,
  "message": "Invalid request: Message cannot be empty",
  "timestamp": "2025-12-10T14:30:00"
}
```

### Server Errors (500 Internal Server Error)

```json
{
  "success": false,
  "message": "An error occurred while processing your request. Please try again later.",
  "timestamp": "2025-12-10T14:30:00"
}
```

### User Not Found

```json
{
  "success": false,
  "message": "User not found. Please ensure you're logged in.",
  "timestamp": "2025-12-10T14:30:00"
}
```

## Retry Logic

The service includes automatic retry for transient failures:

- **Max Attempts**: 3
- **Backoff**: Exponential (1s, 2s, 4s)
- **Retry Conditions**: Network errors, API timeouts, rate limits

## Rate Limiting

**Recommended limits:**
- 60 requests per minute per user
- 1000 requests per hour per user

*(Implementation note: Rate limiting should be added at the API Gateway or application level)*

## Security Considerations

1. **Authentication**: All endpoints should require user authentication (JWT/OAuth2)
2. **Authorization**: Verify userId matches authenticated user
3. **Input Validation**: All inputs validated (max length, format)
4. **Transaction Confirmation**: Money transfers and bill payments require explicit confirmation
5. **Audit Logging**: All chat interactions logged for compliance
6. **Data Privacy**: Conversation history contains sensitive financial data
7. **HTTPS**: All communication should use TLS/SSL

## Performance

- **Average Response Time**: 2-5 seconds (depends on function calls)
- **Concurrent Users**: Scales horizontally with Spring Boot
- **Database**: PostgreSQL with connection pooling (HikariCP)
- **Caching**: In-memory conversation cache (1-hour TTL)

## Testing

### Using cURL

```bash
# Health check
curl -X GET http://localhost:8080/api/v1/chat/health

# Send message
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "message": "How much did I spend this month?"
  }'

# Get history
curl -X GET http://localhost:8080/api/v1/chat/history/123e4567-e89b-12d3-a456-426614174000

# Clear history
curl -X DELETE http://localhost:8080/api/v1/chat/history/123e4567-e89b-12d3-a456-426614174000
```

### Using Postman

1. Import the collection from `postman/AI_Chat_API.postman_collection.json`
2. Set environment variable `BASE_URL` to `http://localhost:8080`
3. Set environment variable `USER_ID` to a valid UUID
4. Run the requests

## Streaming Client Examples

### JavaScript (Browser)

```html
<!DOCTYPE html>
<html>
<head>
    <title>Fulus Pay AI Chat</title>
    <style>
        #chat-box {
            border: 1px solid #ccc;
            padding: 20px;
            height: 400px;
            overflow-y: auto;
            white-space: pre-wrap;
            font-family: monospace;
        }
        #loading {
            display: none;
            color: #666;
        }
    </style>
</head>
<body>
    <h1>Fulus Pay AI Assistant</h1>
    <div id="loading">⏳ Processing...</div>
    <div id="chat-box"></div>
    <input type="text" id="message-input" placeholder="Ask me anything..." style="width: 80%">
    <button onclick="sendMessage()">Send</button>

    <script>
        const userId = '123e4567-e89b-12d3-a456-426614174000';
        let eventSource = null;

        function sendMessage() {
            const message = document.getElementById('message-input').value;
            if (!message) return;

            const chatBox = document.getElementById('chat-box');
            const loading = document.getElementById('loading');

            // Show user message
            chatBox.innerHTML += `\n\nYou: ${message}\n\nAssistant: `;
            loading.style.display = 'block';

            // Create SSE connection
            const encodedMessage = encodeURIComponent(message);
            eventSource = new EventSource(
                `http://localhost:8080/api/v1/chat/stream/${userId}?message=${encodedMessage}`
            );

            eventSource.addEventListener('message', (event) => {
                loading.style.display = 'none';
                chatBox.innerHTML += event.data;
                chatBox.scrollTop = chatBox.scrollHeight;
            });

            eventSource.addEventListener('complete', (event) => {
                console.log('Stream completed');
                eventSource.close();
                loading.style.display = 'none';
            });

            eventSource.addEventListener('error', (event) => {
                console.error('Stream error');
                loading.style.display = 'none';
                eventSource.close();
            });

            document.getElementById('message-input').value = '';
        }

        // Allow Enter key to send
        document.getElementById('message-input').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });
    </script>
</body>
</html>
```

### Python Client

```python
import requests
import json

def stream_chat(user_id: str, message: str):
    """Stream chat response from Fulus Pay AI"""
    url = "http://localhost:8080/api/v1/chat/stream"
    headers = {
        "Content-Type": "application/json",
        "Accept": "text/event-stream"
    }
    data = {
        "userId": user_id,
        "message": message
    }

    response = requests.post(url, json=data, headers=headers, stream=True)

    print(f"You: {message}\n\nAssistant: ", end='', flush=True)

    for line in response.iter_lines():
        if line:
            decoded = line.decode('utf-8')
            if decoded.startswith('data: '):
                token = decoded[6:]  # Remove 'data: ' prefix
                if token == '[DONE]':
                    print('\n')
                    break
                print(token, end='', flush=True)

# Usage
if __name__ == "__main__":
    user_id = "123e4567-e89b-12d3-a456-426614174000"
    stream_chat(user_id, "How much did I spend on food last month?")
```

### Node.js Client

```javascript
const fetch = require('node-fetch');

async function streamChat(userId, message) {
    const response = await fetch('http://localhost:8080/api/v1/chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
        },
        body: JSON.stringify({ userId, message })
    });

    console.log(`You: ${message}\n\nAssistant: `);

    const reader = response.body;
    let buffer = '';

    reader.on('data', (chunk) => {
        buffer += chunk.toString();
        const lines = buffer.split('\n');
        buffer = lines.pop(); // Keep incomplete line

        for (const line of lines) {
            if (line.startsWith('data: ')) {
                const token = line.substring(6);
                if (token === '[DONE]') {
                    console.log('\n');
                    return;
                }
                process.stdout.write(token);
            }
        }
    });
}

// Usage
const userId = '123e4567-e89b-12d3-a456-426614174000';
streamChat(userId, 'Create a budget for my ₦200,000 salary');
```

### React Component

```jsx
import React, { useState } from 'react';

function ChatComponent({ userId }) {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [streaming, setStreaming] = useState(false);
    const [currentResponse, setCurrentResponse] = useState('');

    const sendMessage = async () => {
        if (!input.trim()) return;

        // Add user message
        const userMessage = { role: 'user', content: input };
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setStreaming(true);
        setCurrentResponse('');

        // Create SSE connection
        const encodedMessage = encodeURIComponent(input);
        const eventSource = new EventSource(
            `http://localhost:8080/api/v1/chat/stream/${userId}?message=${encodedMessage}`
        );

        eventSource.addEventListener('message', (event) => {
            setCurrentResponse(prev => prev + event.data);
        });

        eventSource.addEventListener('complete', (event) => {
            setMessages(prev => [...prev, { role: 'assistant', content: currentResponse }]);
            setCurrentResponse('');
            setStreaming(false);
            eventSource.close();
        });

        eventSource.addEventListener('error', (event) => {
            setStreaming(false);
            eventSource.close();
        });
    };

    return (
        <div className="chat-container">
            <div className="messages">
                {messages.map((msg, idx) => (
                    <div key={idx} className={`message ${msg.role}`}>
                        <strong>{msg.role === 'user' ? 'You' : 'Assistant'}:</strong>
                        <p>{msg.content}</p>
                    </div>
                ))}
                {streaming && (
                    <div className="message assistant streaming">
                        <strong>Assistant:</strong>
                        <p>{currentResponse}<span className="cursor">|</span></p>
                    </div>
                )}
            </div>
            <div className="input-area">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                    placeholder="Ask me anything..."
                    disabled={streaming}
                />
                <button onClick={sendMessage} disabled={streaming}>
                    {streaming ? 'Sending...' : 'Send'}
                </button>
            </div>
        </div>
    );
}

export default ChatComponent;
```

## Troubleshooting

### Issue: "User not found"
- Verify the userId exists in the `users` table
- Check UUID format is correct

### Issue: Slow responses
- Check OpenAI API latency
- Review database query performance
- Check if multiple function calls are being made

### Issue: Function not being called
- Verify function is registered in OpenAIConfig
- Check function description is clear
- Review OpenAI API logs

### Issue: Conversation history not persisting
- Verify PostgreSQL connection
- Check `conversations` and `chat_messages` tables
- Review ChatMemoryProvider logs

### Issue: Streaming connection closes immediately
- Check CORS settings for SSE
- Verify `Accept: text/event-stream` header
- Ensure client doesn't timeout waiting for first token
- Check proxy/load balancer SSE support

### Issue: Tokens arriving slowly or delayed
- Check OpenAI API latency
- Review network bandwidth
- Verify no intermediate proxies buffering
- Check backpressure warnings in logs

### Issue: Missing tokens in stream
- Check backpressure buffer size (1000 tokens)
- Review dropped token warnings in logs
- Slow client may trigger overflow
- Increase buffer size if needed

## Additional Resources

- [Spring AI Functions Documentation](./SPRING_AI_FUNCTIONS.md)
- [OpenAI Function Calling Guide](https://platform.openai.com/docs/guides/function-calling)
- [PostgreSQL Chat Memory Implementation](./src/main/java/com/fulus/ai/assistant/service/chat/)
- [API Configuration](./src/main/resources/application.yml)
