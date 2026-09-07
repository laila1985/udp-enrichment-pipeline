package com.servuce.analytics.application.agent;

import com.servuce.analytics.domain.agent.Agent;
import com.servuce.analytics.domain.model.AgentResult;
import com.servuce.analytics.domain.model.AgentTask;
import com.servuce.analytics.domain.port.LlmClient;

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

        String report = llmClient.complete(SYSTEM_PROMPT, userPrompt);
        return AgentResult.report(task.taskId(), report);
    }
}