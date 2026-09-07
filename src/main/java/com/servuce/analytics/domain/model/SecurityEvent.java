package com.servuce.analytics.domain.model;

import java.time.Instant;

/**
 * Normalized security event produced from a raw UDP syslog/telemetry datagram.
 * This is a pure domain object with no framework dependencies.
 */
public record SecurityEvent(
        String eventId,
        String sourceIp,
        String destinationIp,
        int sourcePort,
        int destinationPort,
        String protocol,
        String eventType,
        Instant timestamp,
        String rawPayload
) {
}