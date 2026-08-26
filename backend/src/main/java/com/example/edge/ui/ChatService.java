package com.example.edge.ui;

import com.example.edge.ChatClients;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service("uiChatService")
public class ChatService {

    private final ChatClients chatClients;

    public ChatService(ChatClients chatClients) {
        this.chatClients = chatClients;
    }

    public String chat(String conversationId, String message) {
        return chatClients.chat(conversationId, message);
    }

    public Flux<String> chatStream(String conversationId, String message) {
        return chatClients.chatStream(conversationId, message);
    }
}
