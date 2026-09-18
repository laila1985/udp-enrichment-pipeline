package com.service.analytics.domain.port;

import com.service.analytics.domain.agent.Tool;

import java.util.List;

/**
 * Port for interacting with a large language model (Ollama in production).
 * Supports tool calling: the model may request tool executions, which the
 * caller resolves and feeds back as tool results.
 */
public interface LlmClient {
    /**
     * Send a prompt to the LLM and return its text response.
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * Send a prompt with available tools. Returns the model's response,
     * which may include tool-call requests.
     */
    LlmResponse completeWithTools(String systemPrompt, String userPrompt, List<Tool> tools);

    record LlmResponse(String content, List<ToolCall> toolCalls, String error) {
        public LlmResponse(String content, List<ToolCall> toolCalls) {
            this(content, toolCalls, null);
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }

        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        public static LlmResponse error(String message) {
            return new LlmResponse(null, List.of(), message);
        }
    }

    record ToolCall(String name, java.util.Map<String, Object> arguments) {
    }
}