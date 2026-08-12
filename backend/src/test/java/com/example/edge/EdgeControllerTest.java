package com.example.edge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EdgeControllerTest {

    @MockitoBean
    ChatClient chatClient;

    @Autowired
    EdgeController edgeController;

    @Test
    void contextLoads() {
        assertThat(edgeController).isNotNull();
    }
}
