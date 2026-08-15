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
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                if (!scanner.hasNextLine()) {
                    System.out.println("\nGoodbye!");
                    break;
                }
                String input = scanner.nextLine();
                String command = input.trim().toLowerCase();
                if ("exit".equals(command) || "quit".equals(command) || "/exit".equals(command)) {
                    System.out.println("Goodbye!");
                    break;
                }
                if ("/help".equals(command)) {
                    printHelp();
                    continue;
                }
                if ("/tools".equals(command)) {
                    printTools();
                    continue;
                }
                if ("/clear".equals(command)) {
                    sessionId.set(SESSION_ID_PREFIX + UUID.randomUUID());
                    System.out.println("Conversation cleared.\n");
                    continue;
                }

                try {
                    System.out.print("\nAI: ");
                    chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.get()))
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .blockLast();
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
