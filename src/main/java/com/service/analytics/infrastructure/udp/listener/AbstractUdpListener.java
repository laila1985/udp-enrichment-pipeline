package com.service.analytics.infrastructure.udp.listener;

import com.service.analytics.infrastructure.udp.UdpMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared base for UDP listeners: holds the message handler, port, buffer size,
 * and the running flag used to control the receive-loop lifecycle.
 */
public abstract class AbstractUdpListener implements UdpListener {

    protected final UdpMessageHandler handler;
    protected final int port;
    protected final int bufferSize;
    protected final AtomicBoolean running = new AtomicBoolean(false);

    protected AbstractUdpListener(UdpMessageHandler handler, int port, int bufferSize) {
        this.handler = handler;
        this.port = port;
        this.bufferSize = bufferSize;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
