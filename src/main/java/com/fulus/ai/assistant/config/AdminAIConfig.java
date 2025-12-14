package com.fulus.ai.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Admin Business Insights AI Configuration
 *
 * Separate configuration for admin chat client with:
 * - Higher token limits for detailed business insights
 * - Balanced temperature for professional analysis
 * - GPT-4 Turbo for superior reasoning
 * - Retry logic with exponential backoff
 * - Timeout handling
 *
 * Configured independently from user chat client to allow
 * different behavior and resource allocation.
 */
@Configuration
@EnableRetry
@Slf4j
public class AdminAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${admin.ai.openai.model:gpt-4-turbo}")
    private String adminModel;

    @Value("${admin.ai.openai.temperature:0.7}")
    private Double adminTemperature;

    @Value("${admin.ai.openai.max-tokens:1500}")
    private Integer adminMaxTokens;

    @Value("${admin.ai.openai.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Value("${admin.ai.openai.max-retries:3}")
    private Integer maxRetries;

    /**
     * Dedicated ChatClient bean for admin business insights
     *
     * Configuration:
     * - Model: gpt-4-turbo (superior reasoning for business analysis)
     * - Temperature: 0.7 (balanced between creativity and consistency)
     * - Max Tokens: 1500 (detailed business insights and recommendations)
     * - Timeout: 30 seconds
     * - Retries: 3 attempts with exponential backoff
     *
     * @param adminChatModel Custom chat model configured for admin use
     * @return Configured ChatClient for admin insights
     */
    @Bean
    @Qualifier("adminChatClient")
    public ChatClient adminChatClient(@Qualifier("adminChatModel") OpenAiChatModel adminChatModel) {
        log.info("Configuring Admin ChatClient with model={}, temperature={}, maxTokens={}",
                adminModel, adminTemperature, adminMaxTokens);

        return ChatClient.builder(adminChatModel).build();
    }

    /**
     * Dedicated OpenAI Chat Model for admin insights
     * Separate from user chat model to allow different configurations
     *
     * @param openAiApi OpenAI API instance
     * @return OpenAiChatModel configured for admin use
     */
    @Bean
    @Qualifier("adminChatModel")
    public OpenAiChatModel adminChatModel(OpenAiApi openAiApi) {
        log.info("Configuring Admin OpenAI Chat Model");

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(adminModel)
                .withTemperature(adminTemperature)
                .withMaxTokens(adminMaxTokens)
                .build();

        return new OpenAiChatModel(openAiApi, options, null, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    /**
     * Retry template for admin AI calls
     * Handles transient failures and rate limits
     *
     * Configuration:
     * - Max attempts: 3 (configurable via property)
     * - Backoff: Exponential (1s, 2s, 4s)
     * - Retryable: Rate limit errors, timeout errors, API errors
     *
     * @return RetryTemplate configured for OpenAI API
     */
    @Bean
    @Qualifier("adminAiRetryTemplate")
    public RetryTemplate adminAiRetryTemplate() {
        log.info("Configuring Admin AI Retry Template with maxRetries={}", maxRetries);

        return RetryTemplate.builder()
                .maxAttempts(maxRetries)
                .exponentialBackoff(
                        Duration.ofSeconds(1),  // Initial interval
                        2.0,                    // Multiplier
                        Duration.ofSeconds(10)  // Max interval
                )
                .retryOn(org.springframework.web.client.HttpServerErrorException.class)
                .retryOn(org.springframework.web.client.ResourceAccessException.class)
                .retryOn(java.net.SocketTimeoutException.class)
                .traversingCauses()
                .withListener(new org.springframework.retry.listener.RetryListenerSupport() {
                    @Override
                    public <T, E extends Throwable> void onError(
                            org.springframework.retry.RetryContext context,
                            org.springframework.retry.RetryCallback<T, E> callback,
                            Throwable throwable) {
                        log.warn("Admin AI API call failed (attempt {}/{}): {}",
                                context.getRetryCount() + 1,
                                maxRetries,
                                throwable.getMessage());
                    }
                })
                .build();
    }

    /**
     * RestClient for OpenAI API with timeout configuration
     * Used by OpenAiApi for HTTP calls
     *
     * @return RestClient with timeout settings
     */
    @Bean
    @Qualifier("adminOpenAiRestClient")
    public RestClient adminOpenAiRestClient() {
        log.info("Configuring Admin OpenAI RestClient with timeout={}s", timeoutSeconds);

        return RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(10));      // Connection timeout
                    setReadTimeout(Duration.ofSeconds(timeoutSeconds)); // Read timeout
                }})
                .build();
    }
}

