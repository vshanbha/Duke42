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

        // QnA tool is a first-class citizen per https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool
        // and https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/AskUserQuestionTool.md
        // Spec: https://code.claude.com/docs/en/agent-sdk/user-input#question-format (questions[]:{question,header,options{label,description},multiSelect})
        // Implemented separately with its own QuestionHandler – not bundled via AoP if-else.
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        ToolCallback qnaNormalized = new AskUserQuestionNormalizationCallback(
            ToolCallbacks.from(askUserQuestionTool)[0]);
        ToolCallback[] domainCallbacks = ToolCallbacks.from(new CalculatorTool(), new UnitConverterTool());

        ToolCallback[] visibleTools = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(qnaNormalized),
                java.util.Arrays.stream(domainCallbacks))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
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
