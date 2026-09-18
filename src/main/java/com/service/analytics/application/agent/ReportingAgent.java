package com.service.analytics.application.agent;

import com.service.analytics.domain.agent.Agent;
import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;
import com.service.analytics.domain.port.LlmClient;

import java.util.List;

/**
 * Reporting agent: generates a human-readable incident report from a
 * threat finding using the LLM.
 */
public class ReportingAgent implements Agent {

    private static final String SYSTEM_PROMPT = """
            You are a SOC report writer. Produce a concise, professional
            incident report from the given threat finding. Include severity,
            MITRE technique, and recommended actions.
            """;

    private final LlmClient llmClient;

    public ReportingAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentResult execute(AgentTask task) {
        if (task.event() == null) {
            return AgentResult.failure(task.taskId(), "No event to report on");
        }

        String userPrompt = "Generate a report for event: " + task.event().eventType() +
                " from " + task.event().sourceIp() + " to " + task.event().destinationIp();

        LlmClient.LlmResponse response =
                llmClient.completeWithTools(SYSTEM_PROMPT, userPrompt, List.of());
        if (response.hasError()) {
            return AgentResult.failure(task.taskId(), response.error());
        }
        return AgentResult.report(task.taskId(), response.content());
    }
}