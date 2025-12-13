# Transaction API Documentation

The Fulus Pay AI Assistant provides comprehensive transaction management with pagination, filtering, analytics, and export capabilities.

## Overview

The Transaction API enables:
1. Paginated transaction history with filtering
2. Transaction summaries with category-wise analytics
3. Export to CSV and PDF formats
4. Real-time balance tracking

## Base URL

```
http://localhost:8080/api/v1/transactions
```

## Endpoints

### 1. Get Paginated Transactions

**Endpoint:** `GET /api/v1/transactions/{userId}`

Retrieve paginated transaction history with optional filtering by category and date range.

**Path Parameters:**
- `userId` (required): User UUID

**Query Parameters:**
- `page` (optional): Page number, 0-indexed (default: `0`)
- `size` (optional): Page size, 1-100 (default: `20`)
- `category` (optional): Transaction category filter
- `fromDate` (optional): Start date (ISO 8601 format)
- `toDate` (optional): End date (ISO 8601 format)

**Supported Categories:**
- `FOOD`
- `TRANSPORT`
- `ENTERTAINMENT`
- `SHOPPING`
- `BILLS`
- `HEALTHCARE`
- `EDUCATION`
- `INVESTMENT`
- `TRANSFER`
- `SALARY`
- `OTHER`

**Response:**
```json
{
  "transactions": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "userId": "user-uuid",
      "type": "DEBIT",
      "category": "FOOD",
      "amount": 5000.00,
      "description": "Lunch at restaurant",
      "balanceAfter": 95000.00,
      "reference": "TXN-20241211-ABC123",
      "status": "COMPLETED",
      "createdAt": "2024-12-11T14:30:22"
    }
  ],
  "currentPage": 0,
  "totalPages": 5,
  "totalElements": 100,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

**Example (cURL):**
```bash
# Get first page (20 transactions)
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000?page=0&size=20"

# Filter by category
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000?category=FOOD"

# Filter by date range
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000?fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59"

# Combined filters
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000?page=0&size=50&category=FOOD&fromDate=2024-11-01T00:00:00"
```

**Example (JavaScript/Fetch):**
```javascript
const userId = '123e4567-e89b-12d3-a456-426614174000';
const params = new URLSearchParams({
  page: 0,
  size: 20,
  category: 'FOOD',
  fromDate: '2024-11-01T00:00:00',
  toDate: '2024-11-30T23:59:59'
});

const response = await fetch(`http://localhost:8080/api/v1/transactions/${userId}?${params}`);
const data = await response.json();

console.log(`Total transactions: ${data.totalElements}`);
console.log(`Pages: ${data.totalPages}`);
data.transactions.forEach(txn => {
  console.log(`${txn.createdAt} - ${txn.description}: ₦${txn.amount}`);
});
```

**Example (Python):**
```python
import requests
from datetime import datetime

user_id = '123e4567-e89b-12d3-a456-426614174000'
params = {
    'page': 0,
    'size': 20,
    'category': 'FOOD',
    'fromDate': '2024-11-01T00:00:00',
    'toDate': '2024-11-30T23:59:59'
}

response = requests.get(f'http://localhost:8080/api/v1/transactions/{user_id}', params=params)
data = response.json()

print(f"Total transactions: {data['totalElements']}")
print(f"Current page: {data['currentPage']} of {data['totalPages']}")

for txn in data['transactions']:
    print(f"{txn['createdAt']} - {txn['description']}: ₦{txn['amount']}")
