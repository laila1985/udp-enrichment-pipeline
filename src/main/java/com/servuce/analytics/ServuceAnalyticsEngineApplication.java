package com.servuce.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Servuce Analytics Engine — an agentic AI SIEM platform
 * that ingests UDP telemetry, processes it through specialized AI agents
 * (Ollama deepseek-r1), publishes to Kafka, and caches state in Redis.
 */
@SpringBootApplication
public class ServuceAnalyticsEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServuceAnalyticsEngineApplication.class, args);
    }
}