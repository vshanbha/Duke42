# Lesson: Spring Shell startup ordering + sandbox/test gotchas (sessions 7–9)

## Date
2026-09-05

## Context
Sessions 7–8 migrated the CLI to Spring Shell 4.0.3 and replaced
CalculatorTool/UnitConverterTool with sandboxed FileSystemTools/GlobTool/GrepTool
(worklog/2026-08-31-session-7.md, worklog/2026-08-31-session-8.md). Session 9
added auto-enter-chat on startup (ChatAutoStarter) and synced TUTORIAL.md.
The pending-lessons reminder (2026-08-31, session start HEAD df1d9e2) listed
22 changed files with no lesson file — this file closes it.

## Non-obvious facts

1. **Shell's own loop has no `@Order` (LOWEST_PRECEDENCE).** `ShellRunnerAutoConfiguration.springShellApplicationRunner()`
   carries only `@Bean` + `@ConditionalOnMissingBean` — verified via
   `javap -verbose` on spring-shell-core-autoconfigure-4.0.3.jar (observed
   2026-09-05 via jar inspection). So an `ApplicationRunner` with
   `@Order(HIGHEST_PRECEDENCE)` runs before the interactive loop — the basis
   for auto-enter-chat. Pinned by `ChatAutoStarterTest.shouldRunBeforeShellLoop`.
2. **`getNonOptionArgs()` vs option args.** `DefaultApplicationArguments("help")`
   puts `help` in non-option args; `--rag.enabled=true` lands in option names.
   Guarding auto-start on `getNonOptionArgs().isEmpty()` lets Spring-option
   invocations (`--rag.enabled=true`) enter chat while `java -jar app.jar help`
   still reaches Shell (observed 2026-09-05 via unit tests).
3. **`System.console()` is the TTY detector.** Null under surefire/pipes/CI, so
   the runner never blocks tests — doubled by
   `spring.shell.interactive.enabled=false` in test resources. Injected as a
   `BooleanSupplier` seam so tests need no PowerMock.
4. **`toRealPath()` for symlink-safe sandboxing.**
   `toAbsolutePath().normalize()` does not resolve symlinks: a symlink inside
   `allowedDirectory` pointing outside passes `startsWith`. Use `toRealPath()`
   when the path exists, fall back to `normalize()` (observed 2026-08-31 via
   GlobToolTest/GrepToolTest symlink-escape tests;
   SandboxedGlobTool.java:40, docs/DECISION_LOG.md Decision 9).
5. **Test-config shadowing.** `src/test/resources/application.properties` shadows
   main config; duplicate main keys there and guard with `ConfigPropertiesTest`
   (observed 2026-08-31 via Shell migration session).
6. **Commit lint checks every commit** in `main..HEAD`, not just the tip —
   squash to one commit when older commits violate it (observed 2026-08-31).
7. **Same-branch fixups only.** Addressing reviewer findings by opening a new PR
   violates AGENTS.md; add fixup commits + force-push instead (observed
   2026-08-31 via PR #10 → #11, and 2026-09-05 via PR #13 folded into #12).

## Applies to
CLI agent Shell/TUI work and any future sandbox or startup-order changes.
