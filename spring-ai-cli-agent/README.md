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
# Prerequisites: Java 17+, Maven, Ollama with lfm2.5
ollama pull lfm2.5

# Run
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
│   │   └── tools/
│   │       ├── CalculatorTool.java    # Math evaluator (SpEL)
│   │       └── UnitConverterTool.java # Unit conversions
│   └── cli/
│       └── ChatLoop.java             # Terminal REPL
├── src/main/resources/
│   └── application.properties         # Ollama config
└── src/test/java/                    # 31 tests
```

## Run Tests

```bash
mvn test
# Unit, integration, and CLI behavior tests

# Opt-in model/tool-call evaluation; requires Ollama with lfm2.5 running
mvn test -Devals=true -Dtest=ToolCallingEvalTest

# Run only the AskUserQuestionTool eval
mvn test -Devals=true -Dtest=ToolCallingEvalTest#clarificationPromptMustExecuteAskUserQuestionTool

# Run the ambiguity eval without naming the tool in the prompt
mvn test -Devals=true -Dtest=ToolCallingEvalTest#sufficientlyAmbiguousPromptShouldTriggerClarificationTool
```

The evaluations exit with code `0` when the model invokes the expected tool and
returns a result. They exit nonzero when Ollama is unavailable or the tool call
does not occur. They check the tool trace, not the model's factual wording.

## Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=lfm2.5  # or qwen2.5, llama3.1
```

## How It Works

1. `Application.java` starts Spring Boot
2. `AgentConfiguration.java` creates a `ChatClient` bean with tools and memory
3. `ChatLoop.java` reads user input, calls `chatClient.prompt()`, prints response
4. The AI decides when to use tools based on the tool descriptions

## Learn More

See [TUTORIAL.md](../TUTORIAL.md) for a step-by-step guide to building this from scratch.
