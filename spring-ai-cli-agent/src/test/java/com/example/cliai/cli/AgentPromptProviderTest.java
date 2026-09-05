package com.example.cliai.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptProviderTest {

    @Test
    void shouldShowAgentPrompt() {
        AgentPromptProvider provider = new AgentPromptProvider();
        assertThat(provider.getPrompt().toString()).contains("agent>");
    }
}
