package com.service.analytics.infrastructure.udp.listener;

/**
 * Contract for the UDP ingestion listener. Implementations may use blocking
 * I/O ({@link SimpleUdpListener}), non-blocking NIO ({@link NioUdpListener}),
 * or the Netty event-driven framework ({@link NettyUdpListener}).
 *
 * <p>The active implementation is selected at startup via the
 * {@code udp.listener.mode} property ({@code simple | nio | netty}).</p>
 */
public interface UdpListener {

    /** Bind the socket/channel and begin receiving datagrams. */
    void start() throws Exception;

    /** Stop receiving and release underlying resources. */
    void stop();

    /** @return {@code true} while the listener is actively receiving. */
    boolean isRunning();
}
