package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    void shouldExitCleanlyWhenInputEnds() {
        ChatClient chatClient = mock(ChatClient.class);

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(new byte[0]));

            chatLoop.run();

            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldHandleSlashCommandsWithoutCallingModel() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("/help\n/tools\n/clear\n/exit\n"
                .getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output));

            chatLoop.run();

            String text = output.toString(StandardCharsets.UTF_8);
            org.assertj.core.api.Assertions.assertThat(text)
                .contains("/help", "CalculatorTool", "Conversation cleared.", "Goodbye!");
            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
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
        org.springframework.ai.chat.model.ChatResponse r1 = new org.springframework.ai.chat.model.ChatResponse(
            java.util.List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage("AI"))));
        org.springframework.ai.chat.model.ChatResponse r2 = new org.springframework.ai.chat.model.ChatResponse(
            java.util.List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage(" response"))));
        when(streamSpec.chatResponse()).thenReturn(Flux.just(r1, r2));

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            String input = "Hello\nexit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

            chatLoop.run();

            verify(chatClient).prompt();
            verify(requestSpec).user("Hello");
            verify(requestSpec).stream();
            verify(streamSpec).chatResponse();
        } finally {
            System.setIn(originalIn);
        }
    }
}
