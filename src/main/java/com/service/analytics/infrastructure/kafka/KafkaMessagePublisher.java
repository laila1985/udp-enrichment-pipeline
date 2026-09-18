package com.service.analytics.infrastructure.kafka;

import com.service.analytics.domain.model.SecurityEvent;
import com.service.analytics.domain.port.MessagePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the MessagePublisher port against Kafka.
 */
@Component
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, SecurityEvent> kafkaTemplate;
    private final String topic;

    public KafkaMessagePublisher(
            KafkaTemplate<String, SecurityEvent> kafkaTemplate,
            @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(SecurityEvent event) {
        kafkaTemplate.send(topic, event.eventId(), event);
    }
}