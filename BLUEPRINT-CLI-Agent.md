# Blueprint: Spring AI — Learning Path & Project

**Goal**: Build a general-purpose AI assistant that teaches Spring AI concepts step by step
**End products**: A CLI agent (terminal) + a Vaadin web UI (browser) + a REST API (for JS devs)
**Stack**: Spring Boot 4 + Spring AI 2.0 + Ollama (local, free, no API keys) + Vaadin 25

---

## PREREQUISITES

### What you need installed

| Tool | Version | Purpose |
|---|---|---|
| Java | 17+ | Runtime |
| Maven | 3.6+ | Build |
| Ollama | Latest | Local LLM runtime |

### Ollama setup

```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull a model that supports tool calling
ollama pull lfm2.5

# Verify it works
ollama run lfm2.5 "Say hello"
```

### Why Ollama?

- **Free**: No API keys, no usage limits, no billing
- **Private**: Everything runs on your machine
- **Fast**: No network latency for local models
- **Tool calling**: `lfm2.5`, `qwen2.5`, `llama3.1`, and `mistral` support function calling

---

## PROJECT STRUCTURE

```
Duke42/
├── spring-ai-cli-agent/           # Learning project (CLI)
│   ├── pom.xml
│   ├── src/main/java/com/example/cliai/
│   │   ├── Application.java
│   │   ├── agent/
│   │   │   ├── AgentConfiguration.java
│   │   │   └── tools/
│   │   │       ├── CalculatorTool.java
│   │   │       └── UnitConverterTool.java
│   │   └── cli/
│   │       └── ChatLoop.java
│   └── src/test/java/             # 31 tests
│
├── backend/                       # Enterprise demo (Vaadin + REST + MCP)
│   ├── pom.xml
│   ├── src/main/java/com/example/edge/
│   │   ├── Application.java
│   │   ├── EdgeConfiguration.java
│   │   ├── EdgeController.java
│   │   └── ui/
│   │       ├── ChatView.java     # Vaadin web UI
│   │       └── ChatService.java  # Wraps ChatClient
│   └── src/main/resources/
│       └── application.yaml
│
├── polyglot/                      # MCP server (Quarkus, GraalPy)
│   └── src/main/java/com/example/
│       └── SentimentScoringResource.java
│
└── BLUEPRINT-CLI-Agent.md         # This file
```

### Architecture

```
┌──────────────────────────────────────────────────┐
│         Backend (Spring Boot, port 8080)          │
│                                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │  Vaadin UI  │  │  REST API   │  │  MCP     │ │
│  │  /chat      │  │  /edge/*    │  │  Client  │ │
│  │  (browser)  │  │  (for JS    │  │  (polyglot│ │
│  │             │  │   devs)     │  │   MCP)   │ │
│  └──────┬──────┘  └──────┬──────┘  └────┬─────┘ │
│         │                │              │        │
│         └────────────────┼──────────────┘        │
│                          │                       │
│                   ┌──────┴──────┐                │
│                   │ Spring AI   │                │
│                   │ ChatClient  │                │
│                   │ + Memory    │                │
│                   │ + Tools     │                │
│                   └──────┬──────┘                │
│                          │                       │
│                   ┌──────┴──────┐                │
│                   │   Ollama    │                │
│                   └─────────────┘                │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│      CLI Agent (Spring Boot, port 8081)           │
│      Terminal REPL, separate module               │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│      Polyglot MCP Server (Quarkus, port 9000)     │
│      Python sentiment analysis via GraalPy        │
└──────────────────────────────────────────────────┘
```

---

## LEARNING PATH (7 STEPS)

Each step adds one concept. At every step, the project compiles and runs.

---

### STEP 1: Basic ChatClient

**Concept**: ChatClient is to LLMs what JdbcTemplate is to databases — a fluent API that
sends prompts and gets responses.

**What you'll build**: A CLI loop that sends user input to the LLM and prints the response.

**Key files**:

`pom.xml` — dependencies:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.0</version>
</parent>

<properties>
    <java.version>17</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>
</dependencies>
```

`application.yaml`:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: lfm2.5
```

`Application.java`:

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`AgentConfiguration.java`:

```java
@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
```

`ChatLoop.java`:

```java
@Component
class ChatLoop implements CommandLineRunner {

    private final ChatClient chatClient;

    ChatLoop(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        System.out.println("AI Agent (type 'exit' to quit)\n");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input.trim())) break;

                String response = chatClient.prompt()
                    .user(input)
                    .call()
                    .content();
                System.out.println("AI: " + response + "\n");
            }
        }
    }
}
```

**Verify**:
```bash
mvn spring-boot:run
# Type: What is 2+2?
# You should get a response from the LLM
```

---

### STEP 2: Chat Memory

**Concept**: Advisors are like AOP aspects — they wrap AI calls with cross-cutting behavior.
`MessageChatMemoryAdvisor` adds conversation history so the AI remembers previous turns.

**What you'll add**: Memory advisor to the ChatClient, conversation ID to track sessions.

