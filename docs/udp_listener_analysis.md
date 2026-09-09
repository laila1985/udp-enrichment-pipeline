Here are the differences between the three UDP listeners in your project. All three extend `AbstractUdpListener` and receive syslog messages over UDP, but they use different underlying I/O approaches:

## 1. SimpleUdpListener (Blocking I/O — `java.net`)
- Uses the classic `java.net.DatagramSocket` + `DatagramPacket` API.
- **Blocking** receive loop: `serverSocket.receive(receivePacket)` blocks the thread until a packet arrives.
- Runs on a single thread (the listener thread itself).
- Simplest implementation, easy to understand, but one thread handles everything sequentially.
- Buffer: fixed `byte[2048]`.

## 2. NioUdpListener (Non-blocking I/O — `java.nio`)
- Uses `java.nio.channels.DatagramChannel` with `ByteBuffer`.
- The channel is **not** configured as non-blocking (the `configureBlocking(true)` line is commented out), so `channel.receive(buffer)` actually blocks too — but the code is written to handle a `null` return (non-blocking style) with a commented-out `Thread.sleep(50)` to reduce CPU usage.
- Uses `StandardSocketOptions.SO_RCVBUF` to set the receive buffer.
- More modern NIO API, gives finer control over buffers and socket options, and is the foundation for scalable non-blocking designs (though here it's effectively blocking).
- Buffer: `ByteBuffer.allocate(2048)`.

## 3. NettyUdpListener (Event-driven framework — Netty)
- Uses the **Netty** framework (`Bootstrap`, `NioEventLoopGroup`, `NioDatagramChannel`).
- **Event-driven / asynchronous**: instead of a manual receive loop, you register a `ChannelInitializer` and add the `syslogHandler` to the pipeline. Netty's event loop threads handle incoming packets and invoke the handler.
- Uses an `EventLoopGroup` (default 16 threads — noted in a TODO) for concurrency, so it can scale across multiple threads.
- Supports `SO_BROADCAST` and `SO_RCVBUF` via `ChannelOption`.
- Most scalable and production-ready, but heaviest in terms of dependencies and complexity.

## Summary Table

| Aspect      | Simple             | NIO                     | Netty                 |
|-------------|--------------------|-------------------------|-----------------------|
| API         | `java.net`         | `java.nio`              | Netty framework       |
| Model       | Blocking           | Blocking (NIO API)      | Event-driven / async  |
| Threads     | 1                  | 1                       | Event loop group (16) |
| Concurrency | None               | None                    | Multi-threaded        |
| Complexity  | Lowest             | Medium                  | Highest               |
| Scalability | Low                | Medium                  | High                  |

**Key takeaway:** Simple and NIO are both effectively single-threaded blocking loops (NIO just uses the newer channel API), 
while Netty is a fully asynchronous, multi-threaded event-driven implementation that can handle much higher throughput.