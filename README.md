# Servuce Analytics Engine

An **agentic AI SIEM platform** built on Spring Boot. It ingests UDP security telemetry, processes it through specialized AI agents powered by **Ollama (`deepseek-r1`)**, publishes normalized events to **Kafka**, and caches session/permission state in **Redis**.

Built with **Hexagonal (Ports & Adapters) + Clean Architecture** — business logic is fully testable and infrastructure is swappable.

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Agents](#agents)
5. [Tools](#tools)
6. [Prerequisites](#prerequisites)
7. [Quick Start](#quick-start)
8. [Configuration](#configuration)
9. [Testing UDP Ingestion](#testing-udp-ingestion)
10. [Project Structure](#project-structure)
11. [Extending the System](#extending-the-system)
12. [Roadmap](#roadmap)

---

## Overview

The platform models a real-world **SIEM (Security Information and Event Management)** use case. A firewall emits syslog messages over UDP whenever it blocks a connection. The engine:

1. **Ingests** thousands of datagrams per second (UDP).
2. **Normalizes** them into a common `SecurityEvent` schema.
3. **Detects** suspicious patterns (port scans, brute force, C2 beaconing).
4. **Investigates** using AI agents that call threat-intel tools.
5. **Reports** findings to SOC analysts.
6. **Caches** analyst sessions and enforces RBAC permissions.

## Features

- **UDP ingestion** — non-blocking `DatagramChannel` on port 8080
- **Agentic AI pipeline** — Orchestrator, Ingestion, Analysis, and Reporting agents
- **Ollama integration** — native tool calling with `deepseek-r1`
- **Tool framework** — threat-intel lookups (IP reputation, GeoIP) callable by agents
- **Kafka** — event bus for normalized events
- **Redis** — analyst session caching and RBAC permissions
- **Hexagonal architecture** — clean, testable, swappable infrastructure
- **Observability** — Spring Actuator + Micrometer

## Architecture

The system follows a strict three-layer dependency rule:

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

**Dependency rule:** `domain` → no deps · `application` → `domain` only · `infrastructure` → `application` ports + `domain`.

Business logic never imports Kafka, Redis, or Ollama directly — it depends on **ports** (interfaces). Swapping Kafka for RabbitMQ, or Ollama for OpenAI, is a one-class change in the infrastructure layer.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full design.

## Agents

| Agent | Role | Dependencies (ports) |
|-------|------|----------------------|
| `OrchestratorAgent` | Routes tasks to specialized agents by task type | Other agents |
| `IngestionAgent` | Publishes normalized events to the bus | `MessagePublisher` |
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

## Tools

Tools implement the `Tool` interface and are auto-registered in `ToolRegistry` via Spring's `List<Tool>` injection.

| Tool | Purpose | Status |
|------|---------|--------|
| `lookup_ip_reputation` | IP reputation (abuseIPDB/VirusTotal) | Stub |
| `enrich_geoip` | IP geolocation (MaxMind) | Stub |

To add a new tool, implement `Tool` and annotate it with `@Component` — it is automatically available to agents.

## Prerequisites

- **Java 17+** (JDK 21 recommended)
- **Docker** (for Kafka, Redis, Ollama)

## Quick Start

```bash
# 1. Start infrastructure (Kafka + Redis + Ollama)
docker compose up -d

# 2. Pull the model
docker exec -it $(docker compose ps -q ollama) ollama pull deepseek-r1

# 3. Run the application
./gradlew bootRun
```

The application starts and the UDP server begins listening on port `8080`.

## Configuration

All settings live in `src/main/resources/application.yml` and are overridable via environment variables:

| Property | Env var | Default |
|----------|---------|---------|
| UDP port | `UDP_PORT` | `8080` |
| UDP buffer size | — | `2048` |
| Ollama URL | `OLLAMA_BASE_URL` | `http://localhost:11434` |
| Ollama model | `OLLAMA_MODEL` | `deepseek-r1` |
| Kafka brokers | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| Kafka topic | `KAFKA_TOPIC` | `analytics-events` |
| Redis host | `REDIS_HOST` | `localhost` |
| Redis port | `REDIS_PORT` | `6379` |

Example override:

```bash
UDP_PORT=9000 OLLAMA_MODEL=llama3.1 ./gradlew bootRun
```

## Testing UDP Ingestion

Send a JSON datagram to the UDP listener:

```bash
echo -n '{"eventType":"firewall_block","destinationIp":"10.0.0.5","destinationPort":22}' \
  | nc -u -w1 localhost 8080
```

The message is parsed into a `SecurityEvent` and published to the `analytics-events` Kafka topic.

## Project Structure

```
src/main/java/com/servuce/analytics/
├── domain/
│   ├── model/          SecurityEvent, ThreatFinding, AgentTask, AgentResult
│   ├── agent/          Agent, Tool (interfaces)
│   └── port/           MessagePublisher, SessionStore, PermissionStore,
│                       LlmClient, ToolExecutor
├── application/
│   ├── agent/          OrchestratorAgent, IngestionAgent, AnalysisAgent, ReportingAgent
│   └── service/        MessageProcessingService, SessionService, PermissionService
└── infrastructure/
    ├── udp/            UdpServer, UdpMessageHandler
    ├── kafka/          KafkaMessagePublisher
    ├── redis/          RedisSessionStore, RedisPermissionStore
    ├── ollama/         OllamaLlmClient
    ├── tool/           ToolRegistry, IpReputationTool, GeoIpTool
    └── config/         KafkaConfig, ApplicationConfig
```

## Extending the System

### Add a new agent
1. Implement `Agent` in `application/agent/`.
2. Register it in `ApplicationConfig` and (optionally) in `OrchestratorAgent`.

### Add a new tool
1. Implement `Tool` in `infrastructure/tool/`.
2. Annotate with `@Component` — it is auto-registered.

### Swap infrastructure
- **Kafka → RabbitMQ:** implement `MessagePublisher` and replace the bean.
- **Ollama → OpenAI:** implement `LlmClient` and replace the bean.
- **Redis → Memcached:** implement `SessionStore`/`PermissionStore` and replace the beans.

No changes to the domain or application layers are required.

## Roadmap

- [ ] Split monolith into microservices (ingestion, analysis, reporting, orchestrator, ollama-gateway, tool-registry)
- [ ] Wire threat-intel tools to real APIs (abuseIPDB, VirusTotal, MaxMind)
- [ ] Add Kafka consumer for downstream analysis pipeline
- [ ] Add structured LLM output parsing (JSON schema) for findings
- [ ] Add authentication/authorization on top of the permission store

## License

Proprietary — internal use.