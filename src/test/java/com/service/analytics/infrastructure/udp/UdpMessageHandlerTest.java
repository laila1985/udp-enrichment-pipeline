package com.service.analytics.infrastructure.udp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.analytics.domain.model.SecurityEvent;
import com.service.analytics.domain.port.MessagePublisher;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpMessageHandlerTest {

    @Test
    void parsesJsonFieldsInsteadOfFabricatingFromSender() {
        AtomicReference<SecurityEvent> captured = new AtomicReference<>();
        MessagePublisher publisher = captured::set;
        UdpMessageHandler handler = new UdpMessageHandler(new ObjectMapper(), publisher);

        String json = "{\"eventId\":\"evt-1\",\"sourceIp\":\"10.0.0.5\",\"destinationIp\":\"203.0.113.9\"," +
                "\"sourcePort\":5555,\"destinationPort\":443,\"protocol\":\"TCP\",\"eventType\":\"scan\"," +
                "\"timestamp\":\"2024-01-01T00:00:00Z\"}";
        handler.handle(json.getBytes(StandardCharsets.UTF_8),
                new InetSocketAddress("192.168.1.1", 9999));

        SecurityEvent e = captured.get();
        assertNotNull(e);
        assertEquals("evt-1", e.eventId());
        assertEquals("10.0.0.5", e.sourceIp());
        assertEquals("203.0.113.9", e.destinationIp());
        assertEquals(5555, e.sourcePort());
        assertEquals(443, e.destinationPort());
        assertEquals("TCP", e.protocol());
        assertEquals("scan", e.eventType());
    }

    @Test
    void fallsBackToSenderWhenFieldsMissing() {
        AtomicReference<SecurityEvent> captured = new AtomicReference<>();
        MessagePublisher publisher = captured::set;
        UdpMessageHandler handler = new UdpMessageHandler(new ObjectMapper(), publisher);

        handler.handle("{}".getBytes(StandardCharsets.UTF_8),
                new InetSocketAddress("192.168.1.1", 9999));

        SecurityEvent e = captured.get();
        assertNotNull(e);
        assertEquals("192.168.1.1", e.sourceIp());
        assertEquals(9999, e.sourcePort());
        assertNotNull(e.eventId());
    }

    @Test
    void doesNotPublishOnMalformedPayload() {
        AtomicReference<SecurityEvent> captured = new AtomicReference<>();
        MessagePublisher publisher = captured::set;
        UdpMessageHandler handler = new UdpMessageHandler(new ObjectMapper(), publisher);

        handler.handle("not-json".getBytes(StandardCharsets.UTF_8),
                new InetSocketAddress("192.168.1.1", 9999));

        assertTrue(captured.get() == null);
    }
}
