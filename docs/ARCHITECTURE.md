# UDP Enrichment Pipeline — Architecture

## 1. Overview

The **UDP Enrichment Pipeline** is an **agentic AI SIEM platform** built on Spring Boot. It ingests UDP security telemetry (syslog/NetFlow), normalizes it into a unified schema, drives it through specialized AI agents powered by **Ollama (`deepseek-r1`)**, publishes normalized events to **Kafka**, and caches analyst sessions and access permissions in **Redis**.

The system follows **Hexagonal (Ports & Adapters) + Clean Architecture** with strict dependency rules, making business logic fully testable and infrastructure swappable.

## 2. Real-World Use Case: "Sentinel" SIEM

A firewall emits syslog messages over UDP whenever it blocks a connection. The platform:

1. **Ingests** thousands of datagrams per second (UDP).
2. **Normalizes** them into a common `SecurityEvent` schema.
3. **Publishes** them to Kafka for durable downstream consumption.
4. **Investigates** using AI agents that call threat-intel tools.
5. **Reports** findings to SOC analysts.
6. **Caches** analyst sessions and enforces RBAC permissions.

## 3. Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│  DOMAIN (pure Java, zero framework deps)                    │
│  - model: SecurityEvent, ThreatFinding, AgentTask,          │
│           AgentResult                                       │
│  - agent: Agent, Tool (interfaces)                          │
│  - port: MessagePublisher, SessionStore, PermissionStore,   │
│           LlmClient, ToolExecutor                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ (implements ports)
┌──────────────────────────▼──────────────────────────────────┐
│  APPLICATION (use cases, orchestration)                     │
│  - agent: OrchestratorAgent, IngestionAgent, AnalysisAgent, │
│           ReportingAgent                                    │
│  - service: SessionService, PermissionService               │
└──────────────────────────┬──────────────────────────────────┘
                           │ (adapters)
┌──────────────────────────▼──────────────────────────────────┐
│  INFRASTRUCTURE (adapters + config)                         │
│  - udp: UdpListener (+ Simple/Nio/Netty), UdpMessageHandler │
│  - kafka: KafkaMessagePublisher, EventAnalysisConsumer      │
│  - redis: RedisSessionStore, RedisPermissionStore           │
│  - ollama: OllamaLlmClient                                  │
│  - tool: ToolRegistry, IpReputationTool, GeoIpTool          │
│  - web: SessionController, PermissionController             │
│  - config: KafkaConfig, ApplicationConfig, ResilienceConfig │
└─────────────────────────────────────────────────────────────┘
```

## 4. Dependency Rule

- `domain` → no dependencies
- `application` → depends only on `domain`
- `infrastructure` → depends on `application` ports and `domain`

Business logic never imports Kafka, Redis, or Ollama classes directly — it depends on **ports** (interfaces). Swapping Kafka for RabbitMQ, or Ollama for OpenAI, is a one-class change in the infrastructure layer.

## 5. Data Flow

```
[UDP Sender] ──UDP:514──▶ UdpListener (simple|nio|netty)
                                              │ parse + normalize
                                              ▼
                                      SecurityEvent
                                              │
                                              ▼
                              MessagePublisher (Kafka: analytics-events)
                                              │
                                              ▼
                                   EventAnalysisConsumer
                                              │
                                              ▼
                                     OrchestratorAgent
                                       │ route by type
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
      IngestionAgent          AnalysisAgent            ReportingAgent
      (publish)               (Ollama + tools)         (Ollama report)
              │                       │                       │
              └───────────────────────┼───────────────────────┘
                                      ▼
                          Redis (session + permissions)
```

## 6. Agents

| Agent | Role | Dependencies (ports) |
|-------|------|----------------------|
| `OrchestratorAgent` | Routes tasks to specialized agents | Other agents |
| `IngestionAgent` | Publishes normalized events | `MessagePublisher` |
| `AnalysisAgent` | Investigates events via LLM + tools | `LlmClient`, `ToolExecutor` |
| `ReportingAgent` | Generates incident reports | `LlmClient` |

### Tool-Calling Loop (AnalysisAgent)

```
1. Send prompt + tool definitions to Ollama
2. If response contains tool_calls:
     a. Execute each tool via ToolExecutor
     b. Append tool results to the conversation
     c. Re-send to Ollama
3. Return final assistant message
```

> **Result parsing note:** `AnalysisAgent` currently infers `severity` and the MITRE technique
> from the LLM output using substring heuristics (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`; `T1110`,
> `T1046`). This is intended to be replaced with structured LLM output (JSON schema) in production.

## 7. Ports (Interfaces)

