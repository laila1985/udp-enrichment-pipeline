package com.service.analytics.infrastructure.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration. Exposes a named {@link Retry} for the Ollama
 * adapter so that transient LLM/network failures are retried with backoff.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Retry ollamaRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        return RetryRegistry.of(config).retry("ollama");
    }
}
