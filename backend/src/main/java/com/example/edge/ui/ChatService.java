package com.example.edge.ui;

import com.example.edge.ChatClients;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
class ChatService {

    private final ChatClients chatClients;

    ChatService(ChatClients chatClients) {
        this.chatClients = chatClients;
    }

    String chat(String conversationId, String message) {
        return chatClients.chat(conversationId, message);
    }

    Flux<String> chatStream(String conversationId, String message) {
        return chatClients.chatStream(conversationId, message);
    }
}
