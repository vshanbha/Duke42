package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * QnA-specific payload normalizer per https://code.claude.com/docs/en/agent-sdk/user-input#question-format.
 * The Claude spec and AskUserQuestionTool.md require {@code {"questions":[...]}} with each entry
 * {@code {question, header≤12, options[2-4]{label,description}, multiSelect}}. Some small models
 * (e.g. lfm2.5) occasionally emit a flat {@code {"question","header","options",...}} object.
 * This decorator repairs that shape before delegating – applied only to the QnA ToolCallback,
 * not via a generic AoP if-else.
 */
final class AskUserQuestionNormalizationCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    AskUserQuestionNormalizationCallback(ToolCallback delegate) {
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
        return delegate.call(normalizeArguments(arguments));
    }

    @Override
    public String call(String arguments, ToolContext context) {
        return delegate.call(normalizeArguments(arguments), context);
    }

    private String normalizeArguments(String arguments) {
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
            // Preserve original so Spring AI reports normal tool error.
        }
        return arguments;
    }
}
