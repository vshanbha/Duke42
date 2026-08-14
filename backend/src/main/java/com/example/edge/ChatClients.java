package com.example.edge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatClients {

    private final ChatClient inferClient;
    private final ChatClient chatClient;

    ChatClients(ChatClient chatClient, ChatClient chatClientWithMemory) {
        this.inferClient = chatClient;
        this.chatClient = chatClientWithMemory;
    }

    public String infer(String prompt) {
        return inferClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    public String chat(String chatId, String message) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }
        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
            .call()
            .content();
    }

    public String toolChat(String chatId, String message) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }
        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId + "-tools"))
            .call()
            .content();
    }

    public Flux<String> chatStream(String chatId, String message) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("User ID cannot be empty."));
        }
        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
            .stream()
            .content();
    }
}
