package com.servuce.analytics.domain.model;

import java.util.List;

/**
 * Result of an AI agent's investigation of a security event.
 * Contains severity, MITRE ATT&CK mapping, and the investigation steps taken.
 */
public record ThreatFinding(
        String eventId,
        Severity severity,
        String mitreTechnique,
        String summary,
        List<String> investigationSteps
) {
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}