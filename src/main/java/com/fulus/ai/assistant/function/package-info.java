/**
 * Spring AI Function Tools
 *
 * This package contains Spring AI function tools that enable the AI model to automatically
 * call backend functions to fetch real-time data and perform operations.
 *
 * <h2>How Function Calling Works</h2>
 * <ol>
 *   <li>User asks a natural language question (e.g., "How much did I spend last month?")</li>
 *   <li>GPT-4 analyzes the query and detects it needs transaction data</li>
 *   <li>AI automatically calls the appropriate function with inferred parameters</li>
 *   <li>Function executes, queries database, and returns formatted result</li>
 *   <li>AI incorporates the result into a natural language response</li>
 * </ol>
 *
 * <h2>Available Functions</h2>
 * <ul>
 *   <li><b>TransactionQueryFunction</b> - Query and analyze user transactions
 *     <ul>
 *       <li>Parameters: userId, category (optional), timePeriod (optional)</li>
 *       <li>Time periods: "this month", "last week", "last 30 days", etc.</li>
 *       <li>Returns: Formatted summary with totals and category breakdown</li>
 *     </ul>
 *   </li>
 *   <li><b>StatementGeneratorFunction</b> - Generate detailed account statements
 *     <ul>
 *       <li>Parameters: userId, transactionType (credit/debit/all), period (optional)</li>
 *       <li>Returns: Formatted table with Date | Description | Amount | Balance</li>
 *       <li>Includes: Opening balance, total credits/debits, closing balance</li>
 *       <li>Suitable for display or Text-to-Speech (TTS)</li>
 *     </ul>
 *   </li>
 *   <li><b>SavingsCalculatorFunction</b> - Calculate savings projections with compound interest
 *     <ul>
 *       <li>Parameters: monthlyAmount, months, interestRate (optional, defaults to 5%)</li>
 *       <li>Returns: Conversational explanation with totals and month-by-month growth</li>
 *       <li>Includes: Total contributions, interest earned, final amount, effective yield</li>
 *       <li>Perfect for financial planning and goal setting</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // User query
 * String userMessage = "How much did I spend on food last month?";
 *
 * // AI automatically calls queryTransactions with:
 * // {
 * //   "userId": "user-uuid",
 * //   "category": "FOOD",
 * //   "timePeriod": "last month"
 * // }
 *
 * // Function returns formatted summary
 * // AI responds: "You spent ₦30,000 on food last month across 12 transactions."
 * </pre>
 *
 * <h2>Creating New Functions</h2>
 * <pre>
 * &#64;Component("myFunction")
 * &#64;Description("What this function does")
 * public class MyFunction implements Function&lt;MyRequest, String&gt; {
 *     &#64;Override
 *     public String apply(MyRequest request) {
 *         // Implementation
 *         return "Result";
 *     }
 * }
 * </pre>
 *
 * <p>Then register in OpenAIConfig:</p>
 * <pre>
 * FunctionCallback callback = FunctionCallbackWrapper.builder(myFunction)
 *     .withName("myFunction")
 *     .withDescription("Function description")
 *     .withInputType(MyRequest.class)
 *     .build();
 * </pre>
 *
 * @see com.fulus.ai.assistant.function.TransactionQueryFunction
 * @see org.springframework.ai.model.function.FunctionCallback
 * @see org.springframework.ai.model.function.FunctionCallbackWrapper
 */
package com.fulus.ai.assistant.function;
