package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in model evaluation: run with -Devals=true and a reachable Ollama instance. */
@SpringBootTest
@EnabledIfSystemProperty(named = "evals", matches = "true")
class ToolCallingEvalTest {

    @MockitoBean
    ChatLoop chatLoop;

    @Autowired
    ChatClient chatClient;

    @Test
    void calculatorPromptMustExecuteCalculatorTool() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            String response = chatClient.prompt()
                .user("Use the calculator tool to evaluate exactly (15 * 7) + 23. Do not calculate it yourself.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "eval-" + UUID.randomUUID()))
                .call()
                .content();

            String trace = output.toString(StandardCharsets.UTF_8);
            assertThat(trace).contains("[Tool] calculate", "[Tool result] 128.0");
            assertThat(response).isNotBlank();
        }
        finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void clarificationPromptMustExecuteAskUserQuestionTool() {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output));

            String response = chatClient.prompt()
                .user("Before answering, use AskUserQuestionTool to ask whether I mean Java coffee or Java software. Do not ask in ordinary text.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "eval-" + UUID.randomUUID()))
                .call()
                .content();

            String trace = output.toString(StandardCharsets.UTF_8);
            assertThat(trace).contains("[Tool] AskUserQuestionTool", "[Tool result]");
            assertThat(response).isNotBlank();
        }
        finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void sufficientlyAmbiguousPromptShouldTriggerClarificationTool() {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output));

            String response = chatClient.prompt()
                .user("Tell me about Java. I have not specified which meaning or domain I mean, and you should clarify before answering.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "eval-" + UUID.randomUUID()))
                .call()
                .content();

            String trace = output.toString(StandardCharsets.UTF_8);
            assertThat(trace).contains("[Tool] AskUserQuestionTool", "[Tool result]");
            assertThat(response).isNotBlank();
        }
        finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
