# UDP Listener Comparison

The platform can ingest UDP datagrams using one of three pluggable `UdpListener`
implementations. All three share the same `UdpMessageHandler` (parse + normalize) and are
selected at startup via the `udp.listener.mode` property (`simple`, `nio`, or `netty`):

```yaml
udp:
  port: 514
  buffer-size: 2048
  listener:
    mode: nio   # simple | nio | netty
```

## 1. SimpleUdpListener (Blocking I/O — `java.net`)

- Uses the classic `java.net.DatagramSocket` + `DatagramPacket` API.
- **Blocking** receive loop: `socket.receive(packet)` blocks until a packet arrives.
- Single receive thread handles everything sequentially.
- Simplest to understand; best for low-throughput or resource-constrained deployments.
- Buffer: configurable via `udp.buffer-size`.

## 2. NioUdpListener (Non-blocking I/O — `java.nio`)

- Uses `java.nio.channels.DatagramChannel` with `ByteBuffer`.
- `configureBlocking(false)` with a single poll loop; handles the `null` return.
- Uses `StandardSocketOptions.SO_REUSEADDR`.
- Modern NIO API with fine control over buffers/options; no extra dependencies. **Default.**

## 3. NettyUdpListener (Event-driven framework — Netty)

- Uses `Bootstrap` + `NioEventLoopGroup` + `NioDatagramChannel`.
- **Event-driven / asynchronous**: a `SimpleChannelInboundHandler<DatagramPacket>` forwards
  each packet to `UdpMessageHandler` on an event-loop thread.
- Multi-threaded (event-loop group), so it scales across threads for high throughput.
- Supports `SO_BROADCAST` and a 25 MB `SO_RCVBUF`.

## Summary Table

| Aspect      | Simple              | NIO                      | Netty                 |
|-------------|---------------------|--------------------------|-----------------------|
| API         | `java.net`          | `java.nio`               | Netty framework       |
| Model       | Blocking            | Non-blocking (NIO API)   | Event-driven / async  |
| Threads     | 1                   | 1                        | Event-loop group      |
| Concurrency | None                | None                     | Multi-threaded        |
| Complexity  | Lowest              | Medium                   | Highest               |
| Scalability | Low                 | Medium                   | High                  |

**Key takeaway:** `simple` and `nio` are single-threaded loops (NIO uses the newer channel
API), while `netty` is a fully asynchronous, multi-threaded event-driven implementation that
can handle much higher throughput. Choose the mode that best matches your deployment's
throughput and complexity requirements via `UDP_LISTENER_MODE`.
