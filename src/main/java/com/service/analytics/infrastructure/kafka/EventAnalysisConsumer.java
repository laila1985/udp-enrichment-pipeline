package com.service.analytics.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.analytics.application.agent.OrchestratorAgent;
import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;
import com.service.analytics.domain.model.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes normalized {@link SecurityEvent} JSON from the {@code analytics-events}
 * topic and drives the agent pipeline (analyze + report) via the
 * {@link OrchestratorAgent}. This closes the loop: UDP → Kafka → agents.
 */
@Component
public class EventAnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventAnalysisConsumer.class);

    private final OrchestratorAgent orchestratorAgent;
    private final ObjectMapper objectMapper;
    private final String topic;

    public EventAnalysisConsumer(
            OrchestratorAgent orchestratorAgent,
            ObjectMapper objectMapper,
            @Value("${kafka.topic}") String topic) {
        this.orchestratorAgent = orchestratorAgent;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.consumer-group}")
    public void onEvent(String json) {
        try {
            SecurityEvent event = objectMapper.readValue(json, SecurityEvent.class);
            runAgent(AgentTask.TaskType.ANALYZE, event);
            runAgent(AgentTask.TaskType.REPORT, event);
        } catch (Exception e) {
            log.error("Failed to process event from topic {}", topic, e);
        }
    }

    private void runAgent(AgentTask.TaskType type, SecurityEvent event) {
        AgentTask task = new AgentTask(
                UUID.randomUUID().toString(), type, event, Map.of());
        AgentResult result = orchestratorAgent.execute(task);
        if (!result.success()) {
            log.warn("Agent {} failed for event {}: {}", type, event.eventId(), result.messages());
        } else {
            log.info("Agent {} completed for event {}", type, event.eventId());
        }
    }
}
