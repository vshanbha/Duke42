# Spring AI CLI Agent

A terminal-based AI assistant that teaches Spring AI concepts step by step.

## What It Does

Chat with a local LLM (Ollama) from your terminal. The agent has:
- **Conversation memory** — remembers what you said earlier
- **Calculator tool** — evaluates math expressions
- **Unit converter** — converts km/miles, kg/lbs, etc.
- **Logging** — shows prompts and responses for debugging

## Quick Start

```bash
# Prerequisites: Java 17+, Maven, Ollama with gemma4:e4b (Linux/CI) or gemma4:e4b-mlx (Mac MLX, already downloaded)
ollama pull gemma4:e4b # or gemma4:e4b-mlx on Mac
# Native Ollama thinking: spring.ai.ollama.chat.think=medium in application.properties

# Run (uses gemma4:e4b; Mac MLX: --spring.profiles.active=local or -Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx)
mvn spring-boot:run

# Chat
You: What is (15 * 7) + 23?
AI: The answer is 128.

You: Convert 100 km to miles
AI: 100 km is 62.14 miles.

You: My name is Alice
AI: Nice to meet you, Alice!

You: What's my name?
AI: Your name is Alice.
```

## Project Structure

```
spring-ai-cli-agent/
├── src/main/java/com/example/cliai/
│   ├── Application.java              # Entry point
│   ├── agent/
│   │   ├── AgentConfiguration.java   # ChatClient + tools + memory
│   │   ├── UserVisibleToolCallback.java # pure trace embellishment
│   │   └── tools/
│   │       ├── CalculatorTool.java    # Math evaluator (SpEL)
│   │       └── UnitConverterTool.java # Unit conversions
│   └── cli/
│       ├── ChatLoop.java             # Terminal REPL
│       ├── SlashCommand.java         # command pattern interface
│       └── SlashCommandHandler.java  # registry for /help, /tools, /clear, /think, /exit
├── src/main/resources/
│   ├── application.properties         # Ollama config (checked-in: gemma4:e4b)
│   └── application-local.properties.example # local Mac MLX override (not committed)
└── src/test/java/                    # 46 tests (43 unit/integration + 3 evals skipped)
```

## Run Tests

From top level (`Duke42/`):

```bash
mvn test # all modules (spring-ai-cli-agent 46 + backend 10)
mvn test -pl spring-ai-cli-agent -am # only CLI agent
```

From `spring-ai-cli-agent/`:

```bash
mvn test # 46 tests, 3 evals skipped without Ollama
mvn test -Devals=true # opt-in model/tool-call evals, requires Ollama gemma4:e4b (or gemma4:e4b-mlx via -Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx or --spring.profiles.active=local)
```

All tests pass with general setup – no need to specify `-Dtest=...`. Evals exit `0` when model invokes expected tool (checked via `[Tool]` trace), non-zero when Ollama unavailable.

## Configuration

Edit `src/main/resources/application.properties` (checked-in: `gemma4:e4b`, Linux/CI-friendly):

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=gemma4:e4b  # Linux/CI: gemma4:e4b (9.6 GB); Mac MLX: gemma4:e4b-mlx (8.8 GB) via application-local.properties
# Full <10 GB tools-capable comparison: see ../ollama-model-links.md (single source of truth)
```

Local Mac override – copy `application-local.properties.example` to `application-local.properties` (git-ignored) and run with `--spring.profiles.active=local`:

```properties
spring.ai.ollama.chat.options.model=gemma4:e4b-mlx
```
Or one-off: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.ai.ollama.chat.options.model=gemma4:e4b-mlx`

## How It Works

1. `Application.java` starts Spring Boot
2. `AgentConfiguration.java` creates a `ChatClient` bean with tools and memory
3. `ChatLoop.java` reads user input, calls `chatClient.prompt()`, prints response
4. The AI decides when to use tools based on the tool descriptions

## Learn More

See [TUTORIAL.md](../TUTORIAL.md) for a step-by-step guide to building this from scratch.
