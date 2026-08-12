package com.example.cliai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentConfigurationTest {

    @Test
    void shouldCreateChatClientBean() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);

        ChatClient chatClient = config.chatClient(chatModel, mcpProvider);

        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateDistinctChatClientInstances() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);

        ChatClient client1 = config.chatClient(chatModel, mcpProvider);
        ChatClient client2 = config.chatClient(chatModel, mcpProvider);

        assertThat(client1).isNotSameAs(client2);
    }
}
