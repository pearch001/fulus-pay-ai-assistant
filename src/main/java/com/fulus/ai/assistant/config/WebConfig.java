package com.fulus.ai.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${spring.web.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${spring.web.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${spring.web.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${spring.web.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${spring.web.cors.max-age}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] headers;
        if ("*".equals(allowedHeaders)) {
            headers = new String[]{"*"};
        } else {
            headers = allowedHeaders.split(",");
        }

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(headers)
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}
