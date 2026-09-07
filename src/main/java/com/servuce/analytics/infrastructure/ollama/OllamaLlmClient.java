package com.servuce.analytics.infrastructure.ollama;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servuce.analytics.domain.agent.Tool;
import com.servuce.analytics.domain.port.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter implementing the LlmClient port against Ollama's native /api/chat
 * endpoint, with tool-calling support.
 */
@Component
public class OllamaLlmClient implements LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaLlmClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        LlmResponse response = completeWithTools(systemPrompt, userPrompt, List.of());
        return response.content();
    }

    @Override
    public LlmResponse completeWithTools(String systemPrompt, String userPrompt, List<Tool> tools) {
        try {
            Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt, tools);
            String raw = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(raw);
        } catch (Exception e) {
            return new LlmResponse("LLM error: " + e.getMessage(), List.of());
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt, List<Tool> tools) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        if (!tools.isEmpty()) {
            body.put("tools", tools.stream().map(this::toToolDefinition).toList());
        }
        return body;
    }

    private Map<String, Object> toToolDefinition(Tool tool) {
        Map<String, Object> function = new java.util.HashMap<>();
        function.put("name", tool.name());
        function.put("description", tool.description());
        function.put("parameters", tool.parametersSchema());

        Map<String, Object> def = new java.util.HashMap<>();
        def.put("type", "function");
        def.put("function", function);
        return def;
    }

    private LlmResponse parseResponse(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode message = root.path("message");
        String content = message.path("content").asText("");

        List<ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.path("tool_calls");
        if (toolCallsNode.isArray()) {
            for (JsonNode call : toolCallsNode) {
                String name = call.path("function").path("name").asText();
                Map<String, Object> args = objectMapper.convertValue(
                        call.path("function").path("arguments"),
                        new TypeReference<Map<String, Object>>() {});
                toolCalls.add(new ToolCall(name, args));
            }
        }
        return new LlmResponse(content, toolCalls);
    }
}