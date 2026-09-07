package com.servuce.analytics.infrastructure.udp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Non-blocking UDP server that listens for syslog/telemetry datagrams.
 * Each received datagram is handed to the UdpMessageHandler on a dedicated
 * receive thread.
 */
@Component
public class UdpServer {

    private static final Logger log = LoggerFactory.getLogger(UdpServer.class);

    private final UdpMessageHandler messageHandler;
    private final int port;
    private final int bufferSize;

    private DatagramChannel channel;
    private Thread receiveThread;
    private volatile boolean running = false;

    public UdpServer(
            UdpMessageHandler messageHandler,
            @Value("${udp.port}") int port,
            @Value("${udp.buffer-size}") int bufferSize) {
        this.messageHandler = messageHandler;
        this.port = port;
        this.bufferSize = bufferSize;
    }

    @PostConstruct
    public void start() throws IOException {
        channel = DatagramChannel.open();
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.bind(new InetSocketAddress(port));
        channel.configureBlocking(false);

        running = true;
        receiveThread = new Thread(this::receiveLoop, "udp-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        log.info("UDP server listening on port {}", port);
    }

    private void receiveLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        while (running) {
            try {
                buffer.clear();
                InetSocketAddress sender = (InetSocketAddress) channel.receive(buffer);
                if (sender != null) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    messageHandler.handle(data, sender);
                }
            } catch (IOException e) {
                if (running) {
                    log.error("UDP receive error", e);
                }
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                log.warn("Error closing UDP channel", e);
            }
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        log.info("UDP server stopped");
    }
}