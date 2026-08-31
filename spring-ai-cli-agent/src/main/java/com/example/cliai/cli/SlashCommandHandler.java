package com.example.cliai.cli;

import java.util.List;
import java.util.UUID;

/**
 * Registry for all slash commands – single source of truth for name/description/dispatch.
 * Uses command pattern: each slash command is a {@link SlashCommand} object.
 *
 * <p>Output goes to {@code System.out} rather than the JLine {@code Terminal} writer because
 * {@link org.jline.terminal.impl.DumbTerminal} does not bridge {@code terminal.writer()} to
 * the provided {@code OutputStream} (verified: even with explicit {@code flush()}, output
 * goes nowhere). {@code System.out} is the only reliable cross-terminal output path for
 * short menu text; the chat streaming path ({@code ChatLoop.streamAndPrint}) uses
 * {@code AttributedString.print(terminal)} which does work on real TTYs.
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
            new ThinkCommand(),
            new RoleCommand(),
            new ModelCommand(),
            new TempCommand()
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
            System.out.println("  /help    Show this help");
            System.out.println("  /tools   List available tools");
            System.out.println("  /clear   Start a fresh conversation");
            System.out.println("  /think   Show thinking config/help");
            System.out.println("  /role    Show/set the assistant role (PromptTemplate {role})");
            System.out.println("  /model   Show/switch chat model via ChatOptions");
            System.out.println("  /temp    Show/set sampling temperature (ChatOptions)");
            System.out.println("  /image   <path> <question> - ask about an image (multimodality)");
            System.out.println("  /convert <value> <from> <to> - structured output conversion");
            System.out.println("  /exit    Exit the CLI");
            System.out.println("  exit     Exit the CLI\n");
            return Result.HANDLED;
        }
    }

    static final class ToolsCommand implements SlashCommand {
        @Override public String name() { return "/tools"; }
        @Override public String description() { return "List available tools"; }
        @Override public boolean supports(String input) { return "/tools".equalsIgnoreCase(input.trim()); }
        @Override public Result execute(String input, Context context) {
            System.out.println("\nAvailable tools:");
			System.out.println("  FileSystemTools    Read, write, and edit files (project-sandboxed)");
			System.out.println("  GlobTool           Find files by glob pattern");
			System.out.println("  GrepTool           Search file contents by regex");
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
            System.out.println("\nThinking is model-controlled via spring.ai.ollama.chat.think in application.properties.");
            System.out.println("Current model: gemma4:e4b-mlx supports thinking (tools+thinking). Enable via:");
            System.out.println("  spring.ai.ollama.chat.think=medium  # or true/false/low/medium/high (OllamaChatOptions.enableThinking())");
            System.out.println("Docs: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning");
            System.out.println(" Docs reasoning_content via OpenAI compatibility: ...#_reasoning_content_via_openai_compatibility");
            System.out.println("Manual indicator: CLI shows 'Thinking...' while waiting; thinking content from metadata \"thinking\"/\"reasoningContent\" is shown as [Thinking] if available.\n");
            return Result.HANDLED;
        }
    }

    /** BLUEPRINT Step 2: per-request role via PromptTemplate {role} placeholder. */
    static final class RoleCommand implements SlashCommand {
        @Override public String name() { return "/role"; }
        @Override public String description() { return "Show/set the assistant role"; }
        @Override public boolean supports(String input) {
            String c = input.trim().toLowerCase();
            return c.equals("/role") || c.startsWith("/role ");
        }
        @Override public Result execute(String input, Context context) {
            String arg = input.length() > "/role".length() ? input.substring("/role".length()).trim() : "";
            if (arg.isEmpty()) {
                String current = context.role().get();
                System.out.println("Role: " + (current == null ? com.example.cliai.agent.SystemPrompts.DEFAULT_ROLE + " (default)" : current));
                System.out.println("Set with: /role math tutor   (rendered into the {role} PromptTemplate placeholder)");
                return Result.HANDLED;
            }
            context.role().set(arg);
            System.out.println("Role set to: " + arg + "\n");
            return Result.HANDLED;
        }
    }

    /** BLUEPRINT Step 9: switch models per-call via ChatOptions. */
    static final class ModelCommand implements SlashCommand {
        @Override public String name() { return "/model"; }
        @Override public String description() { return "Show/switch chat model"; }
        @Override public boolean supports(String input) {
            String c = input.trim().toLowerCase();
            return c.equals("/model") || c.startsWith("/model ");
        }
        @Override public Result execute(String input, Context context) {
            String arg = input.length() > "/model".length() ? input.substring("/model".length()).trim() : "";
            if (arg.isEmpty()) {
                String override = context.modelOverride().get();
                System.out.println("Model: " + (override == null ? "(configured default)" : override));
                System.out.println("Switch with: /model lfm2.5   (per-call OllamaChatOptions model override)");
                return Result.HANDLED;
            }
            if ("reset".equalsIgnoreCase(arg)) {
                context.modelOverride().set(null);
                System.out.println("Model override cleared – using configured default.\n");
                return Result.HANDLED;
            }
            context.modelOverride().set(arg);
            System.out.println("Model override set to: " + arg + " (/model reset to clear)\n");
            return Result.HANDLED;
        }
    }

    /** BLUEPRINT Steps 2/9: sampling temperature via per-call ChatOptions. */
    static final class TempCommand implements SlashCommand {
        @Override public String name() { return "/temp"; }
        @Override public String description() { return "Show/set sampling temperature"; }
        @Override public boolean supports(String input) {
            String c = input.trim().toLowerCase();
            return c.equals("/temp") || c.startsWith("/temp ");
        }
        @Override public Result execute(String input, Context context) {
            String arg = input.length() > "/temp".length() ? input.substring("/temp".length()).trim() : "";
            if (arg.isEmpty()) {
                Double t = context.temperature().get();
                System.out.println("Temperature: " + (t == null ? "(configured default)" : t));
                System.out.println("Set with: /temp 0.7   (higher = more creative, lower = more deterministic)");
                return Result.HANDLED;
            }
            if ("reset".equalsIgnoreCase(arg)) {
                context.temperature().set(null);
                System.out.println("Temperature reset to configured default.\n");
                return Result.HANDLED;
            }
            try {
                double value = Double.parseDouble(arg);
                if (value < 0.0 || value > 2.0) {
                    System.out.println("Temperature must be between 0.0 and 2.0\n");
                    return Result.HANDLED;
                }
                context.temperature().set(value);
                System.out.println("Temperature set to: " + value + "\n");
            } catch (NumberFormatException e) {
                System.out.println("Invalid temperature: " + arg + "\n");
            }
            return Result.HANDLED;
        }
    }
}
