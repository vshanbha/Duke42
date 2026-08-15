package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Adds a concise, user-visible trace around a tool invocation. */
final class UserVisibleToolCallback implements ToolCallback {

    private final ToolCallback delegate;

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
        return invoke(arguments, () -> delegate.call(arguments));
    }

    @Override
    public String call(String arguments, ToolContext context) {
        return invoke(arguments, () -> delegate.call(arguments, context));
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
