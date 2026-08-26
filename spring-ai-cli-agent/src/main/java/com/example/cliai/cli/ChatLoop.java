package com.example.cliai.cli;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.example.cliai.agent.SystemPrompts;
import com.example.cliai.agent.tools.UnitConversion;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@Component
class ChatLoop implements CommandLineRunner {

    private static final String SESSION_ID_PREFIX = "session-";
    private static final String IMAGE_PREFIX = "/image ";
    private static final String CONVERT_PREFIX = "/convert ";

    private final ChatClient chatClient;

    ChatLoop(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║   Spring AI CLI Agent                ║");
        System.out.println("║   Type 'exit' to quit                ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        AtomicReference<String> sessionId = new AtomicReference<>(SESSION_ID_PREFIX + UUID.randomUUID());
        AtomicReference<String> role = new AtomicReference<>(null);
        AtomicReference<String> modelOverride = new AtomicReference<>(null);
        AtomicReference<Double> temperature = new AtomicReference<>(null);
        SlashCommandHandler slashHandler = new SlashCommandHandler();
        SlashCommand.Context slashContext = new SlashCommand.Context(sessionId, role, modelOverride, temperature);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                if (!scanner.hasNextLine()) {
                    System.out.println("\nGoodbye!");
                    break;
                }
                String input = scanner.nextLine();
                SlashCommand.Result slashResult = slashHandler.handle(input, slashContext);
                if (slashResult == SlashCommand.Result.EXIT) {
                    break;
                }
                if (slashResult == SlashCommand.Result.HANDLED) {
                    continue;
                }

                // BLUEPRINT Step 8: Multimodality – /image /tmp/pic.jpg What do you see?
                if (input.startsWith(IMAGE_PREFIX)) {
                    handleImageQuery(input, sessionId.get(), role, modelOverride, temperature);
                    continue;
                }
                // BLUEPRINT Step 7: Structured Output – /convert 100 km miles
                if (input.startsWith(CONVERT_PREFIX)) {
                    handleConvert(input, sessionId.get(), modelOverride);
                    continue;
                }

                try {
                    ChatClient.ChatClientRequestSpec spec = buildSpec(input,
                        sessionId.get(), role, modelOverride, temperature);
                    streamAndPrint(spec);
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
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

    /** BLUEPRINT Step 12/13: streaming with thinking indicator and [Thinking] trace. */
    private void streamAndPrint(ChatClient.ChatClientRequestSpec spec) {
        System.out.print("\nThinking... ");
        System.out.flush();
        AtomicBoolean firstContent = new AtomicBoolean(true);
        AtomicBoolean thinkingPrinted = new AtomicBoolean(false);
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
                    if (thinkingPrinted.compareAndSet(false, true)) {
                        System.out.print("\r");
                    }
                    System.out.println("[Thinking] " + thinking);
                    System.out.flush();
                }
                String content = null;
                try { content = cr.getResult().getOutput().getText(); } catch (Exception ignored) {}
                if (content != null && !content.isBlank()) {
                    if (firstContent.getAndSet(false)) {
                        if (!thinkingPrinted.get()) System.out.print("\r");
                        System.out.print("AI: ");
                    }
                    System.out.print(content);
                    System.out.flush();
                }
            })
            .blockLast();
        if (firstContent.get() && !thinkingPrinted.get()) {
            System.out.print("\r");
        }
        System.out.println("\n");
    }

    /** BLUEPRINT Step 8: vision via Media attachment on the user message. */
    private void handleImageQuery(String input,
                                  String conversationId,
                                  AtomicReference<String> role,
                                  AtomicReference<String> modelOverride,
                                  AtomicReference<Double> temperature) {
        ImageQuery query = parseImageArgs(input);
        if (query == null || mimeFor(query.path()) == null) {
            System.out.println("Usage: /image <path-to-image> <question>   (png/jpg/jpeg/gif/webp)\n");
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
            System.out.println("\n[Vision] inspecting " + query.path() + "...");
            streamAndPrint(buildSpec(message, conversationId, role, modelOverride, temperature));
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            System.out.println("[Error] " + msg);
            // e.g. Ollama MLX builds reject vision: {"error":"this model does not support image input"}
            if (msg.contains("does not support image")) {
                System.out.println("Hint: switch to a vision-capable model first, e.g. /model minicpm-v4.6\n");
            } else {
                System.out.println();
            }
        }
    }

    /** BLUEPRINT Step 7: Structured Output via BeanOutputConverter JSON schema + outputSchema option. */
    private void handleConvert(String input, String conversationId, AtomicReference<String> modelOverride) {
        String[] args = parseConvertArgs(input);
        if (args == null) {
            System.out.println("Usage: /convert <value> <from-unit> <to-unit>   e.g. /convert 100 km miles\n");
            return;
        }
        BeanOutputConverter<UnitConversion> converter = new BeanOutputConverter<>(UnitConversion.class);
        try {
            OllamaChatOptions.Builder options = OllamaChatOptions.builder()
                // JSON Schema derived from the UnitConversion record; deterministic answer → no thinking
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
            System.out.println(converter.getJsonSchema());
            System.out.printf("Converted: %s %s = %.2f %s%n%n", args[0], args[1], conversion.value(), conversion.unit());
        } catch (Exception e) {
            System.out.println("[Error] structured output failed: " + e.getMessage() + "\n");
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
