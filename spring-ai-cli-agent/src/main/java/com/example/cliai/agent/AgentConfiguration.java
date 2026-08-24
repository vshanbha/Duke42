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

        // QnA implemented separately per https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool
        // and AskUserQuestionTool.md – tool implementation passed as .questionHandler(...) to builder (no ToolCallbacks wrapping hack)
        // Spec: https://code.claude.com/docs/en/agent-sdk/user-input#question-format (questions[]:{question,header,options{label,description},multiSelect})
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        // Domain tools embellished with pure trace – QnA stays separate, no AoP branching or normalization wrapper
        ToolCallback[] domainWithTrace = java.util.Arrays.stream(
                ToolCallbacks.from(new CalculatorTool(), new UnitConverterTool()))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
                """)
            .defaultTools(askUserQuestionTool)
            .defaultToolCallbacks(domainWithTrace)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
