package com.service.analytics.infrastructure.tool;

import com.service.analytics.domain.agent.Tool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    private static Tool tool(String name) {
        return new Tool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public Map<String, Object> parametersSchema() {
                return Map.of();
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                return "ok-" + name;
            }
        };
    }

    @Test
    void executesKnownTool() {
        ToolRegistry registry = new ToolRegistry(List.of(tool("a"), tool("b")));
        assertEquals("ok-a", registry.execute("a", Map.of()));
    }

    @Test
    void returnsErrorMessageForUnknownTool() {
        ToolRegistry registry = new ToolRegistry(List.of(tool("a")));
        assertTrue(registry.execute("missing", Map.of()).startsWith("Unknown tool"));
    }

    @Test
    void exposesAvailableTools() {
        ToolRegistry registry = new ToolRegistry(List.of(tool("a"), tool("b")));
        assertEquals(2, registry.availableTools().size());
    }
}
