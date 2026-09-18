package com.service.analytics.infrastructure.udp.listener;

import com.service.analytics.infrastructure.udp.UdpMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Blocking UDP listener built on the classic {@code java.net.DatagramSocket}
 * API. Runs a single receive thread; simplest to reason about and best suited
 * to low-throughput or resource-constrained deployments.
 */
public class SimpleUdpListener extends AbstractUdpListener {

    private static final Logger log = LoggerFactory.getLogger(SimpleUdpListener.class);

    private DatagramSocket socket;
    private Thread receiveThread;

    public SimpleUdpListener(UdpMessageHandler handler, int port, int bufferSize) {
        super(handler, port, bufferSize);
    }

    @Override
    public void start() throws Exception {
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        running.set(true);
        receiveThread = new Thread(this::receiveLoop, "udp-simple-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();
        log.info("SimpleUdpListener listening on port {}", port);
    }

    private void receiveLoop() {
        byte[] buffer = new byte[bufferSize];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                handler.handle(data, (InetSocketAddress) packet.getSocketAddress());
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
        if (socket != null) {
            socket.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        log.info("SimpleUdpListener stopped");
    }
}
