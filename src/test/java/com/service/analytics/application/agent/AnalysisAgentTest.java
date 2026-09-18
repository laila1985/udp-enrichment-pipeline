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

import static org.junit.jupiter.api.Assertions.*;

class AnalysisAgentTest {

    private LlmClient llmClient;
    private ToolExecutor toolExecutor;
    private AnalysisAgent agent;

    @BeforeEach
    void setUp() {
        llmClient = new LlmClient() {
            @Override
            public String complete(String systemPrompt, String userPrompt) {
                return "final";
            }

            @Override
            public LlmResponse completeWithTools(String systemPrompt, String userPrompt, List<Tool> tools) {
                return new LlmResponse("Severity: HIGH. MITRE T1110.", List.of());
            }
        };
        toolExecutor = new ToolExecutor() {
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
                return "result";
            }
        };
        agent = new AnalysisAgent(llmClient, toolExecutor);
    }

    @Test
    void failsWhenEventIsNull() {
        AgentResult result = agent.execute(new AgentTask("t1", AgentTask.TaskType.ANALYZE, null, Map.of()));
        assertFalse(result.success());
    }

    @Test
    void returnsFindingWithInferredSeverity() {
        SecurityEvent event = new SecurityEvent(
                "e1", "1.2.3.4", "5.6.7.8", 123, 80, "TCP", "scan", Instant.now(), "{}");
        AgentResult result = agent.execute(new AgentTask("t1", AgentTask.TaskType.ANALYZE, event, Map.of()));
        assertTrue(result.success());
        assertNotNull(result.finding());
        assertEquals(com.service.analytics.domain.model.ThreatFinding.Severity.HIGH, result.finding().severity());
        assertEquals("e1", result.finding().eventId());
    }

    @Test
    void reportsFailureWhenLlmReturnsError() {
        llmClient = new LlmClient() {
            @Override
            public String complete(String systemPrompt, String userPrompt) {
                return null;
            }

            @Override
            public LlmResponse completeWithTools(String systemPrompt, String userPrompt, List<Tool> tools) {
                return LlmResponse.error("boom");
            }
        };
        agent = new AnalysisAgent(llmClient, toolExecutor);
        SecurityEvent event = new SecurityEvent(
                "e1", "1.2.3.4", "5.6.7.8", 123, 80, "TCP", "scan", Instant.now(), "{}");
        AgentResult result = agent.execute(new AgentTask("t1", AgentTask.TaskType.ANALYZE, event, Map.of()));
        assertFalse(result.success());
        assertFalse(result.messages().isEmpty());
    }
}