```

---

### 2. Get Transaction Summary

**Endpoint:** `GET /api/v1/transactions/{userId}/summary`

Get comprehensive spending summary with category-wise breakdown and analytics.

**Path Parameters:**
- `userId` (required): User UUID

**Query Parameters:**
- `period` (optional): Time period (default: `this_month`)
  - `this_month` - Current month
  - `last_month` - Previous month
  - `last_3_months` - Last 3 months
  - `last_6_months` - Last 6 months
  - `this_year` - Current year
  - `last_year` - Previous year
  - `all_time` - All transactions
  - `custom` - Custom date range (requires `fromDate` and `toDate`)
- `fromDate` (required if period=custom): Start date (ISO 8601)
- `toDate` (required if period=custom): End date (ISO 8601)

**Response:**
```json
{
  "startDate": "2024-12-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "period": "This Month",
  "totalIncome": 250000.00,
  "totalExpenses": 125000.00,
  "netAmount": 125000.00,
  "totalTransactions": 45,
  "incomeTransactions": 5,
  "expenseTransactions": 40,
  "categoryBreakdown": [
    {
      "category": "FOOD",
      "amount": 35000.00,
      "transactionCount": 12,
      "percentage": 28.0
    },
    {
      "category": "TRANSPORT",
      "amount": 25000.00,
      "transactionCount": 8,
      "percentage": 20.0
    },
    {
      "category": "BILLS",
      "amount": 20000.00,
      "transactionCount": 5,
      "percentage": 16.0
    }
  ],
  "topCategory": {
    "category": "FOOD",
    "amount": 35000.00,
    "transactionCount": 12,
    "percentage": 28.0
  }
}
```

**Example (cURL):**
```bash
# This month summary
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/summary?period=this_month"

# Last 3 months summary
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/summary?period=last_3_months"

# Custom period
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/summary?period=custom&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59"
```

**Example (JavaScript):**
```javascript
const userId = '123e4567-e89b-12d3-a456-426614174000';

// Get this month's summary
const response = await fetch(
  `http://localhost:8080/api/v1/transactions/${userId}/summary?period=this_month`
);
const summary = await response.json();

console.log(`Period: ${summary.period}`);
console.log(`Total Income: ₦${summary.totalIncome.toLocaleString()}`);
console.log(`Total Expenses: ₦${summary.totalExpenses.toLocaleString()}`);
console.log(`Net Amount: ₦${summary.netAmount.toLocaleString()}`);

console.log('\nCategory Breakdown:');
summary.categoryBreakdown.forEach(cat => {
  console.log(`${cat.category}: ₦${cat.amount.toLocaleString()} (${cat.percentage.toFixed(1)}%)`);
});

if (summary.topCategory) {
  console.log(`\nTop Spending Category: ${summary.topCategory.category} - ₦${summary.topCategory.amount.toLocaleString()}`);
}
```

**Example (Python):**
```python
import requests

user_id = '123e4567-e89b-12d3-a456-426614174000'

# Get last 3 months summary
response = requests.get(
    f'http://localhost:8080/api/v1/transactions/{user_id}/summary',
    params={'period': 'last_3_months'}
)
summary = response.json()

print(f"Period: {summary['period']}")
print(f"Total Income: ₦{summary['totalIncome']:,.2f}")
print(f"Total Expenses: ₦{summary['totalExpenses']:,.2f}")
print(f"Net Amount: ₦{summary['netAmount']:,.2f}")

print("\nCategory Breakdown:")
for cat in summary['categoryBreakdown']:
    print(f"  {cat['category']}: ₦{cat['amount']:,.2f} ({cat['percentage']:.1f}%)")

if summary['topCategory']:
    print(f"\nTop Category: {summary['topCategory']['category']}")
```

---

### 3. Export Transactions

**Endpoint:** `GET /api/v1/transactions/{userId}/export`

Export transactions as downloadable CSV or PDF file.

**Path Parameters:**
- `userId` (required): User UUID

**Query Parameters:**
- `format` (optional): Export format - `csv` or `pdf` (default: `csv`)
- `fromDate` (optional): Start date (ISO 8601)
- `toDate` (optional): End date (ISO 8601)

**Response:** Binary file (CSV or PDF)

**CSV Format:**
```csv
Date,Reference,Type,Category,Description,Amount,Balance After,Status
2024-12-11 14:30:22,TXN-20241211-ABC123,DEBIT,FOOD,Lunch at restaurant,₦5000.00,₦95000.00,COMPLETED
2024-12-10 09:15:30,TXN-20241210-XYZ789,CREDIT,SALARY,Monthly salary,₦250000.00,₦100000.00,COMPLETED
```

**PDF Format:**
- Professional formatted statement with header
- User account information
- Summary section (opening balance, total income, expenses, closing balance)
- Detailed transaction table with color coding
- Footer with generation timestamp

**Example (cURL):**
```bash
# Export as CSV
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/export?format=csv" \
  --output statement.csv

# Export as PDF
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/export?format=pdf" \
  --output statement.pdf

