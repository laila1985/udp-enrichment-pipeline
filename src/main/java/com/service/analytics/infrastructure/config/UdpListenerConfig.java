package com.service.analytics.infrastructure.config;

import com.service.analytics.infrastructure.udp.UdpMessageHandler;
import com.service.analytics.infrastructure.udp.listener.NettyUdpListener;
import com.service.analytics.infrastructure.udp.listener.NioUdpListener;
import com.service.analytics.infrastructure.udp.listener.SimpleUdpListener;
import com.service.analytics.infrastructure.udp.listener.UdpListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link UdpListener} implementation based on the
 * {@code udp.listener.mode} property ({@code simple | nio | netty}).
 *
 * <p>Exactly one listener bean is registered per runtime; the chosen listener is
 * started on application startup and stopped on shutdown.</p>
 */
@Configuration
public class UdpListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(UdpListenerConfig.class);

    private final UdpListener listener;

    public UdpListenerConfig(UdpListener listener) {
        this.listener = listener;
    }

    @Bean
    @ConditionalOnProperty(name = "udp.listener.mode", havingValue = "simple")
    public UdpListener simpleUdpListener(
            UdpMessageHandler handler,
            @Value("${udp.port}") int port,
            @Value("${udp.buffer-size}") int bufferSize) {
        return new SimpleUdpListener(handler, port, bufferSize);
    }

    @Bean
    @ConditionalOnProperty(name = "udp.listener.mode", havingValue = "nio", matchIfMissing = true)
    public UdpListener nioUdpListener(
            UdpMessageHandler handler,
            @Value("${udp.port}") int port,
            @Value("${udp.buffer-size}") int bufferSize) {
        return new NioUdpListener(handler, port, bufferSize);
    }

    @Bean
    @ConditionalOnProperty(name = "udp.listener.mode", havingValue = "netty")
    public UdpListener nettyUdpListener(
            UdpMessageHandler handler,
            @Value("${udp.port}") int port,
            @Value("${udp.buffer-size}") int bufferSize) {
        return new NettyUdpListener(handler, port, bufferSize);
    }

    @PostConstruct
    public void start() throws Exception {
        listener.start();
    }

    @PreDestroy
    public void stop() {
        listener.stop();
    }
}
