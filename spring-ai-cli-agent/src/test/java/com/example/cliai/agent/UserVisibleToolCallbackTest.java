package com.example.cliai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserVisibleToolCallbackTest {

    @Test
    void passesArgumentsThroughUnchangedForAskUserQuestionTool() {
        AtomicReference<String> received = new AtomicReference<>();
        ToolCallback delegate = new StubToolCallback("AskUserQuestionTool", received);
        UserVisibleToolCallback callback = new UserVisibleToolCallback(delegate);

        String payload = """
            {"questions":[{"question":"What kind of project?","header":"Project type","options":[{"label":"A","description":"desc"}],"multiSelect":false}]}
            """;
        callback.call(payload);

        assertThat(received.get()).isEqualTo(payload);
    }

    @Test
    void passesArgumentsThroughUnchangedForCalculatorTool() {
        AtomicReference<String> received = new AtomicReference<>();
        ToolCallback delegate = new StubToolCallback("CalculatorTool", received);
        UserVisibleToolCallback callback = new UserVisibleToolCallback(delegate);

        callback.call("{\"expression\":\"2 + 2\"}");

        assertThat(received.get()).isEqualTo("{\"expression\":\"2 + 2\"}");
    }

    @Test
    void preservesToolDefinition() {
        AtomicReference<String> received = new AtomicReference<>();
        ToolCallback delegate = new StubToolCallback("AskUserQuestionTool", received);
        UserVisibleToolCallback callback = new UserVisibleToolCallback(delegate);

        assertThat(callback.getToolDefinition().name()).isEqualTo("AskUserQuestionTool");
        assertThat(callback.getToolDefinition().description()).isEqualTo("test");
        assertThat(callback.getToolDefinition().inputSchema()).isEqualTo("{}");
    }

    private static final class StubToolCallback implements ToolCallback {
        private final ToolDefinition definition;
        private final AtomicReference<String> received;

        private StubToolCallback(String name, AtomicReference<String> received) {
            this.definition = ToolDefinition.builder()
                .name(name)
                .description("test")
                .inputSchema("{}")
                .build();
            this.received = received;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return ToolMetadata.builder().build();
        }

        @Override
        public String call(String arguments) {
            received.set(arguments);
            return "ok";
        }
    }
}