# Export specific date range
curl -X GET "http://localhost:8080/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000/export?format=pdf&fromDate=2024-11-01T00:00:00&toDate=2024-11-30T23:59:59" \
  --output november_statement.pdf
```

**Example (JavaScript):**
```javascript
const userId = '123e4567-e89b-12d3-a456-426614174000';

// Export as PDF
async function exportPDF() {
  const params = new URLSearchParams({
    format: 'pdf',
    fromDate: '2024-11-01T00:00:00',
    toDate: '2024-11-30T23:59:59'
  });

  const response = await fetch(
    `http://localhost:8080/api/v1/transactions/${userId}/export?${params}`
  );

  if (response.ok) {
    const blob = await response.blob();

    // Download file
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'statement.pdf';
    link.click();
    window.URL.revokeObjectURL(url);
  }
}

// Export as CSV
async function exportCSV() {
  const params = new URLSearchParams({
    format: 'csv'
  });

  const response = await fetch(
    `http://localhost:8080/api/v1/transactions/${userId}/export?${params}`
  );

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'statement.csv';
  link.click();
  window.URL.revokeObjectURL(url);
}
```

**Example (Python):**
```python
import requests
from datetime import datetime

user_id = '123e4567-e89b-12d3-a456-426614174000'

# Export as PDF
params = {
    'format': 'pdf',
    'fromDate': '2024-11-01T00:00:00',
    'toDate': '2024-11-30T23:59:59'
}

response = requests.get(
    f'http://localhost:8080/api/v1/transactions/{user_id}/export',
    params=params
)

if response.status_code == 200:
    with open('statement.pdf', 'wb') as f:
        f.write(response.content)
    print("PDF exported successfully")

# Export as CSV
params = {'format': 'csv'}
response = requests.get(
    f'http://localhost:8080/api/v1/transactions/{user_id}/export',
    params=params
)

if response.status_code == 200:
    with open('statement.csv', 'wb') as f:
        f.write(response.content)
    print("CSV exported successfully")
```

---

### 4. Health Check

**Endpoint:** `GET /api/v1/transactions/health`

Check transaction service health.

**Response:** `Transaction Service is running`

---

## Complete Example: React Transaction Dashboard

```jsx
import React, { useState, useEffect } from 'react';

