package com.servuce.analytics.application.service;

import com.servuce.analytics.domain.model.SecurityEvent;
import com.servuce.analytics.domain.port.MessagePublisher;

/**
 * Orchestrates the processing of a normalized security event:
 * publishes it to the message bus for downstream agents.
 */
public class MessageProcessingService {

    private final MessagePublisher messagePublisher;

    public MessageProcessingService(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    public void process(SecurityEvent event) {
        messagePublisher.publish(event);
    }
}