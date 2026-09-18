package com.service.analytics.infrastructure.udp.listener;

import com.service.analytics.infrastructure.udp.UdpMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Non-blocking UDP listener built on {@code java.nio.channels.DatagramChannel}.
 * Uses the modern NIO channel API with a single receive thread and no extra
 * dependencies. This is the default listener.
 */
public class NioUdpListener extends AbstractUdpListener {

    private static final Logger log = LoggerFactory.getLogger(NioUdpListener.class);

    private DatagramChannel channel;
    private Thread receiveThread;

    public NioUdpListener(UdpMessageHandler handler, int port, int bufferSize) {
        super(handler, port, bufferSize);
    }

    @Override
    public void start() throws Exception {
        channel = DatagramChannel.open();
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        running.set(true);
        receiveThread = new Thread(this::receiveLoop, "udp-nio-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();
        log.info("NioUdpListener listening on port {}", port);
    }

    private void receiveLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        while (running.get()) {
            try {
                buffer.clear();
                InetSocketAddress sender = (InetSocketAddress) channel.receive(buffer);
                if (sender != null) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    handler.handle(data, sender);
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("UDP receive error", e);
                }
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                log.warn("Error closing UDP channel", e);
            }
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        log.info("NioUdpListener stopped");
    }
}
