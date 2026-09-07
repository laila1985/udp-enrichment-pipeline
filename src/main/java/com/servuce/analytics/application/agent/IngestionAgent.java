package com.servuce.analytics.application.agent;

import com.servuce.analytics.domain.agent.Agent;
import com.servuce.analytics.domain.model.AgentResult;
import com.servuce.analytics.domain.model.AgentTask;
import com.servuce.analytics.domain.port.MessagePublisher;

/**
 * Ingestion agent: receives a normalized security event and publishes it
 * to the message bus for downstream analysis. Depends only on the
 * MessagePublisher port, not on Kafka directly.
 */
public class IngestionAgent implements Agent {

    private final MessagePublisher messagePublisher;

    public IngestionAgent(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Override
    public AgentResult execute(AgentTask task) {
        if (task.event() == null) {
            return AgentResult.failure(task.taskId(), "No event to ingest");
        }
        messagePublisher.publish(task.event());
        return AgentResult.report(task.taskId(), "Event ingested: " + task.event().eventId());
    }
}