**Changes to `AgentConfiguration.java`**:

```java
@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
```

**Changes to `ChatLoop.java`** — add conversation ID:

```java
String response = chatClient.prompt()
    .user(input)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-1"))
    .call()
    .content();
```

**Verify**:
```bash
mvn spring-boot:run
# Type: My name is Alice
# Type: What's my name?
# AI should remember "Alice"
```

---

### STEP 3: AskUserQuestionTool

**Concept**: Tools are like stored procedures — plugins the AI can invoke when it needs
external data or user input. The AI *decides* when to call them. You don't invoke tools.

**What you'll add**: AskUserQuestionTool so the AI can ask clarifying questions.

**New dependency** in `pom.xml`:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.10.0</version>
</dependency>
```

**Changes to `AgentConfiguration.java`**:

```java
@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultTools(
                AskUserQuestionTool.builder()
                    .questionHandler(new CommandLineQuestionHandler())
                    .build()
            )
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
```

**New imports**:

```java
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
```

**Verify**:
```bash
mvn spring-boot:run
# Type: Help me learn Spring AI
# The AI should ask about your experience level, interests, etc.
# Answer the questions — the AI will tailor its response
```

---

### STEP 4: Custom Tool

**Concept**: You can build your own tools by implementing a method annotated with `@Tool`.
The AI decides when to call based on the tool's description.

**What you'll build**: A simple calculator tool.

**New file `CalculatorTool.java`**:

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

class CalculatorTool {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Tool(description = "Evaluate a mathematical expression. Supports +, -, *, /, parentheses. Example: (2 + 3) * 4")
    double calculate(
            @ToolParam(description = "The math expression to evaluate") String expression) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            var result = parser.parseExpression(expression).getValue(context);
            return result instanceof Number n ? n.doubleValue() : Double.parseDouble(result.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot evaluate: " + expression, e);
        }
    }
}
```

**Changes to `AgentConfiguration.java`** — register the tool:

```java
.defaultTools(
    AskUserQuestionTool.builder()
        .questionHandler(new CommandLineQuestionHandler())
        .build(),
    new CalculatorTool()
)
```

**Verify**:
```bash
mvn spring-boot:run
# Type: What is (15 * 7) + 23?
# AI should call the calculator tool and give you the answer
```

---

### STEP 5: Multiple Tools + Tool Calling Flow

**Concept**: When multiple tools are registered, the AI picks the right one based on the
user's request. It can even call multiple tools in sequence.

**What you'll add**: A second tool (unit converter) and observe the AI choosing between them.

**New file `UnitConverterTool.java`**:

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

class UnitConverterTool {

    @Tool(description = "Convert between units. Supports: km/miles, kg/lbs, celsius/fahrenheit, liters/gallons")
    String convert(
            @ToolParam(description = "The value to convert") double value,
            @ToolParam(description = "Source unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String from,
            @ToolParam(description = "Target unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String to) {

        // Conversion logic
        return switch (from.toLowerCase() + "->" + to.toLowerCase()) {
            case "km->miles" -> value * 0.621371 + " miles";
            case "miles->km" -> value * 1.60934 + " km";
            case "kg->lbs" -> value * 2.20462 + " lbs";
            case "lbs->kg" -> value / 2.20462 + " kg";
            case "celsius->fahrenheit" -> (value * 9/5 + 32) + " °F";
            case "fahrenheit->celsius" -> (value - 32) * 5/9 + " °C";
            case "liters->gallons" -> value * 0.264172 + " gallons";
            case "gallons->liters" -> value * 3.78541 + " liters";
            default -> "Unsupported conversion: " + from + " to " + to;
        };
    }
}
```

**Register in `AgentConfiguration.java`**:

```java
.defaultTools(
    AskUserQuestionTool.builder()
        .questionHandler(new CommandLineQuestionHandler())
        .build(),
    new CalculatorTool(),
    new UnitConverterTool()
)
```

**Verify**:
```bash
mvn spring-boot:run
# Type: Convert 100 km to miles
# AI calls UnitConverterTool
# Type: What is 15 * 7?
# AI calls CalculatorTool
# Type: If I drive 100 km at 60 mph, how long does it take in minutes?
# AI calls both tools in sequence
```

---

### STEP 6: Advisors — Logging

**Concept**: Advisors wrap around every AI call. A logging advisor shows you what's being
sent to the model and what comes back — invaluable for debugging.

**What you'll add**: A simple logging advisor.

**Changes to `AgentConfiguration.java`**:

```java
.defaultAdvisors(
    new SimpleLoggerAdvisor(),  // logs prompts and responses
    MessageChatMemoryAdvisor.builder(chatMemory).build()
)
```

**New import**:

```java
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
```

**Verify**:
```bash
mvn spring-boot:run
# Type anything
# You'll see the full prompt and response logged to console
```

**Note**: `SimpleLoggerAdvisor` is built into Spring AI. For production, you'd use a
proper logging framework. This step teaches you how advisors compose.

---

### STEP 7: Packaging as Executable Jar

**Concept**: Spring Boot's Maven plugin packages your app as a self-contained executable jar.
No servlet container needed — it runs as a CLI application.

**Changes to `pom.xml`** — add build plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**Build and run**:

```bash
# Build the executable jar
mvn clean package -DskipTests

# Run it
java -jar target/spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
```

**Optional: GraalVM Native Image** (advanced, truly native binary):

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <builder>paketobuildpacks/builder-jammy-java:latest</builder>
        </image>
    </configuration>
</plugin>
```

```bash
mvn native:compile -DskipTests
./target/spring-ai-cli-agent
```

**Note**: Native image requires GraalVM JDK and may not work with all tools out of the box.
Start with the regular jar.

---

## FINAL PROJECT STATE

After all 7 steps, the project has:

| Feature | Concept Taught |
|---|---|
| ChatClient with Ollama | Spring AI basics, ChatModel auto-configuration |
| Chat memory | Advisors, conversation persistence |
| AskUserQuestionTool | Tool calling, user interaction |
| CalculatorTool | Custom tools with `@Tool` |
| UnitConverterTool | Multiple tools, AI tool selection |
| SimpleLoggerAdvisor | Advisor composition |
| Executable jar | Spring Boot packaging |

### `pom.xml` (complete)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
        https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>spring-ai-cli-agent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>spring-ai-cli-agent</name>
    <description>Spring AI CLI Agent — Learning Project</description>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>2.0.0</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-ollama</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springaicommunity</groupId>
            <artifactId>spring-ai-agent-utils</artifactId>
            <version>0.10.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### `application.yaml` (complete)

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: lfm2.5
```

### `AgentConfiguration.java` (complete)

```java
package com.example.cliai.agent;

import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultTools(
                AskUserQuestionTool.builder()
                    .questionHandler(new CommandLineQuestionHandler())
                    .build(),
                new CalculatorTool(),
                new UnitConverterTool()
            )
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
```

### `ChatLoop.java` (complete)

```java
package com.example.cliai.cli;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class ChatLoop implements CommandLineRunner {

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

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input.trim()) || "quit".equalsIgnoreCase(input.trim())) {
                    System.out.println("Goodbye!");
                    break;
                }

