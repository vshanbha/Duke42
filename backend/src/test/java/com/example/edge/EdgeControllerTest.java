package com.example.edge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
class EdgeControllerTest {

    @MockitoBean
    private ChatClient chatClient;

    @Autowired
    private EdgeController edgeController;

    @Test
    void contextLoads() {
        assertThat(edgeController).isNotNull();
    }

    @Test
    void shouldHaveChatClientInjected() {
        assertThat(chatClient).isNotNull();
    }
}
