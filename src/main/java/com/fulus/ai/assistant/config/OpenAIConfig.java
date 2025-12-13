package com.fulus.ai.assistant.config;

import com.fulus.ai.assistant.dto.BudgetRequest;
import com.fulus.ai.assistant.dto.PayBillRequest;
import com.fulus.ai.assistant.dto.SavingsCalculatorRequest;
import com.fulus.ai.assistant.dto.SendMoneyRequest;
import com.fulus.ai.assistant.dto.StatementGenerateRequest;
import com.fulus.ai.assistant.dto.TransactionQueryRequest;
import com.fulus.ai.assistant.function.BudgetAssistantFunction;
import com.fulus.ai.assistant.function.OfflineTransactionQueryFunction;
import com.fulus.ai.assistant.function.PayBillFunction;
import com.fulus.ai.assistant.function.SavingsCalculatorFunction;
import com.fulus.ai.assistant.function.SendMoneyFunction;
import com.fulus.ai.assistant.function.StatementGeneratorFunction;
import com.fulus.ai.assistant.function.TransactionQueryFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableRetry
@EnableScheduling
@Slf4j
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4-turbo}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    /**
     * Configure OpenAI API with custom settings
     */
    @Bean
    public OpenAiApi openAiApi() {
        log.info("Configuring OpenAI API with model: {}", model);

        try {
            return new OpenAiApi(apiKey);
        } catch (Exception e) {
            log.error("Failed to initialize OpenAI API: {}", e.getMessage());
            throw new IllegalStateException("OpenAI API initialization failed. Check your API key.", e);
        }
    }

    /**
     * Configure OpenAI Chat Model with options and function callbacks
     */
    @Bean
    public ChatModel openAiChatModel(
            OpenAiApi openAiApi,
            TransactionQueryFunction transactionQueryFunction,
            StatementGeneratorFunction statementGeneratorFunction,
            SavingsCalculatorFunction savingsCalculatorFunction,
            BudgetAssistantFunction budgetAssistantFunction,
            SendMoneyFunction sendMoneyFunction,
            PayBillFunction payBillFunction,
            OfflineTransactionQueryFunction offlineTransactionQueryFunction) {

        log.info("Configuring OpenAI Chat Model with temperature={}, maxTokens={}", temperature, maxTokens);

        // Create function callback for transaction query
        FunctionCallback transactionQueryCallback = FunctionCallbackWrapper.builder(transactionQueryFunction)
                .withName("queryTransactions")
                .withDescription("Query and analyze user transactions. Returns detailed summary with total amount, " +
                        "transaction count, income vs expenses, and breakdown by category. " +
                        "Use when users ask about spending, transactions, or financial activity.")
                .withInputType(TransactionQueryRequest.class)
                .build();

        // Create function callback for statement generation
        FunctionCallback statementGeneratorCallback = FunctionCallbackWrapper.builder(statementGeneratorFunction)
                .withName("generateStatement")
                .withDescription("Generate a detailed account statement showing all transactions in a formatted table " +
                        "with date, description, amount, and running balance. Includes summary with opening balance, " +
                        "total credits, total debits, and closing balance. Use when users request their account statement, " +
                        "transaction history, or want to see a detailed list of all transactions.")
                .withInputType(StatementGenerateRequest.class)
                .build();

        // Create function callback for savings calculator
        FunctionCallback savingsCalculatorCallback = FunctionCallbackWrapper.builder(savingsCalculatorFunction)
                .withName("calculateSavingsProjection")
                .withDescription("Calculate savings projections with compound interest. Shows how much money will be " +
                        "accumulated after saving a fixed amount monthly for a specific period. Returns a conversational " +
                        "explanation with total contributions, interest earned, final amount, effective annual yield, and " +
                        "month-by-month growth. Use when users want to plan savings, see investment returns, estimate future " +
                        "savings, or understand how compound interest works.")
                .withInputType(SavingsCalculatorRequest.class)
                .build();

        // Create function callback for budget assistant
        FunctionCallback budgetAssistantCallback = FunctionCallbackWrapper.builder(budgetAssistantFunction)
                .withName("createBudget")
                .withDescription("Create a personalized monthly budget based on user's income and spending history. " +
                        "Analyzes the last 3 months of transactions, applies the 50/30/20 budgeting rule (50% needs, " +
                        "30% wants, 20% savings) adapted for Nigerian context, and provides category-wise allocations. " +
                        "Generates personalized recommendations based on actual spending patterns. Saves the budget to " +
                        "database for tracking. Use when users want to create a budget, plan their finances, understand " +
                        "how to allocate their income, or get spending recommendations.")
                .withInputType(BudgetRequest.class)
                .build();

        // Create function callback for sending money
        // ⚠️ WARNING: This function executes REAL financial transactions
        FunctionCallback sendMoneyCallback = FunctionCallbackWrapper.builder(sendMoneyFunction)
                .withName("sendMoney")
                .withDescription("⚠️ CRITICAL: Send money to another user. This executes REAL financial transactions. " +
                        "ONLY use when the user EXPLICITLY requests to send money or transfer funds. " +
                        "ALWAYS confirm amount and recipient before executing. Returns confirmation with transaction reference. " +
                        "Can resolve recipient by phone number or name.")
                .withInputType(SendMoneyRequest.class)
                .build();

        // Create function callback for paying bills
        // ⚠️ WARNING: This function executes REAL financial transactions
        FunctionCallback payBillCallback = FunctionCallbackWrapper.builder(payBillFunction)
                .withName("payBill")
                .withDescription("⚠️ CRITICAL: Pay bills for electricity, water, airtime, data, or cable TV. " +
                        "This executes REAL financial transactions. ONLY use when the user EXPLICITLY requests to pay a bill. " +
                        "ALWAYS confirm bill type, amount, and account number before executing. " +
                        "Returns payment confirmation with token.")
                .withInputType(PayBillRequest.class)
                .build();

        // Create function callback for offline transaction queries
        FunctionCallback offlineTransactionCallback = FunctionCallbackWrapper.builder(offlineTransactionQueryFunction)
                .withName("queryOfflineTransactions")
                .withDescription("Query and explain offline transactions and sync conflicts. Use this when users ask about: " +
                        "1) Pending offline transactions that haven't synced yet " +
                        "2) Failed offline transaction sync attempts " +
                        "3) Sync conflicts or errors with offline payments " +
                        "4) Why their offline payment to someone failed. " +
                        "Returns plain English explanations of sync status, conflicts, and resolution steps. " +
                        "Can explain technical errors (double spending, insufficient funds, signature issues, etc.) in user-friendly language.")
                .withInputType(OfflineTransactionQueryFunction.Request.class)
                .build();

        log.info("Registered function callbacks: queryTransactions, generateStatement, calculateSavingsProjection, createBudget, sendMoney, payBill, queryOfflineTransactions");

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .withFunctionCallbacks(java.util.List.of(
                        transactionQueryCallback,
                        statementGeneratorCallback,
                        savingsCalculatorCallback,
                        budgetAssistantCallback,
                        sendMoneyCallback,
                        payBillCallback,
                        offlineTransactionCallback))
                .build();

        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * Configure ChatClient for easy interaction with OpenAI with function calling
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Configuring ChatClient with OpenAI ChatModel and function calling support");

        return ChatClient.builder(chatModel)
                .defaultSystem("You are Fulus Pay AI Assistant, a helpful and friendly financial assistant. " +
                        "You help users manage their finances, make payments, track expenses, and achieve their savings goals. " +
                        "You have access to tools to query transaction data, generate account statements, calculate savings projections, " +
                        "send money, pay bills, and manage offline transactions. " +
                        "\n\n⚠️ CRITICAL SAFETY RULES:\n" +
                        "- The sendMoney and payBill functions execute REAL financial transactions.\n" +
                        "- ONLY use these when the user EXPLICITLY requests to send money or pay a bill.\n" +
                        "- ALWAYS confirm transaction details (amount, recipient, bill type) with the user before executing.\n" +
                        "- NEVER suggest or recommend using these functions unless the user explicitly asks.\n\n" +
                        "TOOL USAGE GUIDELINES:\n" +
                        "- When users ask about their spending or financial activity, use the queryTransactions tool.\n" +
                        "- When users request their account statement or transaction history, use the generateStatement tool.\n" +
                        "- When users want to plan savings or see how their money will grow, use the calculateSavingsProjection tool.\n" +
                        "- When users want to create a budget or need help managing their money, use the createBudget tool.\n" +
                        "- When users explicitly request to send money or transfer funds, use the sendMoney tool.\n" +
                        "- When users explicitly request to pay a bill, use the payBill tool.\n" +
                        "- When users ask about offline transactions, pending syncs, or sync conflicts/errors, use the queryOfflineTransactions tool.\n\n" +
                        "Always be concise, clear, and professional in your responses.")
                .build();
    }

    /**
     * Configure OpenAI Audio API for Whisper and TTS
     */
    @Bean
    public OpenAiAudioApi openAiAudioApi() {
        log.info("Configuring OpenAI Audio API for Whisper and TTS");

        try {
            return new OpenAiAudioApi(apiKey);
        } catch (Exception e) {
            log.error("Failed to initialize OpenAI Audio API: {}", e.getMessage());
            throw new IllegalStateException("OpenAI Audio API initialization failed. Check your API key.", e);
        }
    }

    /**
     * Configure OpenAI Whisper model for speech-to-text transcription
     */
    @Bean
    public OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel(OpenAiAudioApi openAiAudioApi) {
        log.info("Configuring OpenAI Audio Transcription Model (Whisper)");
        return new OpenAiAudioTranscriptionModel(openAiAudioApi);
    }

    /**
     * Configure OpenAI TTS model for text-to-speech synthesis
     */
    @Bean
    public OpenAiAudioSpeechModel openAiAudioSpeechModel(OpenAiAudioApi openAiAudioApi) {
        log.info("Configuring OpenAI Audio Speech Model (TTS)");
        return new OpenAiAudioSpeechModel(openAiAudioApi);
    }
}
