# Service  Engine

> **Agentic AI SIEM Platform** — ingests security telemetry, investigates it with AI agents, and reports findings to SOC analysts.

The **Service SIEM Engine** is a Spring Boot application that ingests UDP security telemetry (syslog/NetFlow), normalizes it into a common schema, and routes it through specialized AI agents powered by **Ollama (`deepseek-r1`)**. Normalized events are published to **Kafka**, while analyst sessions and RBAC permissions are cached in **Redis**.

Built on **Hexagonal (Ports & Adapters) + Clean Architecture**, the platform keeps business logic framework-agnostic and infrastructure swappable.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Testing](#testing)
- [Roadmap](#roadmap)

---

## Features

- **UDP ingestion** — high-throughput syslog/NetFlow datagram intake.
- **Event normalization** — raw telemetry mapped to a unified `SecurityEvent` model.
- **Agentic AI pipeline** — orchestrator routes tasks to specialized agents.
- **LLM-powered analysis** — `deepseek-r1` via Ollama with a tool-calling loop.
- **Threat-intel tools** — IP reputation and GeoIP enrichment (pluggable).
- **Kafka event bus** — normalized events published to `analytics-events`.
- **Redis state** — analyst sessions and RBAC permissions cached.
- **Observability** — Spring Actuator health, info, and metrics endpoints.
- **Clean architecture** — strict dependency rules for testability and swappability.

---

## Architecture

The system follows **Hexagonal (Ports & Adapters) + Clean Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│  DOMAIN (pure Java, zero framework deps)                    │
│  - model: SecurityEvent, ThreatFinding, AgentTask,          │
│           AgentResult                                       │
│  - agent: Agent, Tool (interfaces)                          │
│  - port: MessagePublisher, SessionStore, PermissionStore,  │
│          LlmClient, ToolExecutor                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ (implements ports)
┌──────────────────────────▼──────────────────────────────────┐
│  APPLICATION (use cases, orchestration)                     │
│  - agent: OrchestratorAgent, IngestionAgent, AnalysisAgent, │
│           ReportingAgent                                    │
│  - service: MessageProcessingService, SessionService,       │
│             PermissionService                               │
└──────────────────────────┬──────────────────────────────────┘
                           │ (adapters)
┌──────────────────────────▼──────────────────────────────────┐
│  INFRASTRUCTURE (adapters + config)                         │
│  - udp: UdpServer, UdpMessageHandler                        │
│  - kafka: KafkaMessagePublisher                             │
│  - redis: RedisSessionStore, RedisPermissionStore           │
│  - ollama: OllamaLlmClient                                  │
│  - tool: ToolRegistry, IpReputationTool, GeoIpTool          │
│  - config: KafkaConfig, ApplicationConfig                   │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Rule

- `domain` → no dependencies
- `application` → depends only on `domain`
- `infrastructure` → depends on `application` ports and `domain`

Business logic never imports Kafka, Redis, or Ollama classes directly — it depends on **ports** (interfaces). Swapping Kafka for RabbitMQ, or Ollama for OpenAI, is a one-class change in the infrastructure layer.

For a deeper dive, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.x |
| Build | Gradle |
| LLM | Ollama (`deepseek-r1`) |
| Messaging | Apache Kafka (Spring Kafka) |
| Cache/State | Redis (Spring Data Redis, Lettuce) |
| HTTP client | Spring WebFlux / WebClient |
| Resilience | Resilience4j |
| Observability | Spring Actuator + Micrometer |

---

## Prerequisites

- **Java 17** or later
- **Docker** & **Docker Compose** (for Kafka, Redis, Ollama)
- **Gradle** (wrapper included — no global install needed)

---

## Getting Started

### 1. Start infrastructure

```bash
docker compose up -d
```

This launches Kafka, Redis, and Ollama.

### 2. Pull the LLM model

```bash
docker exec -it <ollama-container> ollama pull deepseek-r1
```

> Find the container name with `docker ps`.

### 3. Run the application

```bash
./gradlew bootRun
```

The engine starts listening for UDP datagrams on port `8080` (configurable).

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
```

---

## Configuration

All settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml) and are overridable via environment variables:

| Property | Env var | Default |
|----------|---------|---------|
| `udp.port` | `UDP_PORT` | `8080` |
| `udp.buffer-size` | — | `2048` |
| `ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `ollama.model` | `OLLAMA_MODEL` | `deepseek-r1` |
| `kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `kafka.topic` | `KAFKA_TOPIC` | `analytics-events` |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` |

---

## Project Structure

```
src/main/java/com/servuce/analytics/
├── ServuceAnalyticsEngineApplication.java   # Entry point
├── domain/                                  # Pure business logic (no framework deps)
│   ├── model/                               # SecurityEvent, ThreatFinding, AgentTask, AgentResult
│   ├── agent/                               # Agent, Tool interfaces
│   └── port/                                # MessagePublisher, SessionStore, PermissionStore, LlmClient, ToolExecutor
├── application/                             # Use cases & orchestration
│   ├── agent/                               # OrchestratorAgent, IngestionAgent, AnalysisAgent, ReportingAgent
│   └── service/                             # MessageProcessingService, SessionService, PermissionService
└── infrastructure/                          # Adapters & configuration
    ├── udp/                                 # UdpServer, UdpMessageHandler
    ├── kafka/                               # KafkaMessagePublisher
    ├── redis/                               # RedisSessionStore, RedisPermissionStore
    ├── ollama/                              # OllamaLlmClient
    ├── tool/                                # ToolRegistry, IpReputationTool, GeoIpTool
    └── config/                              # KafkaConfig, ApplicationConfig
```

---

## How It Works

### Data Flow

```
[UDP Sender] ──UDP:8080──▶ UdpServer ──▶ UdpMessageHandler
                                              │ parse + normalize
                                              ▼
                                     SecurityEvent
                                              │
                                              ▼
                              MessagePublisher (Kafka: analytics-events)
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

### Agents

| Agent | Role | Dependencies (ports) |
|-------|------|----------------------|
| `OrchestratorAgent` | Routes tasks to specialized agents | Other agents |
| `IngestionAgent` | Publishes normalized events | `MessagePublisher` |
| `AnalysisAgent` | Investigates events via LLM + tools | `LlmClient`, `ToolExecutor` |
| `ReportingAgent` | Generates incident reports | `LlmClient` |

### Tool-Calling Loop (`AnalysisAgent`)

```
1. Send prompt + tool definitions to Ollama
2. If response contains tool_calls:
     a. Execute each tool via ToolExecutor
     b. Append tool results to the conversation
     c. Re-send to Ollama
3. Return final assistant message
```

### Tools

| Tool | Purpose |
|------|---------|
| `lookup_ip_reputation` | IP reputation (abuseIPDB/VirusTotal) — stub |
| `enrich_geoip` | IP geolocation (MaxMind) — stub |

Tools implement the `Tool` interface and are auto-registered in `ToolRegistry` via Spring's `List<Tool>` injection.

---

## Testing

```bash
./gradlew test
```

The test suite uses JUnit 5, Spring Boot Test, Reactor Test, and Spring Kafka Test.

---

## Roadmap

| Current module | Future service |
|----------------|----------------|
| `udp/` + `IngestionAgent` | `ingestion-service` |
| `AnalysisAgent` | `analysis-service` |
| `ReportingAgent` | `reporting-service` |
| `OrchestratorAgent` | `orchestrator-service` |
| `ollama/` | `ollama-gateway` |
| `tool/` | `tool-registry` |

The hexagonal boundaries make this split mechanical: each module already depends only on ports, so extracting a service means moving the module and wiring its ports to remote adapters (REST/gRPC).

---

## License

Proprietary — © Servuce. All rights reserved.
