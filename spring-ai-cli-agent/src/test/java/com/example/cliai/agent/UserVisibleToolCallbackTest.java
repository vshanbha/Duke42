package com.example.cliai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserVisibleToolCallbackTest {

    @Test
    void wrapsSingleAskUserQuestionInQuestionsArray() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        ToolCallback delegate = new StubToolCallback("AskUserQuestionTool", received);
        UserVisibleToolCallback callback = new UserVisibleToolCallback(delegate);

        String result = callback.call("""
            {"question":"What kind of project?","header":"Project type","options":[],"multiSelect":false}
            """);

        assertThat(result).isEqualTo("ok");
        assertThat(new ObjectMapper().readTree(received.get()).path("questions").isArray()).isTrue();
        assertThat(received.get()).contains("What kind of project?");
    }

    @Test
    void leavesNonAskUserToolArgumentsUnchanged() {
        AtomicReference<String> received = new AtomicReference<>();
        ToolCallback delegate = new StubToolCallback("CalculatorTool", received);
        UserVisibleToolCallback callback = new UserVisibleToolCallback(delegate);

        callback.call("{\"expression\":\"2 + 2\"}");

        assertThat(received.get()).isEqualTo("{\"expression\":\"2 + 2\"}");
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
        public String call(String arguments) {
            received.set(arguments);
            return "ok";
        }
    }
}
