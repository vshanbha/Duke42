package com.example.edge.graphql;

import com.example.edge.ChatClients;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AgentControllerTest {

    @Autowired
    private AgentController controller;

    @MockitoBean
    private ChatClients chatClients;

    @Test
    void shouldCallInfer() {
        when(chatClients.infer(any(String.class))).thenReturn("Hello from AI");

        String result = controller.infer("Say hello");

        assertThat(result).isEqualTo("Hello from AI");
        verify(chatClients).infer("Say hello");
    }

    @Test
    void shouldCallChat() {
        when(chatClients.chat(any(String.class), any(String.class))).thenReturn("I remember you");

        String result = controller.chat("user-123", "Hello");

        assertThat(result).isEqualTo("I remember you");
        verify(chatClients).chat("user-123", "Hello");
    }

    @Test
    void shouldCallToolChat() {
        when(chatClients.toolChat(any(String.class), any(String.class))).thenReturn("Tool response");

        String result = controller.toolChat("user-456", "Analyze this");

        assertThat(result).isEqualTo("Tool response");
        verify(chatClients).toolChat("user-456", "Analyze this");
    }

    @Test
    void shouldCallInferMutation() {
        when(chatClients.infer(any(String.class))).thenReturn("Mutation response");

        String result = controller.inferMutation("Test prompt");

        assertThat(result).isEqualTo("Mutation response");
        verify(chatClients).infer("Test prompt");
    }

    @Test
    void shouldCallChatMutation() {
        when(chatClients.chat(any(String.class), any(String.class))).thenReturn("Chat mutation");

        String result = controller.chatMutation("user-789", "Test message");

        assertThat(result).isEqualTo("Chat mutation");
        verify(chatClients).chat("user-789", "Test message");
    }

    @Test
    void shouldCallToolChatMutation() {
        when(chatClients.toolChat(any(String.class), any(String.class))).thenReturn("Tool mutation");

        String result = controller.toolChatMutation("user-012", "Test tool");

        assertThat(result).isEqualTo("Tool mutation");
        verify(chatClients).toolChat("user-012", "Test tool");
    }
}
