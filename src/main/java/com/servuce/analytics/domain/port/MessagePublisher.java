package com.servuce.analytics.domain.port;

import com.servuce.analytics.domain.model.SecurityEvent;

/**
 * Port for publishing normalized events to a message bus (Kafka in production).
 * The domain depends on this interface, not on any concrete messaging technology.
 */
public interface MessagePublisher {
    void publish(SecurityEvent event);
}