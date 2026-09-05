package com.example.cliai.agent;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import com.example.cliai.agent.tools.SandboxedGlobTool;
import com.example.cliai.agent.tools.SandboxedGrepTool;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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
    ChatClient chatClient(ChatModel chatModel,
                          ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider,
                          ObjectProvider<QuestionAnswerAdvisor> ragAdvisor) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        // QnA implemented separately per https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool
        // and AskUserQuestionTool.md – tool implementation passed as .questionHandler(new CommandLineQuestionHandler()) to builder
        // Spec: https://code.claude.com/docs/en/agent-sdk/user-input#question-format (questions[]:{question,header,options{label,description},multiSelect})
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        // Visibility embellishment for all tools (including QnA) – pure trace, no definition mutation or spec decoration
        // Sandbox is rooted at the JVM working directory — app must be launched from the project root
        java.nio.file.Path projectRoot = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        FileSystemTools fileSystemTools = FileSystemTools.builder()
            .allowedDirectory(projectRoot).build();
        GlobTool globTool = GlobTool.builder().workingDirectory(projectRoot).build();
        GrepTool grepTool = GrepTool.builder().workingDirectory(projectRoot).build();
        SandboxedGlobTool sandboxedGlob = new SandboxedGlobTool(globTool, projectRoot);
        SandboxedGrepTool sandboxedGrep = new SandboxedGrepTool(grepTool, projectRoot);
        ToolCallback[] allWithTrace = java.util.Arrays.stream(
                ToolCallbacks.from(askUserQuestionTool, fileSystemTools, sandboxedGlob, sandboxedGrep))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(SystemPrompts.render(null))
            .defaultToolCallbacks(allWithTrace);

        // BLUEPRINT Step 14: SimpleLoggerAdvisor stays for request/response trace; token/metric
        // observability is native – ChatModel observations are exported via Micrometer/actuator
        // (no separate ObservationAdvisor exists in Spring AI 2.0.0).
        builder.defaultAdvisors(
            new SimpleLoggerAdvisor(),
            MessageChatMemoryAdvisor.builder(chatMemory).build()
        );

        // BLUEPRINT Step 10: RAG advisor is optional like MCP – present only when rag.enabled=true
        ragAdvisor.ifAvailable(builder::defaultAdvisors);

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
