package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatClientIntegrationTest {

    @MockitoBean
    ChatLoop chatLoop;

    @Autowired
    ChatClient chatClient;

    @Test
    void shouldGetResponseFromOllama() {
        String conversationId = "test-" + UUID.randomUUID();

        String response = chatClient.prompt()
            .user("Say hello in exactly 3 words")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .call()
            .content();

        assertThat(response).isNotNull().isNotBlank();
        System.out.println("LLM response: " + response);
    }

    @Test
    void shouldRememberContextAcrossTurns() {
        String conversationId = "memory-test-" + UUID.randomUUID();

        String response1 = chatClient.prompt()
            .user("Remember this exact fact for the next turn: my name is TestUser123. Reply only with OK.")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .call()
            .content();

        assertThat(response1).isNotNull();
        System.out.println("Turn 1: " + response1);

        String response2 = chatClient.prompt()
            .user("Retrieve the name from the previous turn. Reply with only that exact name and no other words.")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .call()
            .content();

        assertThat(response2).isNotNull();
        System.out.println("Turn 2: " + response2);

        assertThat(response2.toLowerCase()).contains("testuser123");
    }
}
