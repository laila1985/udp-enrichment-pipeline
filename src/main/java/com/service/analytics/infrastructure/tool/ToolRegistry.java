package com.service.analytics.infrastructure.tool;

import com.service.analytics.domain.agent.Tool;
import com.service.analytics.domain.port.ToolExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter implementing the ToolExecutor port. Holds all registered tools
 * and executes them by name.
 */
@Component
public class ToolRegistry implements ToolExecutor {

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> toolBeans) {
        this.tools = toolBeans.stream()
                .collect(java.util.stream.Collectors.toMap(Tool::name, t -> t));
    }

    @Override
    public List<Tool> availableTools() {
        return List.copyOf(tools.values());
    }

    @Override
    public Optional<Tool> findByName(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return "Unknown tool: " + toolName;
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return "Tool error: " + e.getMessage();
        }
    }
}