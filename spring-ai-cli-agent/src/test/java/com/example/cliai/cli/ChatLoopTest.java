package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatLoopTest {

    @Test
    void shouldExitOnExitCommand() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        Terminal terminal = TerminalBuilder.builder().dumb(true).streams(new java.io.ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()).build();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        ChatLoop chatLoop = new ChatLoop(chatClient, terminal);

        boolean exit = chatLoop.processLine("exit");

        org.assertj.core.api.Assertions.assertThat(exit).isTrue();
        verify(chatClient, never()).prompt();
    }

    @Test
    void shouldExitOnQuitCommand() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        Terminal terminal = TerminalBuilder.builder().dumb(true).streams(new java.io.ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()).build();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        ChatLoop chatLoop = new ChatLoop(chatClient, terminal);

        boolean exit = chatLoop.processLine("quit");

        org.assertj.core.api.Assertions.assertThat(exit).isTrue();
        verify(chatClient, never()).prompt();
    }

    @Test
    void shouldNotExitOnRegularInput() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        Terminal terminal = TerminalBuilder.builder().dumb(true).streams(new java.io.ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()).build();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        ChatLoop chatLoop = new ChatLoop(chatClient, terminal);

        boolean exit = chatLoop.processLine("Hello there");

        org.assertj.core.api.Assertions.assertThat(exit).isFalse();
    }

    @Test
    void shouldHandleSlashCommandsWithoutCallingModel() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        Terminal terminal = TerminalBuilder.builder().dumb(true).streams(new java.io.ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()).build();
        ChatLoop chatLoop = new ChatLoop(chatClient, terminal);

        // Slash commands render to System.out (consistent across terminals).
        PrintStream originalOut = System.out;
        ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(sysOut));
        try {
            boolean exit = chatLoop.processLine("/help");
            String text = sysOut.toString(StandardCharsets.UTF_8);

            org.assertj.core.api.Assertions.assertThat(exit).isFalse();
            org.assertj.core.api.Assertions.assertThat(text)
                .contains("/help", "/tools", "/exit");
            verify(chatClient, never()).prompt();
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void shouldStreamChatClientResponseOnUserInput() throws Exception {
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder().dumb(true).streams(new java.io.ByteArrayInputStream(new byte[0]), out).build();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        ChatLoop chatLoop = new ChatLoop(chatClient, terminal);

        boolean exit = chatLoop.processLine("Hello");

        org.assertj.core.api.Assertions.assertThat(exit).isFalse();
        verify(chatClient).prompt();
        verify(requestSpec).user("Hello");
        verify(requestSpec).stream();
        verify(streamSpec).chatResponse();
        org.assertj.core.api.Assertions.assertThat(out.toString(StandardCharsets.UTF_8)).contains("AI: ");
    }
}
