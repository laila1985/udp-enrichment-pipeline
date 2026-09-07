package com.servuce.analytics.domain.model;

import java.util.List;

/**
 * Outcome of an agent's execution.
 * May contain a threat finding, a report, or intermediate results.
 */
public record AgentResult(
        String taskId,
        boolean success,
        ThreatFinding finding,
        String report,
        List<String> messages
) {
    public static AgentResult success(String taskId, ThreatFinding finding) {
        return new AgentResult(taskId, true, finding, null, List.of());
    }

    public static AgentResult report(String taskId, String report) {
        return new AgentResult(taskId, true, null, report, List.of());
    }

    public static AgentResult failure(String taskId, String error) {
        return new AgentResult(taskId, false, null, null, List.of(error));
    }
}