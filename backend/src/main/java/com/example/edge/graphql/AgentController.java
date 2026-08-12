package com.example.edge.graphql;

import com.example.edge.ChatClients;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class AgentController {

    private final ChatClients chatClients;

    AgentController(ChatClients chatClients) {
        this.chatClients = chatClients;
    }

    @QueryMapping
    String infer(@Argument String prompt) {
        return chatClients.infer(prompt);
    }

    @QueryMapping
    String chat(@Argument String chatId, @Argument String message) {
        return chatClients.chat(chatId, message);
    }

    @QueryMapping
    String toolChat(@Argument String chatId, @Argument String message) {
        return chatClients.toolChat(chatId, message);
    }

    @MutationMapping
    String inferMutation(@Argument String prompt) {
        return chatClients.infer(prompt);
    }

    @MutationMapping
    String chatMutation(@Argument String chatId, @Argument String message) {
        return chatClients.chat(chatId, message);
    }

    @MutationMapping
    String toolChatMutation(@Argument String chatId, @Argument String message) {
        return chatClients.toolChat(chatId, message);
    }
}
