package com.servuce.analytics.domain.model;

import java.util.Map;

/**
 * A unit of work dispatched to an agent.
 * Carries the task type, the event under investigation, and optional context.
 */
public record AgentTask(
        String taskId,
        TaskType type,
        SecurityEvent event,
        Map<String, Object> context
) {
    public enum TaskType {
        INGEST, ANALYZE, REPORT, ORCHESTRATE
    }
}