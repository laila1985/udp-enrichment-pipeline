package com.service.analytics.domain.agent;

import com.service.analytics.domain.model.AgentResult;
import com.service.analytics.domain.model.AgentTask;

/**
 * Core contract for all specialized agents in the system.
 * Agents depend only on ports (interfaces), never on infrastructure.
 */
public interface Agent {
    AgentResult execute(AgentTask task);
}