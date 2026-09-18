package com.service.analytics.application.agent;

import com.service.analytics.domain.agent.Agent;
import com.service.analytics.domain.agent.Tool;
import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;
import com.service.analytics.domain.model.ThreatFinding;
import com.service.analytics.domain.port.LlmClient;
import com.service.analytics.domain.port.ToolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analysis agent: investigates a security event using the LLM and available
 * tools (threat-intel lookups). Implements the tool-calling loop:
 * prompt -> tool calls -> execute -> feed results back -> final answer.
 */
public class AnalysisAgent implements Agent {

    private static final String SYSTEM_PROMPT = """
            You are a Tier-1 security analyst. Investigate the given security event.
            Use available tools to enrich your analysis (IP reputation, GeoIP, etc.).
            Conclude with a severity (LOW, MEDIUM, HIGH, CRITICAL), a MITRE ATT&CK
            technique, and a concise summary.
            """;

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;

    public AnalysisAgent(LlmClient llmClient, ToolExecutor toolExecutor) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public AgentResult execute(AgentTask task) {
        if (task.event() == null) {
            return AgentResult.failure(task.taskId(), "No event to analyze");
        }

        List<String> steps = new ArrayList<>();
        String userPrompt = buildPrompt(task);

        LlmClient.LlmResponse response =
                llmClient.completeWithTools(SYSTEM_PROMPT, userPrompt, toolExecutor.availableTools());

        if (response.hasError()) {
            return AgentResult.failure(task.taskId(), response.error());
        }

        // Tool-calling loop
        int maxIterations = 5;
        while (response.hasToolCalls() && maxIterations-- > 0) {
            for (LlmClient.ToolCall call : response.toolCalls()) {
                String result = toolExecutor.execute(call.name(), call.arguments());
                steps.add(call.name() + " -> " + result);
            }
            // Re-prompt with tool results appended
            response = llmClient.completeWithTools(
                    SYSTEM_PROMPT,
                    userPrompt + "\n\nTool results:\n" + String.join("\n", steps),
                    toolExecutor.availableTools());
            if (response.hasError()) {
                return AgentResult.failure(task.taskId(), response.error());
            }
        }

        ThreatFinding finding = parseFinding(task.event().eventId(), response.content(), steps);
        return AgentResult.success(task.taskId(), finding);
    }

    private String buildPrompt(AgentTask task) {
        var e = task.event();
        return "Event: " + e.eventType() +
                " from " + e.sourceIp() + ":" + e.sourcePort() +
                " to " + e.destinationIp() + ":" + e.destinationPort() +
                " protocol " + e.protocol() +
                " at " + e.timestamp();
    }

    private ThreatFinding parseFinding(String eventId, String content, List<String> steps) {
        ThreatFinding.Severity severity = inferSeverity(content);
        String mitre = inferMitre(content);
        return new ThreatFinding(eventId, severity, mitre, content, steps);
    }

    private ThreatFinding.Severity inferSeverity(String content) {
        String c = content.toUpperCase();
        if (c.contains("CRITICAL")) return ThreatFinding.Severity.CRITICAL;
        if (c.contains("HIGH")) return ThreatFinding.Severity.HIGH;
        if (c.contains("MEDIUM")) return ThreatFinding.Severity.MEDIUM;
        return ThreatFinding.Severity.LOW;
    }

    private String inferMitre(String content) {
        // Simple heuristic; in production this would be parsed from structured LLM output.
        if (content.contains("T1110")) return "T1110 - Brute Force";
        if (content.contains("T1046")) return "T1046 - Network Service Scanning";
        return "Unknown";
    }
}