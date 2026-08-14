package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatLoopTest {

    @Test
    void shouldExitOnExitCommand() {
        ChatClient chatClient = mock(ChatClient.class);

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            String input = "exit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

            chatLoop.run();

            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldExitOnQuitCommand() {
        ChatClient chatClient = mock(ChatClient.class);

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            String input = "quit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

            chatLoop.run();

            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldStreamChatClientResponseOnUserInput() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("AI", " response"));

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            String input = "Hello\nexit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

            chatLoop.run();

            verify(chatClient).prompt();
            verify(requestSpec).user("Hello");
            verify(requestSpec).stream();
            verify(streamSpec).content();
        } finally {
            System.setIn(originalIn);
        }
    }
}
