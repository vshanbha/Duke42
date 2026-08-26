package com.example.cliai.agent;

import java.util.Map;

import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * BLUEPRINT Step 2: system prompt as a PromptTemplate with a {role} placeholder.
 * Rendered once with {@link #DEFAULT_ROLE} for the ChatClient default, and
 * per-request from the CLI when the user switches roles via /role.
 */
public final class SystemPrompts {

    public static final String SYSTEM_TEMPLATE = """
            You are an interactive CLI assistant acting as {role}.
            Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
            """;

    public static final String DEFAULT_ROLE = "a helpful general-purpose helper";

    private SystemPrompts() {
    }

    /** Render the system prompt for a role – single source of truth for template + default. */
    public static String render(String role) {
        return new PromptTemplate(SYSTEM_TEMPLATE)
            .render(Map.of("role", role == null || role.isBlank() ? DEFAULT_ROLE : role));
    }
}
