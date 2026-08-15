package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Adds a concise, user-visible trace around a tool invocation. */
final class UserVisibleToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    UserVisibleToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String arguments) {
        return invoke(arguments, () -> delegate.call(normalizeArguments(arguments)));
    }

    @Override
    public String call(String arguments, ToolContext context) {
        return invoke(arguments, () -> delegate.call(normalizeArguments(arguments), context));
    }

    private String normalizeArguments(String arguments) {
        if (!getToolDefinition().name().toLowerCase().contains("askuserquestion")) {
            return arguments;
        }
        try {
            JsonNode root = objectMapper.readTree(arguments);
            if (root.isObject() && root.has("options") && !root.has("questions")) {
                ObjectNode question = (ObjectNode) root.deepCopy();
                if (!question.has("question")) {
                    String header = question.path("header").asText("Please choose an option");
                    question.put("question", header + ". Please choose an option.");
                }
                ObjectNode wrapped = objectMapper.createObjectNode();
                ArrayNode questions = wrapped.putArray("questions");
                questions.add(question);
                return objectMapper.writeValueAsString(wrapped);
            }
        }
        catch (Exception ignored) {
            // Preserve the original payload so Spring AI reports the normal tool error.
        }
        return arguments;
    }

    private String invoke(String arguments, java.util.function.Supplier<String> invocation) {
        System.out.println("\n[Tool] " + getToolDefinition().name());
        System.out.println("[Tool arguments] " + arguments);
        try {
            String result = invocation.get();
            System.out.println("[Tool result] " + result);
            return result;
        }
        catch (RuntimeException exception) {
            System.out.println("[Tool error] " + exception.getMessage());
            throw exception;
        }
    }
}
