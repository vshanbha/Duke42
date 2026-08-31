package com.example.cliai.cli;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.cliai.agent.SystemPrompts;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Interactive chat REPL built on Spring Shell (JLine). The {@link #chat()} command
 * owns the input loop; Spring Shell supplies the terminal, line editing, history
 * and ANSI colouring. Coloured output uses JLine's {@link AttributedString}, which
 * only emits escape codes on a real TTY (a dumb terminal gets plain text).
 */
@Component
class ChatLoop {

    private static final String SESSION_ID_PREFIX = "session-";
    private static final String IMAGE_PREFIX = "/image ";
    private static final String CONVERT_PREFIX = "/convert ";

    private static final int THINKING_COLOR = AttributedStyle.CYAN;
    private static final int AI_COLOR = AttributedStyle.GREEN;

    private final ChatClient chatClient;
    private final Terminal terminal;

    private final SlashCommandHandler slashHandler = new SlashCommandHandler();
    private final AtomicReference<String> sessionId = new AtomicReference<>(SESSION_ID_PREFIX + UUID.randomUUID());
    private final AtomicReference<String> role = new AtomicReference<>(null);
    private final AtomicReference<String> modelOverride = new AtomicReference<>(null);
    private final AtomicReference<Double> temperature = new AtomicReference<>(null);
    private final SlashCommand.Context slashContext = new SlashCommand.Context(sessionId, role, modelOverride, temperature);

    ChatLoop(ChatClient chatClient, Terminal terminal) {
        this.chatClient = chatClient;
        this.terminal = terminal;
    }

    /** Spring Shell command: enter the interactive chat loop. */
    @Command(value = "chat", help = "Start an interactive chat session with the agent")
    public void chat() {
        terminal.writer().println("Spring AI CLI Agent — type 'exit' to leave the chat, Ctrl-D to abort.\n");
        terminal.writer().flush();
        // Build the LineReader locally: the auto-configured LineReader bean participates
        // in a circular dependency with the command registry, so we avoid injecting it.
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        while (true) {
            String input;
            try {
                input = lineReader.readLine("You: ");
            } catch (UserInterruptException e) {
                continue; // Ctrl-C: ignore and re-prompt
            } catch (EndOfFileException e) {
                break;     // Ctrl-D: leave the chat
            }
            if (processLine(input)) {
                // 'exit'/'quit' was requested: return to the shell prompt. The shell's
                // built-in 'exit' command terminates the JVM cleanly (no web server to stop).
                return;
            }
        }
    }

    /**
     * Process a single line of input.
     * @return true if the loop should exit
     */
    boolean processLine(String input) {
        String trimmed = input == null ? "" : input;
        if (trimmed.isBlank()) {
            return false;
        }
        SlashCommand.Result result = slashHandler.handle(trimmed, slashContext);
        if (result == SlashCommand.Result.EXIT) {
            return true;
        }
        if (result == SlashCommand.Result.HANDLED) {
            return false;
        }
        if (trimmed.startsWith(IMAGE_PREFIX)) {
            handleImageQuery(trimmed, sessionId.get(), role, modelOverride, temperature);
            return false;
        }
        if (trimmed.startsWith(CONVERT_PREFIX)) {
            handleConvert(trimmed, sessionId.get(), modelOverride);
            return false;
        }
        try {
            ChatClient.ChatClientRequestSpec spec = buildSpec(trimmed, sessionId.get(), role, modelOverride, temperature);
            streamAndPrint(spec);
        } catch (Exception e) {
            terminal.writer().println("\n[Error] " + e.getMessage() + "\n");
            terminal.writer().flush();
        }
        return false;
    }

    private void printColored(String text, int color, boolean bold) {
        AttributedStyle style = AttributedStyle.DEFAULT.foreground(color);
        if (bold) {
            style = style.bold();
        }
        new AttributedString(text, style).print(terminal);
    }

    /**
     * BLUEPRINT Steps 2/9: apply per-call preferences.
     * Role override renders {@link SystemPrompts#SYSTEM_TEMPLATE} ({role} placeholder);
     * temperature/model overrides become per-request {@code OllamaChatOptions}.
     */
    private ChatClient.ChatClientRequestSpec buildSpec(Message message,
                                                       String conversationId,
                                                       AtomicReference<String> role,
                                                       AtomicReference<String> modelOverride,
                                                       AtomicReference<Double> temperature) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
            .messages(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
        return applyPreferences(spec, role, modelOverride, temperature);
    }

    /** Plain-text variant keeps the fluent {@code .user(String)} path (and its tests) intact. */
    private ChatClient.ChatClientRequestSpec buildSpec(String userText,
                                                       String conversationId,
                                                       AtomicReference<String> role,
                                                       AtomicReference<String> modelOverride,
                                                       AtomicReference<Double> temperature) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
            .user(userText)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
        return applyPreferences(spec, role, modelOverride, temperature);
    }

    private ChatClient.ChatClientRequestSpec applyPreferences(ChatClient.ChatClientRequestSpec spec,
                                                              AtomicReference<String> role,
                                                              AtomicReference<String> modelOverride,
                                                              AtomicReference<Double> temperature) {
        if (role.get() != null && !role.get().isBlank()) {
            spec = spec.system(SystemPrompts.render(role.get()));
        }
        OllamaChatOptions.Builder options = OllamaChatOptions.builder();
        boolean hasOverrides = false;
        if (temperature.get() != null) {
            options.temperature(temperature.get());
            hasOverrides = true;
        }
        if (modelOverride.get() != null && !modelOverride.get().isBlank()) {
            options.model(modelOverride.get());
            hasOverrides = true;
        }
        if (hasOverrides) {
            spec = spec.options(options);
        }
        return spec;
    }

    /** BLUEPRINT Step 12/13: streaming with thinking trace (cyan) and answer (green). */
    private void streamAndPrint(ChatClient.ChatClientRequestSpec spec) {
        new AttributedString("Thinking... ", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)).print(terminal);
        terminal.writer().flush();
        AtomicBoolean thinkingPrinted = new AtomicBoolean(false);
        AtomicBoolean thinkingOn = new AtomicBoolean(false);
        AtomicBoolean aiOn = new AtomicBoolean(false);
        spec.stream()
            .chatResponse()
            .doOnNext(cr -> {
                // Reasoning content via OllamaChatOptions thinking (see https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning)
                String thinking = null;
                try {
                    thinking = (String) cr.getResult().getMetadata().get("thinking");
                    if (thinking == null) thinking = (String) cr.getResult().getMetadata().get("reasoningContent");
                } catch (Exception ignored) {}
                if (thinking != null && !thinking.isBlank()) {
                    if (!thinkingOn.get()) {
                        terminal.writer().print("\n");
                        terminal.writer().flush();
                        printColored(thinkingPrinted.compareAndSet(false, true) ? "[Thinking] " : "", THINKING_COLOR, false);
                        thinkingOn.set(true);
                    }
                    printColored(thinking, THINKING_COLOR, false);
                }
                String content = null;
                try { content = cr.getResult().getOutput().getText(); } catch (Exception ignored) {}
                if (content != null && !content.isBlank()) {
                    if (!aiOn.get()) {
                        terminal.writer().print(thinkingPrinted.get() ? "\n\n" : "\n");
                        terminal.writer().flush();
                        printColored("AI: ", AI_COLOR, true);
                        aiOn.set(true);
                    }
                    printColored(content, AI_COLOR, true);
                }
            })
            .blockLast();
        terminal.writer().print("\n\n");
        terminal.writer().flush();
    }

    /** BLUEPRINT Step 8: vision via Media attachment on the user message. */
    private void handleImageQuery(String input,
                                  String conversationId,
                                  AtomicReference<String> role,
                                  AtomicReference<String> modelOverride,
                                  AtomicReference<Double> temperature) {
        ImageQuery query = parseImageArgs(input);
        if (query == null || mimeFor(query.path()) == null) {
            terminal.writer().println("Usage: /image <path-to-image> <question>   (png/jpg/jpeg/gif/webp)\n");
            return;
        }
        try {
            UserMessage message = UserMessage.builder()
                .text(query.question())
                .media(Media.builder()
                    .mimeType(mimeFor(query.path()))
                    .data(new FileSystemResource(query.path()))
                    .build())
                .build();
            terminal.writer().println("\n[Vision] inspecting " + query.path() + "...");
            streamAndPrint(buildSpec(message, conversationId, role, modelOverride, temperature));
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            terminal.writer().println("[Error] " + msg);
            // e.g. Ollama MLX builds reject vision: {"error":"this model does not support image input"}
            if (msg.contains("does not support image")) {
                terminal.writer().println("Hint: switch to a vision-capable model first, e.g. /model minicpm-v4.6\n");
            } else {
                terminal.writer().println();
            }
        }
    }

    /** BLUEPRINT Step 7: Structured Output via BeanOutputConverter JSON schema + outputSchema option. */
    private void handleConvert(String input, String conversationId, AtomicReference<String> modelOverride) {
        String[] args = parseConvertArgs(input);
        if (args == null) {
            terminal.writer().println("Usage: /convert <value> <from-unit> <to-unit>   e.g. /convert 100 km miles\n");
            return;
        }
        record UnitConversion(double value, String unit) {}
        BeanOutputConverter<UnitConversion> converter = new BeanOutputConverter<>(UnitConversion.class);
        try {
            OllamaChatOptions.Builder options = OllamaChatOptions.builder()
                .outputSchema(converter.getJsonSchema())
                .disableThinking()
                .temperature(0.1);
            if (modelOverride.get() != null && !modelOverride.get().isBlank()) {
                options.model(modelOverride.get());
            }
            String json = chatClient.prompt()
                .user("Convert " + args[0] + " " + args[1] + " to " + args[2]
                    + ". Respond only with JSON matching this schema:\n" + converter.getJsonSchema())
                .options(options)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

            UnitConversion conversion = converter.convert(json);
            terminal.writer().println(converter.getJsonSchema());
            terminal.writer().printf("Converted: %s %s = %.2f %s%n%n", args[0], args[1], conversion.value(), conversion.unit());
        } catch (Exception e) {
            terminal.writer().println("[Error] structured output failed: " + e.getMessage() + "\n");
        }
    }

    /** "/image <path> <question...>" → ImageQuery or null when malformed. */
    static ImageQuery parseImageArgs(String input) {
        if (input.length() <= IMAGE_PREFIX.length()) {
            return null;
        }
        String rest = input.substring(IMAGE_PREFIX.length()).trim();
        int spaceIdx = rest.indexOf(' ');
        if (spaceIdx <= 0) {
            return null;
        }
        String path = rest.substring(0, spaceIdx);
        String question = rest.substring(spaceIdx + 1).trim();
        if (path.isBlank() || question.isBlank()) {
            return null;
        }
        return new ImageQuery(path, question);
    }

    record ImageQuery(String path, String question) {}

    /** "/convert <value> <from> <to>" → [value, from, to] or null when malformed. */
    static String[] parseConvertArgs(String input) {
        String[] tokens = input.substring(CONVERT_PREFIX.length()).trim().split("\\s+");
        if (tokens.length != 3) {
            return null;
        }
        try {
            Double.parseDouble(tokens[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        return tokens;
    }

    static MimeType mimeFor(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MimeTypeUtils.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MimeTypeUtils.parseMimeType("image/webp");
        return null;
    }
}
