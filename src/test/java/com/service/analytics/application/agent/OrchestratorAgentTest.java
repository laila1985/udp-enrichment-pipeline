package com.service.analytics.application.agent;

import com.service.analytics.domain.agent.Tool;
import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;
import com.service.analytics.domain.model.SecurityEvent;
import com.service.analytics.domain.port.LlmClient;
import com.service.analytics.domain.port.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestratorAgentTest {

    private OrchestratorAgent orchestrator;

    private static final LlmClient LLM = new LlmClient() {
        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return "";
        }

        @Override
        public LlmResponse completeWithTools(String systemPrompt, String userPrompt, List<Tool> tools) {
            return new LlmResponse("LOW", List.of());
        }
    };

    private static final ToolExecutor TOOLS = new ToolExecutor() {
        @Override
        public List<Tool> availableTools() {
            return List.of();
        }

        @Override
        public Optional<Tool> findByName(String name) {
            return Optional.empty();
        }

        @Override
        public String execute(String toolName, Map<String, Object> arguments) {
            return "";
        }
    };

    @BeforeEach
    void setUp() {
        orchestrator = new OrchestratorAgent(
                new IngestionAgent(event -> {
                }),
                new AnalysisAgent(LLM, TOOLS),
                new ReportingAgent(LLM));
    }

    @Test
    void routesIngestTask() {
        SecurityEvent event = event();
        AgentResult result = orchestrator.execute(
                new AgentTask("t1", AgentTask.TaskType.INGEST, event, Map.of()));
        assertTrue(result.success());
    }

    @Test
    void routesAnalyzeTask() {
        AgentResult result = orchestrator.execute(
                new AgentTask("t1", AgentTask.TaskType.ANALYZE, event(), Map.of()));
        assertTrue(result.success());
    }

    @Test
    void failsForUnknownTaskType() {
        AgentResult result = orchestrator.execute(
                new AgentTask("t1", AgentTask.TaskType.ORCHESTRATE, event(), Map.of()));
        assertFalse(result.success());
    }

    private static SecurityEvent event() {
        return new SecurityEvent(
                "e1", "1.2.3.4", "5.6.7.8", 123, 80, "TCP", "scan", Instant.now(), "{}");
    }
}

