package com.example.cliai.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConfigurationTest {

    @Test
    void shouldCreateChatClientBean() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);

        ChatClient chatClient = config.chatClient(chatModel, mcpProvider);

        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateDistinctChatClientInstances() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);

        ChatClient client1 = config.chatClient(chatModel, mcpProvider);
        ChatClient client2 = config.chatClient(chatModel, mcpProvider);

        assertThat(client1).isNotSameAs(client2);
    }

    @Test
    void defaultSystemShouldBeToolOblivious() {
        ChatModel chatModel = mock(ChatModel.class);
        org.springframework.ai.chat.prompt.ChatOptions opts = org.springframework.ai.chat.prompt.ChatOptions.builder().build();
        when(chatModel.getDefaultOptions()).thenReturn(opts);
        when(chatModel.getOptions()).thenReturn(opts);
        ChatResponse dummy = new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(dummy);

        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);
        AgentConfiguration config = new AgentConfiguration();
        ChatClient chatClient = config.chatClient(chatModel, mcpProvider);

        chatClient.prompt().user("Hello").advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, "test-1")).call().content();

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        String promptText = prompt.getInstructions().toString() + " " + prompt.getContents();

        assertThat(promptText).contains("You are an interactive CLI assistant");
        assertThat(promptText).contains("Be helpful, concise");
        assertThat(promptText).contains("use an available tool to ask");
        assertThat(promptText).contains("never ask in ordinary assistant text");
        // System prompt must NOT mention tool-specific details — those belong in the tool description.
        assertThat(promptText).doesNotContain("AskUserQuestionTool");
        assertThat(promptText).doesNotContain("questions array");
        assertThat(promptText).doesNotContain("The tool input must be a JSON object");
        assertThat(promptText).doesNotContain("\"questions\"");
    }

    @Test
    void askUserQuestionToolDescriptionShouldContainUsagePolicyAndSchema() {
        // The policy previously lived in defaultSystem; now it must live in the tool description.
        ToolCallback delegate = ToolCallbacks.from(
            org.springaicommunity.agent.tools.AskUserQuestionTool.builder()
                .questionHandler(questions -> java.util.Map.of())
                .build()
        )[0];
        ToolCallback wrapped = new UserVisibleToolCallback(delegate);
        String description = wrapped.getToolDefinition().description();

        assertThat(description).contains("You MUST use this tool for every question");
        assertThat(description).contains("Never ask the user a question in ordinary assistant text");
        assertThat(description).contains("questions");
        assertThat(description).contains("header");
        assertThat(description).contains("multiSelect");
        assertThat(description).contains("label");
        assertThat(description).contains("{\"questions\"");
    }

    @Test
    void tutorialStep3MustDocumentToolDescriptionPolicy() throws Exception {
        Path tutorial = Path.of("").toAbsolutePath().resolve("TUTORIAL.md");
        if (!Files.exists(tutorial)) {
            tutorial = Path.of("../TUTORIAL.md").toAbsolutePath().normalize();
        }
        if (!Files.exists(tutorial)) {
            tutorial = Path.of("../../TUTORIAL.md").toAbsolutePath().normalize();
        }
        assertThat(Files.exists(tutorial)).as("TUTORIAL.md must exist").isTrue();

        String content = Files.readString(tutorial);
        assertThat(content).contains("Why registration alone is not enough");
        // Tutorial must explain that the policy belongs in the tool description, not the system prompt.
        assertThat(content).contains("tool-oblivious");
        assertThat(content).contains("UserVisibleToolCallback");
        assertThat(content).contains("MUST use this tool for every question");
        assertThat(content).contains("UserVisibleToolCallback – trace, description enrichment, and normalization");
        // System prompt example must be generic but directive and tool-oblivious.
        assertThat(content).contains("You are an interactive CLI assistant.");
        assertThat(content).contains("Be helpful, concise");
        assertThat(content).contains("use an available tool to ask");
        assertThat(content).contains("never ask in ordinary assistant text");
    }
}
