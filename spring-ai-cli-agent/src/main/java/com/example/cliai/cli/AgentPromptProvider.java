package com.example.cliai.cli;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * Replaces the generic {@code shell:>} prompt with {@code agent>} so the
 * terminal clearly belongs to the CLI agent, not a generic shell.
 */
@Component
class AgentPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("agent> ",
            AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
    }
}
