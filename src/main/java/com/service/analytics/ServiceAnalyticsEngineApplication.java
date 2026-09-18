package com.service.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Service Analytics Engine — an agentic AI SIEM platform
 * that ingests UDP telemetry, processes it through specialized AI agents
 * (Ollama deepseek-r1), publishes to Kafka, and caches state in Redis.
 */
@SpringBootApplication
public class ServiceAnalyticsEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAnalyticsEngineApplication.class, args);
    }
}