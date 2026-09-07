package com.servuce.analytics.infrastructure.udp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servuce.analytics.domain.model.SecurityEvent;
import com.servuce.analytics.domain.port.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Parses raw UDP datagrams into normalized SecurityEvent objects and
 * publishes them via the MessagePublisher port.
 */
@Component
public class UdpMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(UdpMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final MessagePublisher messagePublisher;

    public UdpMessageHandler(ObjectMapper objectMapper, MessagePublisher messagePublisher) {
        this.objectMapper = objectMapper;
        this.messagePublisher = messagePublisher;
    }

    public void handle(byte[] data, InetSocketAddress sender) {
        String payload = new String(data, StandardCharsets.UTF_8);
        try {
            SecurityEvent event = parse(payload, sender);
            messagePublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to parse UDP message from {}: {}", sender, e.getMessage());
        }
    }

    private SecurityEvent parse(String payload, InetSocketAddress sender) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        return new SecurityEvent(
                node.path("eventId").asText(UUID.randomUUID().toString()),
                node.path("sourceIp").asText(sender.getAddress().getHostAddress()),
                node.path("destinationIp").asText(""),
                node.path("sourcePort").asInt(sender.getPort()),
                node.path("destinationPort").asInt(0),
                node.path("protocol").asText("UDP"),
                node.path("eventType").asText("unknown"),
                Instant.now(),
                payload
        );
    }
}