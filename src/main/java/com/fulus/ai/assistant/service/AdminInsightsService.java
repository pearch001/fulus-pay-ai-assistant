package com.fulus.ai.assistant.service;

import com.fulus.ai.assistant.dto.AdminChatRequest;
import com.fulus.ai.assistant.dto.AdminChatResponse;
import com.fulus.ai.assistant.dto.ChartData;
import com.fulus.ai.assistant.entity.AdminChatMessage;
import com.fulus.ai.assistant.entity.AdminConversation;
import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.InsightCategory;
import com.fulus.ai.assistant.enums.MessageType;
import com.fulus.ai.assistant.exception.UnauthorizedException;
import com.fulus.ai.assistant.repository.AdminChatMessageRepository;
import com.fulus.ai.assistant.repository.AdminConversationRepository;
import com.fulus.ai.assistant.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminInsightsService {

    private final AdminConversationRepository conversationRepository;
    private final AdminChatMessageRepository chatMessageRepository;
    private final AdminInsightsCacheService cacheService;
    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final ChartGenerationService chartGenerationService;

    private static final String SYSTEM_PROMPT_FILE = "admin-insights-system-prompt.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final long STATS_CACHE_DURATION_MINUTES = 5;
    private static final int ESTIMATED_CHARS_PER_TOKEN = 4;
    private static final int MAX_CONTEXT_TOKENS = 3000;

    private String systemPromptTemplate;
    private Map<String, Object> cachedPlatformStats;
    private LocalDateTime statsCacheTimestamp;

    public AdminInsightsService(
            AdminConversationRepository conversationRepository,
            AdminChatMessageRepository chatMessageRepository,
            @Qualifier("adminChatClient") ChatClient chatClient,
            UserRepository userRepository,
            AdminInsightsCacheService cacheService,
            ChartGenerationService chartGenerationService) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatClient = chatClient;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
        this.chartGenerationService = chartGenerationService;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_FILE);
            systemPromptTemplate = StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8);
            log.info("Admin insights system prompt loaded successfully. Length: {} characters",
                    systemPromptTemplate.length());
        } catch (IOException e) {
            log.error("Failed to load system prompt from {}", SYSTEM_PROMPT_FILE, e);
            systemPromptTemplate = "You are the Business Insights AI for SyncPay. " +
                    "Provide actionable business intelligence to executives and investors.";
        }
    }

    @Transactional
    public AdminChatResponse processMessage(AdminChatRequest request, UUID adminId) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing admin chat message for admin: {}", adminId);

            // 1. Categorize the query
            InsightCategory category = categorizeQuery(request.getMessage());
            log.info("Query categorized as: {} - {}", category.name(), category.getDisplayName());

            // 2. Get or create conversation
            AdminConversation conversation = createOrGetConversation(adminId, request.getConversationId());

            // 3. Save user message
            AdminChatMessage userMessage = saveMessage(
                    conversation.getConversationId(),
                    "USER",
                    request.getMessage(),
                    calculateTokens(request.getMessage())
            );

            if (userMessage.getMetadata() == null) {
                userMessage.setMetadata(new HashMap<>());
            }
            userMessage.getMetadata().put("category", category.name());
            userMessage.getMetadata().put("categoryDisplay", category.getDisplayName());
            chatMessageRepository.save(userMessage);

            log.debug("Saved user message with {} tokens (sequence: {}, category: {})",
                    userMessage.getTokenCount(), userMessage.getSequenceNumber(), category.getDisplayName());

            // 4. Check cache first
            String cacheKey = null;
            String aiResponse = null;

            if (cacheService.shouldCache(request.getMessage())) {
                // Ensure stats are loaded and timestamp is set
                getOrRefreshStats();

                if (statsCacheTimestamp != null) {
                    cacheKey = cacheService.generateCacheKey(request.getMessage(), statsCacheTimestamp);
                    aiResponse = cacheService.getCachedResponse(cacheKey);

                    if (aiResponse != null) {
                        cacheService.recordCacheHit();
                        log.info("Cache HIT for query: '{}'",
                                request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
                    } else {
                        cacheService.recordCacheMiss();
                        log.info("Cache MISS for query: '{}'",
                                request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
                    }
                }
            }

            // 5. Generate response if not cached
            if (aiResponse == null) {
                List<Message> contextualMessages = buildContextualMessages(
                        conversation.getConversationId(),
                        request.getMessage(),
                        adminId,
                        category
                );
                log.debug("Built {} contextual messages for AI", contextualMessages.size());

                cacheService.recordApiCall();
                aiResponse = callOpenAIWithContext(contextualMessages);
                log.info("Received AI response from OpenAI (length: {} chars)", aiResponse.length());

                if (cacheKey != null) {
                    cacheService.cacheResponse(cacheKey, aiResponse);
                    log.debug("Cached response for future requests");
                }
            }

            // 6. Generate charts if requested
            List<ChartData> charts = new ArrayList<>();
            String userMsgLower = request.getMessage().toLowerCase();

            // Check if user wants visualization
            boolean needsChart = Boolean.TRUE.equals(request.getIncludeCharts()) ||
                                userMsgLower.contains("show") ||
                                userMsgLower.contains("chart") ||
                                userMsgLower.contains("graph") ||
                                userMsgLower.contains("visualize") ||
                                userMsgLower.contains("trend") ||
                                userMsgLower.contains("plot") ||
                                userMsgLower.contains("display");

            if (needsChart) {
                log.info("Chart generation triggered for query: {}", request.getMessage());
                charts = chartGenerationService.generateCharts(request.getMessage());
                log.info("Generated {} chart(s)", charts.size());
            }

            // 7. Save assistant message with chart metadata
            int aiTokens = calculateTokens(aiResponse);
            Map<String, Object> metadata = new HashMap<>();
            if (!charts.isEmpty()) {
                metadata.put("charts", charts);
            }

            AdminChatMessage assistantMessage = saveMessage(
                    conversation.getConversationId(),
                    "ASSISTANT",
                    aiResponse,
                    aiTokens,
                    metadata
            );

            // 8. Auto-summarize conversation if needed
            if (conversation.getMessageCount() % 10 == 0 && conversation.getMessageCount() >= 10) {
                log.info("Triggering auto-summarization for conversation {} (message count: {})",
                        conversation.getConversationId(), conversation.getMessageCount());
                try {
                    String summary = generateConversationSummary(conversation.getConversationId());
                    if (summary != null) {
                        conversation.setConversationSummary(summary);
                        conversationRepository.save(conversation);
                        log.info("Conversation summary generated and saved");
                    }
                } catch (Exception e) {
                    log.error("Failed to generate conversation summary", e);
                }
            }

            // 9. Build response with charts
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Successfully processed message for admin: {} (processing time: {}ms)", adminId, processingTime);

            return AdminChatResponse.builder()
                    .message(aiResponse)
                    .conversationId(conversation.getConversationId())
                    .timestamp(LocalDateTime.now())
                    .processingTimeMs(processingTime)
                    .charts(charts)
                    .sequenceNumber(assistantMessage.getSequenceNumber())
                    .tokenCount(aiTokens)
                    .build();

        } catch (UnauthorizedException e) {
            log.error("Unauthorized access attempt by admin {}: {}", adminId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing admin chat", e);
            return buildErrorResponse(
                    "We encountered an error processing your request. Please try again.",
                    System.currentTimeMillis() - startTime
            );
        }
    }

    @Transactional
    public AdminConversation createOrGetConversation(UUID adminId, UUID conversationId) {
        // 1. If conversationId provided, try to find and validate
        if (conversationId != null) {
            Optional<AdminConversation> existing = conversationRepository.findByConversationId(conversationId);
            if (existing.isPresent()) {
                AdminConversation conv = existing.get();
                // Validate ownership
                if (!conv.getAdminId().equals(adminId)) {
                    throw new UnauthorizedException("Conversation does not belong to admin");
                }
                // Update timestamp
                conv.setUpdatedAt(LocalDateTime.now());
                return conversationRepository.save(conv);
            }
        }

        // 2. Create new conversation
        AdminConversation conversation = new AdminConversation();
        conversation.setAdminId(adminId);
        conversation.setConversationId(UUID.randomUUID());
        conversation.setSubject("Business Insights Chat");
        conversation.setActive(true);
        conversation.setMessageCount(0);
        conversation.setTotalTokens(0L);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        AdminConversation saved = conversationRepository.save(conversation);
        log.info("Created new admin conversation: {} for admin: {}", saved.getConversationId(), adminId);
        return saved;
    }

    public List<AdminChatMessage> getConversationHistory(UUID conversationId, int limit) {
        List<AdminChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderBySequenceNumberAsc(conversationId);

        if (messages.size() > limit) {
            int startIndex = messages.size() - limit;
            messages = messages.subList(startIndex, messages.size());
        }

        return messages;
    }

    public String buildSystemPrompt(UUID adminId) {
        try {
            String adminName = userRepository.findById(adminId)
                    .map(User::getName)
                    .orElse("Admin");

            Map<String, Object> platformStats = getOrRefreshStats();
            String formattedStats = formatPlatformStatsAsBulletPoints(platformStats);

            return systemPromptTemplate
                    .replace("{{CURRENT_DATE}}", LocalDateTime.now().format(DATE_FORMATTER))
                    .replace("{{ADMIN_NAME}}", adminName)
                    .replace("{{PLATFORM_STATS}}", formattedStats);
        } catch (Exception e) {
            log.error("Error building system prompt", e);
            return "You are the Business Insights AI for SyncPay. Provide actionable business intelligence.";
        }
    }

    private Map<String, Object> getOrRefreshStats() {
        if (cachedPlatformStats == null ||
                statsCacheTimestamp == null ||
                LocalDateTime.now().minusMinutes(STATS_CACHE_DURATION_MINUTES).isAfter(statsCacheTimestamp)) {
            cachedPlatformStats = generatePlatformStats();
            statsCacheTimestamp = LocalDateTime.now();
            log.debug("Refreshed platform stats cache");
        }
        return cachedPlatformStats;
    }

    public Map<String, Object> generatePlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // User metrics
        int totalUsers = random.nextInt(15_000, 25_001);
        int activeUsers = (int) (totalUsers * random.nextDouble(0.65, 0.76));
        double monthlyGrowthRate = random.nextDouble(8.0, 15.1);

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("newUsersToday", random.nextInt(50, 201));
        stats.put("monthlyGrowthRate", Math.round(monthlyGrowthRate * 10.0) / 10.0);

        // Transaction metrics
        int totalTransactions = random.nextInt(100_000, 500_001);
        double successRate = random.nextDouble(92.0, 97.1);
        long avgTxValue = random.nextLong(2_500, 8_001);
        long txVolume = totalTransactions * avgTxValue;

        stats.put("totalTransactions", totalTransactions);
        stats.put("transactionsToday", random.nextInt(3_000, 15_001));
        stats.put("transactionSuccessRate", Math.round(successRate * 10.0) / 10.0);
        stats.put("averageTransactionValue", avgTxValue);
        stats.put("transactionVolume", String.format("₦%,d", txVolume));

        // Revenue metrics
        long mrr = random.nextLong(3_000_000, 8_000_001);
        long lastMonthRevenue = random.nextLong(2_500_000, 7_500_001);
        long cardFees = random.nextLong(500_000, 2_000_001);

        stats.put("monthlyRecurringRevenue", String.format("₦%,d", mrr));
        stats.put("revenueLastMonth", String.format("₦%,d", lastMonthRevenue));
        stats.put("cardTransactionFeeRevenue", String.format("₦%,d", cardFees));

        // Customer economics
        long cac = random.nextLong(1_200, 2_501);
        long ltv = random.nextLong(15_000, 45_001);
        double ltvCacRatio = (double) ltv / cac;

        stats.put("customerAcquisitionCost", String.format("₦%,d", cac));
        stats.put("lifetimeValue", String.format("₦%,d", ltv));
        stats.put("ltvToCacRatio", Math.round(ltvCacRatio * 10.0) / 10.0);

        // Transaction breakdown
        stats.put("offlineTransactionPercentage", Math.round(random.nextDouble(15.0, 25.1) * 10.0) / 10.0);
        stats.put("cardTransactionPercentage", Math.round(random.nextDouble(30.0, 40.1) * 10.0) / 10.0);
        stats.put("billPaymentPercentage", Math.round(random.nextDouble(20.0, 30.1) * 10.0) / 10.0);
        stats.put("p2pTransferPercentage", Math.round(random.nextDouble(15.0, 25.1) * 10.0) / 10.0);

        // Operations
        stats.put("peakHours", "2PM - 6PM WAT");
        stats.put("averageResponseTime", random.nextInt(100, 301) + "ms");
        stats.put("uptime", "99." + random.nextInt(95, 100) + "%");

        // Geographic data
        Map<String, Integer> topStates = new HashMap<>();
        topStates.put("Lagos", random.nextInt(5_000, 10_001));
        topStates.put("Abuja", random.nextInt(2_000, 5_001));
        topStates.put("Kano", random.nextInt(1_500, 3_001));
        topStates.put("Oyo", random.nextInt(1_000, 2_501));
        stats.put("topStatesByUsers", topStates);

        return stats;
    }

    @Transactional
    public AdminChatMessage saveMessage(UUID conversationId, String role, String content, int tokenCount) {
        return saveMessage(conversationId, role, content, tokenCount, new HashMap<>());
    }

    @Transactional
    public AdminChatMessage saveMessage(UUID conversationId, String role, String content, int tokenCount, Map<String, Object> metadata) {
        AdminConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        Integer maxSequence = chatMessageRepository.findMaxSequenceNumber(conversationId);
        int nextSequence = (maxSequence != null ? maxSequence : 0) + 1;

        int finalTokenCount = tokenCount > 0 ? tokenCount : calculateTokens(content);

        AdminChatMessage message = new AdminChatMessage();
        message.setConversationId(conversationId); // Use conversationId UUID, not id UUID
        message.setRole(MessageType.valueOf(role));
        message.setContent(content);
        message.setTokenCount(finalTokenCount);
        message.setSequenceNumber(nextSequence);
        message.setCreatedAt(LocalDateTime.now());
        message.setMetadata(metadata != null ? metadata : new HashMap<>());

        AdminChatMessage saved = chatMessageRepository.save(message);

        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversation.setTotalTokens(conversation.getTotalTokens() + finalTokenCount);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.debug("Saved {} message (sequence: {}, tokens: {}) for conversation: {}",
                role, nextSequence, finalTokenCount, conversationId);

        return saved;
    }

    public int calculateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        return Math.max(1, content.length() / ESTIMATED_CHARS_PER_TOKEN);
    }

    private InsightCategory categorizeQuery(String message) {
        if (message == null) return InsightCategory.GENERAL_QUERY;
        String lower = message.toLowerCase();

        if (lower.contains("revenue") || lower.contains("income") || lower.contains("₦"))
            return InsightCategory.REVENUE_ANALYSIS;
        if (lower.contains("user") || lower.contains("customer") || lower.contains("growth"))
            return InsightCategory.USER_GROWTH;
        if (lower.contains("transaction") || lower.contains("payment"))
            return InsightCategory.TRANSACTION_PATTERNS;
        if (lower.contains("fee") || lower.contains("pricing"))
            return InsightCategory.FEE_OPTIMIZATION;
        if (lower.contains("risk") || lower.contains("fraud"))
            return InsightCategory.RISK_ASSESSMENT;
        if (lower.contains("market") || lower.contains("competitor"))
            return InsightCategory.MARKET_INTELLIGENCE;

        return InsightCategory.GENERAL_QUERY;
    }

    private String buildCategoryContext(InsightCategory category) {
        StringBuilder context = new StringBuilder();
        context.append("**Query Category: ").append(category.getDisplayName()).append("**\n");
        context.append(category.getDescription()).append("\n\n");

        switch (category) {
            case REVENUE_ANALYSIS:
                context.append("Focus Areas:\n");
                context.append("- Revenue growth rate and projections\n");
                context.append("- Revenue breakdown by source (fees, subscriptions, etc.)\n");
                context.append("- Monthly Recurring Revenue (MRR) trends\n");
                context.append("- Revenue optimization opportunities\n\n");
                context.append("Suggested Insights: Compare current vs. previous periods, identify top revenue sources, forecast future revenue.");
                break;
            case USER_GROWTH:
                context.append("Focus Areas:\n");
                context.append("- Active user metrics (DAU, MAU)\n");
                context.append("- User retention and churn rates\n");
                context.append("- Customer lifetime value (LTV)\n");
                context.append("- Growth drivers and obstacles\n\n");
                context.append("Suggested Insights: Analyze growth trends, identify retention issues, recommend acquisition strategies.");
                break;
            case TRANSACTION_PATTERNS:
                context.append("Focus Areas:\n");
                context.append("- Transaction volume trends\n");
                context.append("- Success vs. failure rates\n");
                context.append("- Average transaction value\n");
                context.append("- Peak usage times\n\n");
                context.append("Suggested Insights: Identify patterns, optimize success rates, recommend operational improvements.");
                break;
            default:
                context.append("Provide comprehensive business insights and actionable recommendations.");
        }

        return context.toString();
    }

    public List<Message> buildContextualMessages(
            UUID conversationId, String currentMessage, UUID adminId, InsightCategory category) {

        List<Message> messages = new ArrayList<>();

        // 1. System prompt with category context
        String systemPrompt = buildSystemPrompt(adminId);
        String categoryContext = buildCategoryContext(category);
        systemPrompt = systemPrompt + "\n\n" + categoryContext;

        messages.add(new SystemMessage(systemPrompt));

        // 2. Calculate available token budget
        int systemTokens = calculateTokens(systemPrompt);
        int currentMessageTokens = calculateTokens(currentMessage);
        int availableTokens = MAX_CONTEXT_TOKENS - systemTokens - currentMessageTokens;

        // 3. Add conversation summary if exists
        AdminConversation conversation = conversationRepository.findByConversationId(conversationId).orElse(null);
        if (conversation != null && conversation.getConversationSummary() != null &&
                !conversation.getConversationSummary().isEmpty() && availableTokens > 200) {

            String savedSummary = conversation.getConversationSummary();
            int summaryTokens = calculateTokens(savedSummary);

            if (summaryTokens < availableTokens) {
                messages.add(new SystemMessage("Previous conversation summary: " + savedSummary));
                availableTokens -= summaryTokens;
                log.debug("Added inline conversation summary ({} tokens)", summaryTokens);
            }
        }

        // 4. Add recent message history
        List<AdminChatMessage> recentHistory = getConversationHistory(conversationId, MAX_HISTORY_MESSAGES);

        int recentCount = Math.min(5, recentHistory.size());
        List<AdminChatMessage> recentMessages = recentHistory.subList(
                Math.max(0, recentHistory.size() - recentCount), recentHistory.size());

        for (AdminChatMessage msg : recentMessages) {
            int msgTokens = msg.getTokenCount() != null ? msg.getTokenCount() : calculateTokens(msg.getContent());

            if (msgTokens < availableTokens) {
                if (msg.getRole() == MessageType.USER) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if (msg.getRole() == MessageType.ASSISTANT) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
                availableTokens -= msgTokens;
            }
        }

        // 5. Add current user message
        messages.add(new UserMessage(currentMessage));

        return messages;
    }

    public String callOpenAIWithContext(List<Message> contextualMessages) {
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel("gpt-4-turbo")
                    .withTemperature(0.7)
                    .withMaxTokens(1500)
                    .build();

            Prompt prompt = new Prompt(contextualMessages, options);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("OpenAI returned null result");
                return "I apologize, but I couldn't generate a response at this time. Please try rephrasing your question.";
            }

            String aiMessage = response.getResult().getOutput().getContent();
            return aiMessage != null ? aiMessage.trim() : "I apologize, but I couldn't generate a response.";

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to generate AI response: " + e.getMessage(), e);
        }
    }

    @Transactional
    public String generateConversationSummary(UUID conversationId) {
        try {
            List<AdminChatMessage> allMessages = chatMessageRepository
                    .findByConversationIdOrderBySequenceNumberAsc(conversationId);

            if (allMessages == null || allMessages.size() < 10) {
                return null;
            }

            // Extract topics, data points, and recommendations
            List<String> topics = new ArrayList<>();
            List<String> dataPoints = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();

            for (AdminChatMessage msg : allMessages) {
                String content = msg.getContent().toLowerCase();
                if (content.contains("revenue") && !topics.contains("Revenue")) topics.add("Revenue");
                if (content.contains("user") && !topics.contains("Users")) topics.add("Users");
                if (content.contains("transaction") && !topics.contains("Transactions")) topics.add("Transactions");

                if (content.contains("₦") || content.matches(".*\\d+.*")) {
                    String preview = msg.getContent().substring(0, Math.min(100, msg.getContent().length()));
                    if (!dataPoints.contains(preview)) dataPoints.add(preview);
                }

                if (content.contains("recommend") || content.contains("suggest")) {
                    recommendations.add(msg.getContent());
                }
            }

            StringBuilder summaryPrompt = new StringBuilder();
            summaryPrompt.append("Summarize this SyncPay business insights conversation in 3-4 concise bullet points.\n\n");
            summaryPrompt.append("Focus on:\n");
            summaryPrompt.append("- Main topics discussed\n");
            summaryPrompt.append("- Key insights provided\n");
            summaryPrompt.append("- Important metrics mentioned\n");
            summaryPrompt.append("- Action items or recommendations\n\n");

            summaryPrompt.append("Conversation Context:\n");
            summaryPrompt.append("Total Messages: ").append(allMessages.size()).append("\n");

            if (!topics.isEmpty()) {
                summaryPrompt.append("Topics: ").append(String.join(", ", topics)).append("\n");
            }

            if (!dataPoints.isEmpty()) {
                summaryPrompt.append("Key Metrics: ").append(String.join(", ", dataPoints.stream().limit(3).collect(Collectors.toList()))).append("\n");
            }

            if (!recommendations.isEmpty()) {
                summaryPrompt.append("Recommendations Made: ").append(recommendations.size()).append("\n");
            }

            summaryPrompt.append("\nRecent conversation excerpt:\n");
            int startIndex = Math.max(0, allMessages.size() - 6);
            for (int i = startIndex; i < allMessages.size(); i++) {
                AdminChatMessage msg = allMessages.get(i);
                String role = msg.getRole() == MessageType.USER ? "Admin" : "AI";
                String preview = msg.getContent().length() > 150
                        ? msg.getContent().substring(0, 150) + "..."
                        : msg.getContent();
                summaryPrompt.append(role).append(": ").append(preview).append("\n");
            }

            log.debug("Calling AI to generate summary");
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(
                    "You are a conversation summarizer. Create concise, actionable summaries " +
                            "of business intelligence conversations. Use bullet points. Be specific about metrics and recommendations."
            ));
            messages.add(new UserMessage(summaryPrompt.toString()));

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel("gpt-4-turbo")
                    .withTemperature(0.5)
                    .withMaxTokens(300)
                    .build();

            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            if (response == null || response.getResult() == null) {
                log.warn("Failed to generate summary - null response from AI");
                return null;
            }

            String summary = response.getResult().getOutput().getContent();

            if (summary == null || summary.trim().isEmpty()) {
                log.warn("Failed to generate summary - empty response from AI");
                return null;
            }

            summary = summary.trim();
            log.info("Generated conversation summary ({} chars)", summary.length());

            AdminConversation conversation = conversationRepository
                    .findByConversationId(conversationId)
                    .orElse(null);

            if (conversation != null) {
                conversation.setConversationSummary(summary);

                if (conversation.getSubject() == null ||
                        conversation.getSubject().equals("Business Insights Chat")) {
                    String subject = topics.isEmpty() ? "Business Insights Discussion" : topics.get(0);
                    conversation.setSubject(subject);
                }

                conversationRepository.save(conversation);
                log.info("Saved conversation summary to database");
            }

            return summary;

        } catch (Exception e) {
            log.error("Error generating conversation summary for {}", conversationId, e);
            return null;
        }
    }

    private String formatPlatformStatsAsBulletPoints(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder("\nCurrent Platform Metrics:\n");

        sb.append(String.format("• Total Users: %,d\n", stats.get("totalUsers")));
        sb.append(String.format("• Active Users: %,d (%.1f%%)\n",
                stats.get("activeUsers"),
                ((int) stats.get("activeUsers") * 100.0 / (int) stats.get("totalUsers"))));
        sb.append(String.format("• Growth Rate: %.1f%%\n", stats.get("monthlyGrowthRate")));
        sb.append(String.format("• Transactions: %,d\n", stats.get("totalTransactions")));
        sb.append(String.format("• Success Rate: %.1f%%\n", stats.get("transactionSuccessRate")));
        sb.append(String.format("• Transaction Volume: %s\n", stats.get("transactionVolume")));
        sb.append(String.format("• MRR: %s\n", stats.get("monthlyRecurringRevenue")));
        sb.append(String.format("• Revenue Last Month: %s\n", stats.get("revenueLastMonth")));
        sb.append(String.format("• LTV:CAC Ratio: %.1fx\n", stats.get("ltvToCacRatio")));

        sb.append("\nTransaction Breakdown:\n");
        sb.append(String.format("• Offline: %.1f%%\n", stats.get("offlineTransactionPercentage")));
        sb.append(String.format("• Card: %.1f%%\n", stats.get("cardTransactionPercentage")));
        sb.append(String.format("• Bill Payments: %.1f%%\n", stats.get("billPaymentPercentage")));
        sb.append(String.format("• P2P: %.1f%%\n", stats.get("p2pTransferPercentage")));

        sb.append("\nOperations:\n");
        sb.append(String.format("• Peak Hours: %s\n", stats.get("peakHours")));
        sb.append(String.format("• Response Time: %s\n", stats.get("averageResponseTime")));
        sb.append(String.format("• Uptime: %s\n", stats.get("uptime")));

        return sb.toString();
    }

    private AdminChatResponse buildErrorResponse(String errorMessage, long processingTime) {
        return AdminChatResponse.builder()
                .message(errorMessage)
                .conversationId(null)
                .timestamp(LocalDateTime.now())
                .processingTimeMs(processingTime)
                .charts(new ArrayList<>())
                .build();
    }
}

