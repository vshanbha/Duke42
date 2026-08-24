package com.example.cliai.cli;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class ChatLoop implements CommandLineRunner {

    private static final String SESSION_ID_PREFIX = "session-";

    private final ChatClient chatClient;

    ChatLoop(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║   Spring AI CLI Agent                ║");
        System.out.println("║   Type 'exit' to quit                ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        AtomicReference<String> sessionId = new AtomicReference<>(SESSION_ID_PREFIX + UUID.randomUUID());
        SlashCommandHandler slashHandler = new SlashCommandHandler();
        SlashCommand.Context slashContext = new SlashCommand.Context(sessionId);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                if (!scanner.hasNextLine()) {
                    System.out.println("\nGoodbye!");
                    break;
                }
                String input = scanner.nextLine();
                SlashCommand.Result slashResult = slashHandler.handle(input, slashContext);
                if (slashResult == SlashCommand.Result.EXIT) {
                    break;
                }
                if (slashResult == SlashCommand.Result.HANDLED) {
                    continue;
                }

                try {
                    System.out.print("\nThinking... ");
                    System.out.flush();
                    java.util.concurrent.atomic.AtomicBoolean firstContent = new java.util.concurrent.atomic.AtomicBoolean(true);
                    java.util.concurrent.atomic.AtomicBoolean thinkingPrinted = new java.util.concurrent.atomic.AtomicBoolean(false);
                    chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.get()))
                        .stream()
                        .chatResponse()
                        .doOnNext(cr -> {
                            // Reasoning content via OllamaChatOptions thinking (see https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning)
                            String thinking = null;
                            try {
                                thinking = (String) cr.getResult().getMetadata().get("thinking");
                                if (thinking == null) thinking = (String) cr.getResult().getMetadata().get("reasoningContent");
                            } catch (Exception ignored) {}
                            if (thinking != null && !thinking.isBlank()) {
                                if (thinkingPrinted.compareAndSet(false, true)) {
                                    System.out.print("\r");
                                }
                                System.out.println("[Thinking] " + thinking);
                                System.out.flush();
                            }
                            String content = null;
                            try { content = cr.getResult().getOutput().getText(); } catch (Exception ignored) {}
                            if (content != null && !content.isBlank()) {
                                if (firstContent.getAndSet(false)) {
                                    if (!thinkingPrinted.get()) System.out.print("\r");
                                    System.out.print("AI: ");
                                }
                                System.out.print(content);
                                System.out.flush();
                            }
                        })
                        .blockLast();
                    if (firstContent.get() && !thinkingPrinted.get()) {
                        System.out.print("\r");
                    }
                    System.out.println("\n");
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void printHelp() {
        System.out.println("\nCommands:");
        System.out.println("  /help   Show this help");
        System.out.println("  /tools  List available tools");
        System.out.println("  /clear  Start a fresh conversation");
        System.out.println("  /think  Show thinking config/help");
        System.out.println("  /exit   Exit the CLI");
        System.out.println("  exit    Exit the CLI\n");
    }

    private void printTools() {
        System.out.println("\nAvailable tools:");
        System.out.println("  CalculatorTool      Evaluate mathematical expressions");
        System.out.println("  UnitConverterTool   Convert supported units");
        System.out.println("  AskUserQuestionTool Let the agent ask a clarifying question\n");
    }
}
