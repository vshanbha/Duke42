package com.example.cliai.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BLUEPRINT Steps 2/9 slash commands: /role (PromptTemplate placeholder),
 * /model and /temp (per-call ChatOptions overrides).
 */
class SlashCommandHandlerTest {

    private SlashCommandHandler handler;
    private SlashCommand.Context context;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        handler = new SlashCommandHandler();
        context = new SlashCommand.Context(
            new AtomicReference<>("session-test"),
            new AtomicReference<>(null),
            new AtomicReference<>(null),
            new AtomicReference<>(null));
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String stdout() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void roleCommandShouldSetAndShowRole() {
        assertThat(handler.handle("/role math tutor", context)).isEqualTo(SlashCommand.Result.HANDLED);
        assertThat(context.role().get()).isEqualTo("math tutor");

        output.reset();
        handler.handle("/role", context);
        assertThat(stdout()).contains("Role: math tutor");
    }

    @Test
    void bareRoleCommandShouldShowDefaultRole() {
        handler.handle("/role", context);
        assertThat(stdout()).contains(com.example.cliai.agent.SystemPrompts.DEFAULT_ROLE);
    }

    @Test
    void modelCommandShouldSetShowAndReset() {
        handler.handle("/model lfm2.5", context);
        assertThat(context.modelOverride().get()).isEqualTo("lfm2.5");

        output.reset();
        handler.handle("/model", context);
        assertThat(stdout()).contains("Model: lfm2.5");

        handler.handle("/model reset", context);
        assertThat(context.modelOverride().get()).isNull();
    }

    @Test
    void tempCommandShouldSetValidateAndReset() {
        handler.handle("/temp 0.9", context);
        assertThat(context.temperature().get()).isEqualTo(0.9);

        output.reset();
        handler.handle("/temp", context);
        assertThat(stdout()).contains("Temperature: 0.9");

        handler.handle("/temp 5.0", context);
        assertThat(context.temperature().get()).isEqualTo(0.9);
        assertThat(stdout()).contains("between 0.0 and 2.0");

        handler.handle("/temp abc", context);
        assertThat(context.temperature().get()).isEqualTo(0.9);

        handler.handle("/temp reset", context);
        assertThat(context.temperature().get()).isNull();
    }

    @Test
    void helpShouldListNewCommands() {
        handler.handle("/help", context);
        String help = stdout();
        assertThat(help).contains("/role").contains("/model").contains("/temp")
            .contains("/image").contains("/convert");
    }
}
