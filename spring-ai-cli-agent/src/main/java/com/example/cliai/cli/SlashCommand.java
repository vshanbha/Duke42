package com.example.cliai.cli;

/**
 * Command pattern for slash commands – all slash handling lives here, not in {@link ChatLoop}.
 * Each command is a first-class object with name/description/matching logic.
 */
public interface SlashCommand {

    /** Primary name, e.g. "/help" */
    String name();

    /** Human-readable description for /help */
    String description();

    /** True if this command supports the raw input (trimmed, case-insensitive). */
    boolean supports(String input);

    /**
     * Execute the command.
     * @return result indicating whether the loop should continue, exit, or that input was not a command
     */
    Result execute(String input, Context context);

    /** Execution result */
    enum Result {
        /** Command handled, continue loop */
        HANDLED,
        /** Exit command – break loop */
        EXIT,
        /** Not a command – proceed to AI call */
        NOT_HANDLED
    }

    /**
     * Context passed to commands (session, I/O, etc.).
     * Preference overrides are optional per-call {@code ChatOptions} inputs
     * (BLUEPRINT Steps 2/9: temperature, model switching).
     */
    record Context(
            java.util.concurrent.atomic.AtomicReference<String> sessionId,
            java.util.concurrent.atomic.AtomicReference<String> role,
            java.util.concurrent.atomic.AtomicReference<String> modelOverride,
            java.util.concurrent.atomic.AtomicReference<Double> temperature) {

        public Context(java.util.concurrent.atomic.AtomicReference<String> sessionId) {
            this(sessionId,
                 new java.util.concurrent.atomic.AtomicReference<>(null),
                 new java.util.concurrent.atomic.AtomicReference<>(null),
                 new java.util.concurrent.atomic.AtomicReference<>(null));
        }
    }
}
