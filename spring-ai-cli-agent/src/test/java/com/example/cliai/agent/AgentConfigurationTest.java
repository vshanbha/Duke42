package com.example.cliai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentConfigurationTest {

    @Test
    void shouldCreateChatClientBean() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);

        ChatClient chatClient = config.chatClient(chatModel);

        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateDistinctChatClientInstances() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);

        ChatClient client1 = config.chatClient(chatModel);
        ChatClient client2 = config.chatClient(chatModel);

        assertThat(client1).isNotSameAs(client2);
    }
}
