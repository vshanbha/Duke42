package com.example.cliai.cli;

import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Auto-enters the agent chat loop on interactive startup so
 * {@code mvn spring-boot:run} lands directly at the {@code You:} prompt
 * instead of the bare shell prompt (which required typing {@code chat} first).
 *
 * <p>Skips auto-start when: the {@code chat.auto-start} property is
 * {@code false}, no console is attached (tests, pipes, CI), or shell command
 * arguments were passed (e.g. {@code java -jar app.jar help} — letting Spring
 * Shell handle them). Spring {@code --key=value} options (e.g.
 * {@code --rag.enabled=true}) do not block auto-start.
 *
 * <p>Runs before Spring Shell's own {@code ApplicationRunner} loop
 * ({@code springShellApplicationRunner} carries no {@code @Order}, i.e.
 * {@code LOWEST_PRECEDENCE} — verified against spring-shell-core-autoconfigure
 * 4.0.3), so after leaving the chat ({@code exit}) the user lands at the
 * {@code agent>} shell prompt where {@code chat} re-enters and {@code exit}
 * quits. If a future Shell version orders its runner earlier, this
 * {@code HIGHEST_PRECEDENCE} still wins; {@code ChatAutoStarterTest}
 * pins the annotation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "chat.auto-start", havingValue = "true", matchIfMissing = true)
class ChatAutoStarter implements ApplicationRunner {

    private final ChatLoop chatLoop;
    private final BooleanSupplier consoleAttached;

    @Autowired
    ChatAutoStarter(ChatLoop chatLoop) {
        this(chatLoop, () -> System.console() != null);
    }

    /** Test-only seam: injects console detection so tests need no TTY. */
    ChatAutoStarter(ChatLoop chatLoop, BooleanSupplier consoleAttached) {
        this.chatLoop = chatLoop;
        this.consoleAttached = consoleAttached;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.getNonOptionArgs().isEmpty()) {
            return;
        }
        if (!consoleAttached.getAsBoolean()) {
            return;
        }
        chatLoop.chat();
    }
}
