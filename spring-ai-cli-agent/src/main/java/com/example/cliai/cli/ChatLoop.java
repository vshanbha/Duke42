package com.example.cliai.cli;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class ChatLoop implements CommandLineRunner {

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

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input.trim()) || "quit".equalsIgnoreCase(input.trim())) {
                    System.out.println("Goodbye!");
                    break;
                }

                try {
                    String response = chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-1"))
                        .call()
                        .content();
                    System.out.println("\nAI: " + response + "\n");
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
    }
}