| Port | Purpose | Adapter |
|------|---------|---------|
| `MessagePublisher` | Publish events to a bus | `KafkaMessagePublisher` |
| `SessionStore` | Cache analyst sessions | `RedisSessionStore` |
| `PermissionStore` | RBAC permissions | `RedisPermissionStore` |
| `LlmClient` | LLM chat + tool calling | `OllamaLlmClient` |
| `ToolExecutor` | Resolve + execute tools | `ToolRegistry` |

## 8. Pluggable UDP Listener

The ingestion endpoint is implemented by the `UdpListener` interface with three
swappable implementations, selected at startup via `udp.listener.mode`:

| Mode | Class | I/O model | Use case |
|------|-------|-----------|----------|
| `simple` | `SimpleUdpListener` | Blocking `java.net` | Low-throughput / simplest |
| `nio` (default) | `NioUdpListener` | Non-blocking `java.nio` | Balanced, zero extra deps |
| `netty` | `NettyUdpListener` | Event-driven Netty | High-throughput production |

See [`udp_listener_analysis.md`](udp_listener_analysis.md) for a detailed comparison.

## 9. Tools

| Tool | Purpose |
|------|---------|
| `lookup_ip_reputation` | IP reputation (abuseIPDB/VirusTotal) — stub |
| `enrich_geoip` | IP geolocation (MaxMind) — stub |

Tools implement the `Tool` interface and are auto-registered in `ToolRegistry` via Spring's `List<Tool>` injection.

## 10. Configuration

See `src/main/resources/application.yml`. All values are overridable via environment variables:

| Property | Env var | Default |
|----------|---------|---------|
| `server.port` | `SERVER_PORT` | `8080` |
| `udp.port` | `UDP_PORT` | `514` |
| `udp.listener.mode` | `UDP_LISTENER_MODE` | `nio` |
| `ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `ollama.model` | `OLLAMA_MODEL` | `deepseek-r1` |
| `kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `kafka.topic` | `KAFKA_TOPIC` | `analytics-events` |
| `kafka.consumer-group` | `KAFKA_CONSUMER_GROUP` | `analytics-engine` |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` |

## 11. Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| Build | Gradle |
| LLM | Ollama (`deepseek-r1`) |
| Messaging | Apache Kafka (Spring Kafka) |
| Cache/State | Redis (Spring Data Redis, Lettuce) |
| HTTP client | Spring WebFlux / WebClient |
| Resilience | Resilience4j |
| Observability | Spring Actuator + Micrometer |

## 12. REST API

The application exposes two REST adapters that drive the application-layer use cases:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/sessions/{sessionId}` | `POST` | Create/update an analyst session (body: `data`, `ttlSeconds`). |
| `/api/sessions/{sessionId}` | `GET` | Retrieve an analyst session. |
| `/api/sessions/{sessionId}` | `DELETE` | End an analyst session. |
| `/api/permissions/{userId}` | `POST` | Grant a permission (body: `permission`). |
| `/api/permissions/{userId}` | `DELETE` | Revoke a permission (body: `permission`). |
| `/api/permissions/{userId}/{permission}` | `GET` | Check if a permission is granted (returns `{"granted": true/false}`). |

## 13. Running

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Pull the model
docker exec -it <ollama-container> ollama pull deepseek-r1

# 3. Run the application
./gradlew bootRun
```

## 14. Extending the Platform

### Adding a new agent

1. Implement the `Agent` interface in the `application` layer.
2. Register it in `OrchestratorAgent` for a `TaskType`.
3. Wire it in `ApplicationConfig`.

### Adding a new threat-intel tool

1. Implement the `Tool` interface in the `infrastructure/tool` package.
2. Annotate it with `@Component` — it is auto-registered in `ToolRegistry` via Spring's
   `List<Tool>` injection.

### Swapping infrastructure

- **Kafka → RabbitMQ:** implement a new `MessagePublisher` and change the injected bean.
- **Ollama → OpenAI:** implement a new `LlmClient` and change the injected bean.
- **Redis → another store:** implement new `SessionStore` / `PermissionStore` adapters.

Because all business logic depends on ports (interfaces), these swaps never touch the domain or
application layers.

## 15. Future Microservice Split

| Current module | Future service |
|----------------|----------------|
| `udp/` + `IngestionAgent` | `ingestion-service` |
| `AnalysisAgent` | `analysis-service` |
| `ReportingAgent` | `reporting-service` |
| `OrchestratorAgent` | `orchestrator-service` |
| `ollama/` | `ollama-gateway` |
| `tool/` | `tool-registry` |

The hexagonal boundaries make this split mechanical: each module already depends only on ports, so extracting a service means moving the module and wiring its ports to remote adapters (REST/gRPC).