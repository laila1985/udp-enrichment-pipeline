package com.service.analytics.application.agent;

import com.service.analytics.domain.agent.Agent;
import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;

import java.util.EnumMap;
import java.util.Map;

/**
 * Orchestrator agent: routes tasks to the appropriate specialized agent
 * based on the task type. Acts as the entry point for the agent pipeline.
 */
public class OrchestratorAgent implements Agent {

    private final Map<AgentTask.TaskType, Agent> agents = new EnumMap<>(AgentTask.TaskType.class);

    public OrchestratorAgent(
            IngestionAgent ingestionAgent,
            AnalysisAgent analysisAgent,
            ReportingAgent reportingAgent) {
        agents.put(AgentTask.TaskType.INGEST, ingestionAgent);
        agents.put(AgentTask.TaskType.ANALYZE, analysisAgent);
        agents.put(AgentTask.TaskType.REPORT, reportingAgent);
    }

    @Override
    public AgentResult execute(AgentTask task) {
        Agent agent = agents.get(task.type());
        if (agent == null) {
            return AgentResult.failure(task.taskId(), "No agent for task type: " + task.type());
        }
        return agent.execute(task);
    }
}