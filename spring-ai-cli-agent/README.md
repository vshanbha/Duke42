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
# Prerequisites: Java 17+, Maven, Ollama with gemma4:e4b-mlx (default, Mac MLX, already downloaded) or gemma4:e4b (Linux/CI)
ollama pull gemma4:e4b-mlx # default; CI uses gemma4:e4b via -Dspring.ai.ollama.chat.model=gemma4:e4b
# Native Ollama thinking: spring.ai.ollama.chat.think=medium in application.properties

# Run (default model is gemma4:e4b-mlx; CI overrides to gemma4:e4b via -D)
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
│   │       ├── FileSystemTools.java    # Read/write/edit files (sandboxed)
│   │       ├── GlobTool.java           # Find files by glob pattern
│   │       └── GrepTool.java           # Search file contents by regex
│   └── cli/
│       ├── ChatLoop.java             # Terminal REPL
│       ├── SlashCommand.java         # command pattern interface
│       └── SlashCommandHandler.java  # registry for /help, /tools, /clear, /think, /exit
├── src/main/resources/
│   ├── application.properties         # Ollama config (checked-in default: gemma4:e4b-mlx)
│   └── application-local.properties   # local Mac MLX override (git-ignored; not committed, example only)
└── src/test/java/                    # 49 tests (44 run + 3 evals skipped + 2 Docker-gated opt-ins)
```

## Run Tests

From top level (`Duke42/`):

```bash
mvn test # all modules (spring-ai-cli-agent 64 + backend 14)
mvn test -pl spring-ai-cli-agent -am # only CLI agent
```

From `spring-ai-cli-agent/`:

```bash
mvn test # 64 tests, 3 evals skipped without Ollama + 2 Docker-gated opt-ins
mvn test -Devals=true # opt-in model/tool-call evals, requires Ollama gemma4:e4b-mlx (or gemma4:e4b via -Dspring.ai.ollama.chat.model=gemma4:e4b)
```

All tests pass with general setup – no need to specify `-Dtest=...`. Evals exit `0` when model invokes expected tool (checked via `[Tool]` trace), non-zero when Ollama unavailable.

## Configuration

Edit `src/main/resources/application.properties` (checked-in default: `gemma4:e4b-mlx`, Mac MLX):

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma4:e4b-mlx  # default Mac MLX (8.8 GB); CI pins gemma4:e4b (9.6 GB) via -Dspring.ai.ollama.chat.model=gemma4:e4b
# Full <10 GB tools-capable comparison: see ../ollama-model-links.md (single source of truth)
```

CI override – the GitHub workflow runs `mvn test -Dspring.ai.ollama.chat.model=gemma4:e4b` on Linux.
Or one-off: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.ai.ollama.chat.model=gemma4:e4b` # CI/Linux parity override

## How It Works

1. `Application.java` starts Spring Boot
2. `AgentConfiguration.java` creates a `ChatClient` bean with tools and memory
3. `ChatLoop.java` reads user input, calls `chatClient.prompt()`, prints response
4. The AI decides when to use tools based on the tool descriptions

## Learn More

See [TUTORIAL.md](../TUTORIAL.md) for a step-by-step guide to building this from scratch.
