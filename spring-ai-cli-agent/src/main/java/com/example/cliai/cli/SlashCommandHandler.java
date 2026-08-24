package com.example.cliai.cli;

import java.util.List;
import java.util.UUID;

/**
 * Registry for all slash commands – single source of truth for name/description/dispatch.
 * Uses command pattern: each slash command is a {@link SlashCommand} object.
 */
final class SlashCommandHandler {

    private static final String SESSION_ID_PREFIX = "session-";

    private final List<SlashCommand> commands;

    SlashCommandHandler() {
        this.commands = List.of(
            new ExitCommand(),
            new HelpCommand(),
            new ToolsCommand(),
            new ClearCommand(),
            new ThinkCommand()
        );
    }

    /** Dispatch raw input to the matching command, if any. */
    SlashCommand.Result handle(String rawInput, SlashCommand.Context context) {
        String trimmed = rawInput == null ? "" : rawInput.trim();
        for (SlashCommand cmd : commands) {
            if (cmd.supports(trimmed)) {
                return cmd.execute(trimmed, context);
            }
        }
        return SlashCommand.Result.NOT_HANDLED;
    }

    /** All registered commands – for /help rendering */
    List<SlashCommand> allCommands() {
        return commands;
    }

    // --- Concrete commands ---

    static final class ExitCommand implements SlashCommand {
        @Override public String name() { return "/exit"; }
        @Override public String description() { return "Exit the CLI"; }
        @Override public boolean supports(String input) {
            String c = input.trim().toLowerCase();
            return "exit".equals(c) || "quit".equals(c) || "/exit".equals(c);
        }
        @Override public Result execute(String input, Context context) {
            System.out.println("Goodbye!");
            return Result.EXIT;
        }
    }

    static final class HelpCommand implements SlashCommand {
        @Override public String name() { return "/help"; }
        @Override public String description() { return "Show this help"; }
        @Override public boolean supports(String input) { return "/help".equalsIgnoreCase(input.trim()); }
        @Override public Result execute(String input, Context context) {
            System.out.println("\nCommands:");
            // Registry will be injected for full list – fallback to static help if called directly
            System.out.println("  /help   Show this help");
            System.out.println("  /tools  List available tools");
            System.out.println("  /clear  Start a fresh conversation");
            System.out.println("  /think  Show thinking config/help");
            System.out.println("  /exit   Exit the CLI");
            System.out.println("  exit    Exit the CLI\n");
            return Result.HANDLED;
        }
    }

    static final class ToolsCommand implements SlashCommand {
        @Override public String name() { return "/tools"; }
        @Override public String description() { return "List available tools"; }
        @Override public boolean supports(String input) { return "/tools".equalsIgnoreCase(input.trim()); }
        @Override public Result execute(String input, Context context) {
            System.out.println("\nAvailable tools:");
            System.out.println("  CalculatorTool      Evaluate mathematical expressions");
            System.out.println("  UnitConverterTool   Convert supported units");
            System.out.println("  AskUserQuestionTool Let the agent ask a clarifying question\n");
            return Result.HANDLED;
        }
    }

    static final class ClearCommand implements SlashCommand {
        @Override public String name() { return "/clear"; }
        @Override public String description() { return "Start a fresh conversation"; }
        @Override public boolean supports(String input) { return "/clear".equalsIgnoreCase(input.trim()); }
        @Override public Result execute(String input, Context context) {
            context.sessionId().set(SESSION_ID_PREFIX + UUID.randomUUID());
            System.out.println("Conversation cleared.\n");
            return Result.HANDLED;
        }
    }

    static final class ThinkCommand implements SlashCommand {
        @Override public String name() { return "/think"; }
        @Override public String description() { return "Show thinking config/help"; }
        @Override public boolean supports(String input) { return "/think".equalsIgnoreCase(input.trim()); }
        @Override public Result execute(String input, Context context) {
            System.out.println("\nThinking is model-controlled via spring.ai.ollama.chat.think (or spring.ai.ollama.chat.options.think) in application.properties.");
            System.out.println("Current model: gemma4:e4b-mlx supports thinking (tools+thinking). Enable via:");
            System.out.println("  spring.ai.ollama.chat.think=medium  # or true/false/low/medium/high (OllamaChatOptions.enableThinking())");
            System.out.println("  # or spring.ai.ollama.chat.options.think=medium");
            System.out.println("Docs: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning");
            System.out.println(" Docs reasoning_content via OpenAI compatibility: ...#_reasoning_content_via_openai_compatibility");
            System.out.println("Manual indicator: CLI shows 'Thinking...' while waiting; thinking content from metadata \"thinking\"/\"reasoningContent\" is shown as [Thinking] if available.\n");
            return Result.HANDLED;
        }
    }
}
