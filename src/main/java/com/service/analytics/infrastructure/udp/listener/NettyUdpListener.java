package com.service.analytics.infrastructure.udp.listener;

import com.service.analytics.infrastructure.udp.UdpMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Event-driven UDP listener built on the Netty framework. A {@code NioEventLoopGroup}
 * dispatches incoming datagrams to the {@link UdpMessageHandler} across multiple
 * event-loop threads, making this the most scalable option for high-throughput ingestion.
 */
public class NettyUdpListener extends AbstractUdpListener {

    private static final Logger log = LoggerFactory.getLogger(NettyUdpListener.class);

    private EventLoopGroup group;
    private Channel channel;

    public NettyUdpListener(UdpMessageHandler handler, int port, int bufferSize) {
        super(handler, port, bufferSize);
    }

    @Override
    public void start() throws Exception {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 25 * 1024 * 1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(new DatagramHandler());
                    }
                });

        channel = bootstrap.bind(new InetSocketAddress(port)).sync().channel();
        running.set(true);
        log.info("NettyUdpListener listening on port {}", port);
    }

    @Override
    public void stop() {
        running.set(false);
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("NettyUdpListener stopped");
    }

    private class DatagramHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            java.nio.ByteBuffer buf = packet.content().nioBuffer();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            handler.handle(data, packet.sender());
        }
    }
}
