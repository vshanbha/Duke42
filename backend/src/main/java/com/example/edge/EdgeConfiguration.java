package com.example.edge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EdgeConfiguration {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory,
                          ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }

    @Bean
    ChatClient chatClientWithMemory(ChatModel chatModel, ChatMemory chatMemory,
                                     ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider) {
        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