function TransactionDashboard() {
  const userId = '123e4567-e89b-12d3-a456-426614174000';
  const [transactions, setTransactions] = useState([]);
  const [summary, setSummary] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [category, setCategory] = useState('');
  const [period, setPeriod] = useState('this_month');

  // Fetch transactions
  const fetchTransactions = async () => {
    const params = new URLSearchParams({
      page,
      size: 20,
      ...(category && { category })
    });

    const response = await fetch(
      `http://localhost:8080/api/v1/transactions/${userId}?${params}`
    );
    const data = await response.json();

    setTransactions(data.transactions);
    setTotalPages(data.totalPages);
  };

  // Fetch summary
  const fetchSummary = async () => {
    const response = await fetch(
      `http://localhost:8080/api/v1/transactions/${userId}/summary?period=${period}`
    );
    const data = await response.json();
    setSummary(data);
  };

  // Export PDF
  const exportPDF = async () => {
    const response = await fetch(
      `http://localhost:8080/api/v1/transactions/${userId}/export?format=pdf`
    );
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'statement.pdf';
    link.click();
    window.URL.revokeObjectURL(url);
  };

  useEffect(() => {
    fetchTransactions();
  }, [page, category]);

  useEffect(() => {
    fetchSummary();
  }, [period]);

  return (
    <div className="dashboard">
      <h1>Transaction Dashboard</h1>

      {/* Summary Section */}
      {summary && (
        <div className="summary">
          <h2>Summary - {summary.period}</h2>
          <div className="summary-cards">
            <div className="card income">
              <h3>Total Income</h3>
              <p>₦{summary.totalIncome.toLocaleString()}</p>
            </div>
            <div className="card expenses">
              <h3>Total Expenses</h3>
              <p>₦{summary.totalExpenses.toLocaleString()}</p>
            </div>
            <div className="card net">
              <h3>Net Amount</h3>
              <p>₦{summary.netAmount.toLocaleString()}</p>
            </div>
          </div>

          <div className="category-breakdown">
            <h3>Category Breakdown</h3>
            {summary.categoryBreakdown.map(cat => (
              <div key={cat.category} className="category-item">
                <span>{cat.category}</span>
                <span>₦{cat.amount.toLocaleString()}</span>
                <span>{cat.percentage.toFixed(1)}%</span>
              </div>
            ))}
          </div>

          <select value={period} onChange={(e) => setPeriod(e.target.value)}>
            <option value="this_month">This Month</option>
            <option value="last_month">Last Month</option>
            <option value="last_3_months">Last 3 Months</option>
            <option value="this_year">This Year</option>
          </select>
        </div>
      )}

      {/* Transactions Section */}
      <div className="transactions">
        <h2>Transaction History</h2>

        <div className="filters">
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All Categories</option>
            <option value="FOOD">Food</option>
            <option value="TRANSPORT">Transport</option>
            <option value="BILLS">Bills</option>
            <option value="SHOPPING">Shopping</option>
          </select>

          <button onClick={exportPDF}>Export PDF</button>
        </div>

        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Category</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Balance</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map(txn => (
              <tr key={txn.id} className={txn.type.toLowerCase()}>
                <td>{new Date(txn.createdAt).toLocaleString()}</td>
                <td>{txn.description}</td>
                <td>{txn.category}</td>
                <td>{txn.type}</td>
                <td className={txn.type === 'CREDIT' ? 'credit' : 'debit'}>
                  ₦{txn.amount.toLocaleString()}
                </td>
                <td>₦{txn.balanceAfter.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="pagination">
          <button
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
          >
            Previous
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}

export default TransactionDashboard;
```

## Complete Example: Python Transaction Analytics

```python
import requests
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime

class TransactionAnalytics:
    def __init__(self, base_url, user_id):
        self.base_url = base_url
        self.user_id = user_id

    def get_transactions(self, page=0, size=100, category=None, from_date=None, to_date=None):
        """Fetch paginated transactions"""
        params = {'page': page, 'size': size}
        if category:
            params['category'] = category
        if from_date:
            params['fromDate'] = from_date
        if to_date:
            params['toDate'] = to_date

        response = requests.get(
            f"{self.base_url}/api/v1/transactions/{self.user_id}",
            params=params
        )
        return response.json()

    def get_all_transactions(self, **kwargs):
        """Fetch all transactions (handles pagination)"""
        all_transactions = []
        page = 0

        while True:
            data = self.get_transactions(page=page, **kwargs)
            all_transactions.extend(data['transactions'])

            if not data['hasNext']:
                break
            page += 1

        return all_transactions

    def get_summary(self, period='this_month', from_date=None, to_date=None):
        """Get transaction summary"""
        params = {'period': period}
        if period == 'custom':
            params['fromDate'] = from_date
            params['toDate'] = to_date

        response = requests.get(
            f"{self.base_url}/api/v1/transactions/{self.user_id}/summary",
            params=params
        )
        return response.json()

    def export_pdf(self, filename='statement.pdf', from_date=None, to_date=None):
        """Export transactions as PDF"""
        params = {'format': 'pdf'}
        if from_date:
            params['fromDate'] = from_date
        if to_date:
            params['toDate'] = to_date

        response = requests.get(
            f"{self.base_url}/api/v1/transactions/{self.user_id}/export",
            params=params
        )

        with open(filename, 'wb') as f:
            f.write(response.content)
        print(f"PDF exported to {filename}")

    def export_csv(self, filename='statement.csv', from_date=None, to_date=None):
        """Export transactions as CSV"""
        params = {'format': 'csv'}
        if from_date:
            params['fromDate'] = from_date
        if to_date:
            params['toDate'] = to_date

        response = requests.get(
            f"{self.base_url}/api/v1/transactions/{self.user_id}/export",
            params=params
        )

        with open(filename, 'wb') as f:
            f.write(response.content)
        print(f"CSV exported to {filename}")

    def analyze_spending(self, period='last_3_months'):
        """Analyze spending patterns"""
        summary = self.get_summary(period=period)

        print(f"\n=== Spending Analysis: {summary['period']} ===")
        print(f"Total Income: ₦{summary['totalIncome']:,.2f}")
        print(f"Total Expenses: ₦{summary['totalExpenses']:,.2f}")
        print(f"Net Amount: ₦{summary['netAmount']:,.2f}")
        print(f"Total Transactions: {summary['totalTransactions']}")

        print("\nCategory Breakdown:")
        for cat in summary['categoryBreakdown']:
            print(f"  {cat['category']}: ₦{cat['amount']:,.2f} ({cat['percentage']:.1f}%) - {cat['transactionCount']} transactions")

        if summary['topCategory']:
            print(f"\nTop Spending Category: {summary['topCategory']['category']}")

        return summary

    def plot_category_chart(self, period='this_month'):
        """Plot category spending chart"""
        summary = self.get_summary(period=period)

        categories = [cat['category'] for cat in summary['categoryBreakdown']]
        amounts = [cat['amount'] for cat in summary['categoryBreakdown']]

        plt.figure(figsize=(10, 6))
        plt.bar(categories, amounts, color='steelblue')
        plt.xlabel('Category')
        plt.ylabel('Amount (₦)')
        plt.title(f'Spending by Category - {summary["period"]}')
        plt.xticks(rotation=45, ha='right')
        plt.tight_layout()
        plt.show()

    def to_dataframe(self, **kwargs):
        """Convert transactions to pandas DataFrame"""
        transactions = self.get_all_transactions(**kwargs)
        df = pd.DataFrame(transactions)
        df['createdAt'] = pd.to_datetime(df['createdAt'])
        return df

# Usage example
if __name__ == '__main__':
    analytics = TransactionAnalytics(
        base_url='http://localhost:8080',
        user_id='123e4567-e89b-12d3-a456-426614174000'
    )

    # Analyze spending
    analytics.analyze_spending(period='last_3_months')

    # Export reports
    analytics.export_pdf('november_statement.pdf',
                        from_date='2024-11-01T00:00:00',
                        to_date='2024-11-30T23:59:59')

    # Plot chart
    analytics.plot_category_chart(period='this_month')

    # Get DataFrame for custom analysis
    df = analytics.to_dataframe(from_date='2024-01-01T00:00:00')
    print(f"\nTotal transactions in DataFrame: {len(df)}")
    print(df.head())
```

## Error Responses

### 400 Bad Request
```json
{
  "error": "Custom period requires both fromDate and toDate parameters"
}
```

### 404 Not Found
```json
{
  "error": "User not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Failed to generate transaction summary"
}
```

## Performance Considerations

### Pagination
- Default page size: 20 transactions
- Maximum page size: 100 transactions
- Use pagination for large datasets to improve performance

### Filtering
- All queries use indexed columns for optimal performance
- Date range filtering is highly optimized with composite indexes
- Category filtering uses ENUM type for efficiency

### Export
- CSV export is memory-efficient for large datasets
- PDF export includes transaction limits for reasonable file sizes
- Consider date range filtering for large exports

## Best Practices

1. **Pagination:**
   - Always use pagination for transaction lists
   - Start with small page sizes (20-50)
   - Increase size only when needed

2. **Filtering:**
   - Use category filter to narrow results
   - Apply date ranges for specific analysis
   - Combine filters for precise queries

3. **Export:**
   - Use CSV for data analysis and imports
   - Use PDF for formal statements and records
   - Apply date ranges to limit export size

4. **Caching:**
   - Summary data can be cached on client side
   - Refresh summary when new transactions occur
   - Cache export files for repeated downloads

## Integration with AI Chat

Transaction data is accessible to the AI assistant through function calling:
- AI can query transactions using natural language
- Spending summaries are generated on demand
- Export recommendations based on user requests

Example AI interactions:
```
User: "Show me my food expenses last month"
AI: [Uses queryTransactions function] → Returns filtered results

User: "Generate my statement for November"
AI: [Recommends export endpoint] → Provides download link
```

## Related Documentation

- [AI Chat API](./AI_CHAT_API.md) - Text-based chat API
- [Voice API](./VOICE_API.md) - Voice interaction API
- [Spring AI Functions](./SPRING_AI_FUNCTIONS.md) - Available AI function tools
- [README](./README.md) - Main documentation

---

**Generated for Fulus Pay AI Assistant - Transaction API v1.0**
