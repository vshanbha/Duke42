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
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel, ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        Object askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        ToolCallback[] visibleTools = java.util.Arrays.stream(
                ToolCallbacks.from(askUserQuestionTool, new CalculatorTool(), new UnitConverterTool()))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                When clarification is needed, use AskUserQuestionTool instead of asking a
                question in ordinary assistant text.
                The tool input must be a JSON object with a questions array. Each question
                must contain question, header, options, and multiSelect. The questions field
                must always be an array, never a string. Each option must contain label and
                description. Example:
                {"questions":[{"question":"Which option do you prefer?","header":"Preference","options":[{"label":"Option A","description":"First choice"},{"label":"Option B","description":"Second choice"}],"multiSelect":false}]}
                """)
            .defaultToolCallbacks(visibleTools)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
