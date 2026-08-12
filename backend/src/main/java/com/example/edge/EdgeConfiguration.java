package com.example.edge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EdgeConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

        return ChatClient.builder(chatModel)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