                try {
                    String response = chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-1"))
                        .call()
                        .content();
                    System.out.println("\nAI: " + response + "\n");
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
    }
}
```

---

## HOW TO USE THIS BLUEPRINT

### For learning (step by step)

1. Create the project from scratch, implementing one step at a time
2. After each step, run `mvn spring-boot:run` and verify
3. Read the Spring AI docs for the concept you just added
4. Experiment — change the system prompt, add parameters, break things

### For the Baeldung article

The 7 steps map to sections in the article:
- Steps 1-3 → "Project Setup" + "Building the Agent"
- Steps 4-5 → "Custom Tools"
- Step 6 → "Advisors"
- Step 7 → "Packaging"

### For a downloadable tool

After Step 7, you have a self-contained jar that anyone can run:
```bash
java -jar spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
```

---

## COMMON ISSUES

| Issue | Fix |
|---|---|
| `Connection refused` on Ollama | `ollama serve` must be running |
| Tool calling doesn't work | Use a model that supports it: `lfm2.5`, `qwen2.5`, `llama3.1`, `mistral` |
| Slow first response | Ollama loads the model into RAM on first call; subsequent calls are fast |
| `OutOfMemoryError` | Use a smaller model: `ollama pull lfm2.5` |
| Build fails on native image | Skip native for now, use `java -jar` |

---

## NEXT STEPS (post-tutorial)

After completing this project, consider exploring:

- **Subagent orchestration** — delegate tasks to specialized agents
- **RAG** — add document retrieval with vector stores
- **Streaming** — use `.stream()` instead of `.call()` for real-time output
- **Multi-model** — route different tasks to different models

---

## BACKEND MODULE (Enterprise Demo)

The backend module provides a Vaadin web UI, REST API, and MCP client for enterprise demos.

### Backend Dependencies

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.0</version>
</parent>

<properties>
    <java.version>17</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-spring-boot-starter</artifactId>
        <version>25.2.6</version>
    </dependency>
</dependencies>
```

### Backend Application.yaml

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: lfm2.5
    mcp:
      client:
        enabled: false
        sse:
          connections:
            polyglot:
              url: http://localhost:9000

server:
  port: 8080
```

### Backend REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/edge/infer` | POST | Single-shot LLM inference |
| `/edge/chat/{chatId}` | POST | Chat with memory (query param: message) |
| `/edge/toolChat/{chatId}` | POST | Chat with MCP tools (query param: message) |

### Vaadin ChatView (Minimal)

```java
@Route("chat")
public class ChatView extends Composite<Div> {

    private final ChatService chatService;
    private final TextField input = new TextField();
    private final Div messages = new Div();

    public ChatView(ChatService chatService) {
        this.chatService = chatService;
        // Build minimal chat UI
        // Input field + send button
        // Message list
    }
}
```

### Backend Tests

```bash
cd backend && mvn test
```
