package com.servuce.analytics.domain.agent;

import java.util.Map;

/**
 * A callable capability exposed to agents (e.g. threat-intel lookups, API integrations).
 * Implementations are registered in the infrastructure layer's ToolRegistry.
 */
public interface Tool {
    String name();

    String description();

    Map<String, Object> parametersSchema();

    String execute(Map<String, Object> arguments);
}