package com.example.cliai.agent;

import com.example.cliai.agent.tools.CalculatorTool;
import com.example.cliai.agent.tools.UnitConverterTool;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultTools(
                AskUserQuestionTool.builder()
                    .questionHandler(new CommandLineQuestionHandler())
                    .build(),
                new CalculatorTool(),
                new UnitConverterTool()
            )
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}