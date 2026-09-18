package com.service.analytics.domain.port;

import com.service.analytics.domain.agent.Tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port for resolving and executing tools by name.
 * The domain depends on this interface, not on a concrete registry.
 */
public interface ToolExecutor {
    List<Tool> availableTools();

    Optional<Tool> findByName(String name);

    String execute(String toolName, Map<String, Object> arguments);
}