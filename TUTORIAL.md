# Tutorial: Build a Spring AI CLI Agent

A hands-on guide to building a terminal-based AI assistant using Spring AI, with conversation memory, tool calling, and debugging — all running locally with Ollama.

**Time**: 3-4 hours  
**Prerequisites**: Java 17+, Maven 3.6+, Ollama  
**Stack**: Spring Boot 4 + Spring AI 2.0 + Ollama (lfm2.5)

---

## What You'll Build

A CLI agent that:
- Accepts user input from the terminal and responds via a local LLM
- Remembers previous messages in the conversation
- Asks clarifying questions when needed
- Evaluates math expressions (calculator tool)
- Converts units (km/miles, kg/lbs, etc.)
- Logs prompts and responses for debugging
- Packages as a standalone executable jar

---

## Prerequisites

### Install Java and Maven

```bash
# Verify Java 17+
java -version

# Verify Maven 3.6+
mvn -version
```

### Install Ollama

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh

# Pull a model with tool-calling support
ollama pull lfm2.5

# Verify it works
ollama run lfm2.5 "Say hello"
```

**Why Ollama?** Free, private, no API keys, no billing. Everything runs on your machine.

### Create the Project

Spring provides a convenient tool to create project scaffolding (pom.xml directories, and some starting files) through the [Spring initializer website](https://start.spring.io/). However, we can also create the basic structure manually if needed:

```bash
mkdir spring-ai-cli-agent
cd spring-ai-cli-agent
```

Create `pom.xml`:

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

Create the directory structure:

```bash
mkdir -p src/main/java/com/example/cliai/agent/tools
mkdir -p src/main/java/com/example/cliai/cli
mkdir -p src/main/resources
mkdir -p src/test/java/com/example/cliai/agent/tools
mkdir -p src/test/java/com/example/cliai/agent
mkdir -p src/test/java/com/example/cliai/cli
```

---

## Step 1: Basic ChatClient

**Concept**: `ChatClient` is to LLMs what `JdbcTemplate` is to databases — a fluent API that sends prompts and gets responses. You don't call the LLM directly; you go through `ChatClient`.

### 1.1 Configure Ollama

Create `src/main/resources/application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=lfm2.5
```

### 1.2 Create the Entry Point

Create `src/main/java/com/example/cliai/Application.java`:

```java
package com.example.cliai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 1.3 Create the ChatClient Bean

Spring Boot auto-configures `ChatModel` from your `application.properties`. We wrap it in a `ChatClient` bean.

Create `src/main/java/com/example/cliai/agent/AgentConfiguration.java`:

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AgentConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
```

### 1.4 Build the CLI Loop

Create `src/main/java/com/example/cliai/cli/ChatLoop.java`:

```java
package com.example.cliai.cli;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
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

### 1.5 Verify

```bash
mvn spring-boot:run
# Type: What is 2+2?
# You should get a response from the LLM
```

---

## Step 2: Chat Memory

**Concept**: Advisors are like AOP aspects — they wrap AI calls with cross-cutting behavior. `MessageChatMemoryAdvisor` adds conversation history so the AI remembers previous turns.

Without memory, each prompt is independent. The AI has no idea what you said before. Advisors fix this by injecting past messages into the context window.

### 2.1 Update AgentConfiguration

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
```

### 2.2 Update ChatLoop

Add a conversation ID so the AI tracks sessions:

```java
String response = chatClient.prompt()
    .user(input)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-1"))
    .call()
    .content();
```

### 2.3 Verify

```bash
mvn spring-boot:run
# Type: My name is Alice
# Type: What's my name?
# AI should remember "Alice"
```

---

## Step 3: AskUserQuestionTool

**Concept**: Tools are like stored procedures — plugins the AI can invoke when it needs external data or user input. The AI *decides* when to call them based on the tool's description.

`AskUserQuestionTool` lets the AI ask clarifying questions mid-conversation. For example, if you say "Help me learn Spring AI," the AI might ask "What's your experience level?" before giving advice.

> **Why registration alone is not enough:** Exposing a tool makes it *available*, not *preferred*. LLMs are pre-trained to clarify in plain text. Without an explicit nudge the model will often skip the tool and emit `What's your experience?` as assistant text — so `CommandLineQuestionHandler` never fires. That nudge belongs in the **system prompt generically** (`use an available tool to ask - never ask in ordinary assistant text`) without naming `AskUserQuestionTool`, while the tool's own `@Tool` description + `inputSchema` per [Claude spec](https://code.claude.com/docs/en/agent-sdk/user-input#question-format) and [AskUserQuestionTool.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/AskUserQuestionTool.md) already documents `questions[]:{question,header≤12,options[2-4]{label,description},multiSelect}` and `answers:{question:label}`. This keeps concerns separated: system prompt nudges *how to ask*, tool defines *what to ask*, and QnA stays a separate first-class tool per [Spring blog](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) (`AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()`).

### 3.1 Add Dependencies

Add to `pom.xml` (`spring-ai-agent-utils` for `AskUserQuestionTool`; `spring-ai-starter-mcp-client` for optional MCP `SyncMcpToolCallbackProvider` used in the production `AgentConfiguration`):

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.10.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

### 3.2 Update AgentConfiguration

Keep the system prompt **tool-oblivious** but directive – it says *use an available tool to ask, never ask in ordinary text* without naming `AskUserQuestionTool` or its `{"questions":...}` schema (per tool's own `inputSchema` + Claude spec). QnA is a first-class tool per [blog](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) and [docs](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/AskUserQuestionTool.md) – implemented separately with `CommandLineQuestionHandler`:

```java
package com.example.cliai.agent;

import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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
            .defaultSystem("""
                You are an interactive CLI assistant.
                Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
                """)
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

> **Note:** `defaultSystem` must come before `defaultTools`/`defaultAdvisors`. This simplified `defaultTools(AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build())` is exactly how `AskUserQuestionTool.md: Console-Based Implementation` and the blog's `ask-user-question-demo` wire QnA – see 3.3 for the production embellishment.

### 3.3 QnA implemented separately + pure trace embellishment (production)

The tutorial `defaultTools` works, but the checked-in `AgentConfiguration.java` keeps QnA as a separate first-class tool per [blog](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) and [AskUserQuestionTool.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/AskUserQuestionTool.md) + Claude spec [`questions[]:{question,header≤12,options[2-4]{label,description},multiSelect}`](https://code.claude.com/docs/en/agent-sdk/user-input#question-format) – not bundled via AoP `if (name.contains(...))`. Two dedicated decorators:

```java
// QnA implemented separately – not ToolCallbacks.from(askTool, calc, converter) with AoP branching
AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
    .questionHandler(new CommandLineQuestionHandler())
    .build();

ToolCallback qnaNormalized = new AskUserQuestionNormalizationCallback(
    ToolCallbacks.from(askUserQuestionTool)[0]); // QnA-specific repair only
ToolCallback[] domainCallbacks = ToolCallbacks.from(new CalculatorTool(), new UnitConverterTool());

ToolCallback[] visibleTools = java.util.stream.Stream.concat(
        java.util.stream.Stream.of(qnaNormalized),
        java.util.Arrays.stream(domainCallbacks))
    .map(UserVisibleToolCallback::new) // pure trace for all, no definition mutation
    .toArray(ToolCallback[]::new);

return ChatClient.builder(chatModel)
    .defaultSystem("""
        You are an interactive CLI assistant.
        Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
        """)
    .defaultToolCallbacks(visibleTools)
    .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();
```

Create `src/main/java/com/example/cliai/agent/UserVisibleToolCallback.java` (pure embellishment – no `if-else`, no `ObjectMapper`):

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Adds a concise, user-visible trace around a tool invocation – pure embellishment, no definition mutation. */
final class UserVisibleToolCallback implements ToolCallback {
    private final ToolCallback delegate;
    UserVisibleToolCallback(ToolCallback delegate) { this.delegate = delegate; }
    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
    @Override public String call(String arguments) { return invoke(arguments, () -> delegate.call(arguments)); }
    @Override public String call(String arguments, ToolContext context) { return invoke(arguments, () -> delegate.call(arguments, context)); }
    private String invoke(String arguments, java.util.function.Supplier<String> invocation) {
        System.out.println("\n[Tool] " + getToolDefinition().name());
        System.out.println("[Tool arguments] " + arguments);
        try { String result = invocation.get(); System.out.println("[Tool result] " + result); return result; }
        catch (RuntimeException e) { System.out.println("[Tool error] " + e.getMessage()); throw e; }
    }
}
```

Create `src/main/java/com/example/cliai/agent/AskUserQuestionNormalizationCallback.java` (QnA-specific – repairs `lfm2.5` flat `{"question","header","options"}` → `{"questions":[...]}` per spec, applied only to QnA callback):

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** QnA-specific payload normalizer – not a generic AoP `if-else`. */
final class AskUserQuestionNormalizationCallback implements ToolCallback {
    private final ToolCallback delegate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    AskUserQuestionNormalizationCallback(ToolCallback delegate) { this.delegate = delegate; }
    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
    @Override public String call(String arguments) { return delegate.call(normalize(arguments)); }
    @Override public String call(String arguments, ToolContext context) { return delegate.call(normalize(arguments), context); }
    private String normalize(String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments);
            if (root.isObject() && root.has("options") && !root.has("questions")) {
                ObjectNode q = (ObjectNode) root.deepCopy();
                if (!q.has("question")) q.put("question", q.path("header").asText("Please choose an option") + ". Please choose an option.");
                ObjectNode w = objectMapper.createObjectNode(); w.putArray("questions").add(q);
                return objectMapper.writeValueAsString(w);
            }
        } catch (Exception ignored) {}
        return arguments;
    }
}
```

**Jobs:** `AskUserQuestionNormalizationCallback` – QnA-specific spec repair (flat → `questions[]`) without generic `if (name.contains)`; `UserVisibleToolCallback` – generic visible trace (`[Tool]`/`[Tool arguments]`/`[Tool result]`) for all tools, verified by `ToolCallingEvalTest` with `lfm2.5`. Stock `AskUserQuestionTool` description + `inputSchema` already follows Claude spec; validation (`InvalidUserAnswerException`) stays in tool per `AskUserQuestionTool.md: Error Handling`.

### 3.4 Verify

```bash
mvn spring-boot:run
# Type: Help me learn Spring AI
# The AI should ask about your experience level, interests, etc. via AskUserQuestionTool
# Answer the questions — the AI will tailor its response
# If the model asks in plain text instead, check: (1) defaultSystem contains "use an available tool to ask - never ask in ordinary assistant text" (3.2) and (2) UserVisibleToolCallback is wired via ToolCallbacks.from(...).map(UserVisibleToolCallback::new) → defaultToolCallbacks (3.3) – see Common Issues
```

---

## Step 4: Custom Tool (Calculator)

**Concept**: You can build your own tools by implementing a method annotated with `@Tool`. The `description` tells the AI *when* to use it. The `@ToolParam` annotations describe each parameter.

### 4.1 Create CalculatorTool

Create `src/main/java/com/example/cliai/agent/tools/CalculatorTool.java`:

```java
package com.example.cliai.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CalculatorTool {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Tool(description = "Evaluate a mathematical expression. Supports +, -, *, /, parentheses. Example: (2 + 3) * 4")
    double calculate(
            @ToolParam(description = "The math expression to evaluate") String expression) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("pi", Math.PI);
            var result = parser.parseExpression(expression).getValue(context);
            return result instanceof Number n ? n.doubleValue() : Double.parseDouble(result.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot evaluate: " + expression, e);
        }
    }
}
```

We use Spring Expression Language (SpEL) instead of `javax.script` because Java 17+ removed the Nashorn JavaScript engine.

### 4.2 Register the Tool

```java
.defaultTools(
    AskUserQuestionTool.builder()
        .questionHandler(new CommandLineQuestionHandler())
        .build(),
    new CalculatorTool()
)
```

### 4.3 Verify

```bash
mvn spring-boot:run
# Type: What is (15 * 7) + 23?
# AI should call the calculator tool and give you the answer
```

---

## Step 5: Multiple Tools

**Concept**: When multiple tools are registered, the AI picks the right one based on the user's request. It can even call multiple tools in sequence for complex questions.

### 5.1 Create UnitConverterTool

Create `src/main/java/com/example/cliai/agent/tools/UnitConverterTool.java`:

```java
package com.example.cliai.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class UnitConverterTool {

    @Tool(description = "Convert between units. Supports: km/miles, kg/lbs, celsius/fahrenheit, liters/gallons")
    String convert(
            @ToolParam(description = "The value to convert") double value,
            @ToolParam(description = "Source unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String from,
            @ToolParam(description = "Target unit (e.g., km, miles, kg, lbs, celsius, fahrenheit)") String to) {

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

### 5.2 Register Both Tools

```java
.defaultTools(
    AskUserQuestionTool.builder()
        .questionHandler(new CommandLineQuestionHandler())
        .build(),
    new CalculatorTool(),
    new UnitConverterTool()
)
```

### 5.3 Verify

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

## Step 6: Logging Advisor

**Concept**: Advisors wrap around every AI call. A logging advisor shows you what's being sent to the model and what comes back — invaluable for debugging.

### 6.1 Update AgentConfiguration

```java
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

// ...

.defaultAdvisors(
    new SimpleLoggerAdvisor(),
    MessageChatMemoryAdvisor.builder(chatMemory).build()
)
```

### 6.2 Verify

```bash
mvn spring-boot:run
# Type anything
# You'll see the full prompt and response logged to console
```

---

## Step 7: Packaging as Executable Jar

**Concept**: Spring Boot's Maven plugin packages your app as a self-contained executable jar. No servlet container needed — it runs as a CLI application.

### 7.1 Build

```bash
mvn clean package -DskipTests
```

### 7.2 Run

```bash
java -jar target/spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
```

The jar includes all dependencies and the Spring Boot loader. Anyone with Java 17+ can run it.

---

## Step 8: Unit Testing

**Concept**: Write unit tests for your tools and components using JUnit 5, AssertJ, and Mockito. These tests run without Ollama — they test your code logic, not the LLM.

### 8.1 Add Test Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 8.2 CalculatorToolTest

Create `src/test/java/com/example/cliai/agent/tools/CalculatorToolTest.java`:

```java
package com.example.cliai.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorToolTest {

    private CalculatorTool calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
    }

    @Test
    void shouldAddTwoNumbers() {
        assertThat(calculator.calculate("2 + 3")).isEqualTo(5.0);
    }

    @Test
    void shouldSubtractTwoNumbers() {
        assertThat(calculator.calculate("10 - 4")).isEqualTo(6.0);
    }

    @Test
    void shouldMultiplyTwoNumbers() {
        assertThat(calculator.calculate("6 * 7")).isEqualTo(42.0);
    }

    @Test
    void shouldDivideTwoNumbers() {
        assertThat(calculator.calculate("15 / 3")).isEqualTo(5.0);
    }

    @Test
    void shouldHandleParentheses() {
        assertThat(calculator.calculate("(2 + 3) * 4")).isEqualTo(20.0);
    }

    @Test
    void shouldHandleNestedParentheses() {
        assertThat(calculator.calculate("((1 + 2) * (3 + 4))")).isEqualTo(21.0);
    }

    @Test
    void shouldHandleComplexExpression() {
        assertThat(calculator.calculate("(15 * 7) + 23")).isEqualTo(128.0);
    }

    @Test
    void shouldReturnDecimalResult() {
        assertThat(calculator.calculate("10.0 / 3"))
            .isCloseTo(3.333, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void shouldHandleSingleNumber() {
        assertThat(calculator.calculate("42")).isEqualTo(42.0);
    }

    @Test
    void shouldThrowOnDivisionByZero() {
        assertThatThrownBy(() -> calculator.calculate("1 / 0"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnInvalidExpression() {
        assertThatThrownBy(() -> calculator.calculate("abc"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnEmptyExpression() {
        assertThatThrownBy(() -> calculator.calculate(""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

### 8.3 UnitConverterToolTest

Create `src/test/java/com/example/cliai/agent/tools/UnitConverterToolTest.java`:

```java
package com.example.cliai.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitConverterToolTest {

    private UnitConverterTool converter;

    @BeforeEach
    void setUp() {
        converter = new UnitConverterTool();
    }

    @Test
    void shouldConvertKmToMiles() {
        String result = converter.convert(100, "km", "miles");
        assertThat(result).contains("62.1371").contains("miles");
    }

    @Test
    void shouldConvertMilesToKm() {
        String result = converter.convert(62.1371, "miles", "km");
        assertThat(result).contains("km");
    }

    @Test
    void shouldConvertKgToLbs() {
        String result = converter.convert(10, "kg", "lbs");
        assertThat(result).contains("22.0462").contains("lbs");
    }

    @Test
    void shouldConvertLbsToKg() {
        String result = converter.convert(22.0462, "lbs", "kg");
        assertThat(result).contains("kg");
    }

    @Test
    void shouldConvertCelsiusToFahrenheit() {
        String result = converter.convert(100, "celsius", "fahrenheit");
        assertThat(result).contains("212.0").contains("°F");
    }

    @Test
    void shouldConvertFahrenheitToCelsius() {
        String result = converter.convert(212, "fahrenheit", "celsius");
        assertThat(result).contains("100.0").contains("°C");
    }

    @Test
    void shouldConvertLitersToGallons() {
        String result = converter.convert(10, "liters", "gallons");
        assertThat(result).contains("2.64172").contains("gallons");
    }

    @Test
    void shouldConvertGallonsToLiters() {
        String result = converter.convert(10, "gallons", "liters");
        assertThat(result).contains("37.8541").contains("liters");
    }

    @Test
    void shouldHandleZeroValue() {
        String result = converter.convert(0, "km", "miles");
        assertThat(result).contains("0.0").contains("miles");
    }

    @Test
    void shouldHandleNegativeValue() {
        String result = converter.convert(-40, "celsius", "fahrenheit");
        assertThat(result).contains("-40.0").contains("°F");
    }

    @Test
    void shouldReturnUnsupportedForUnknownConversion() {
        String result = converter.convert(10, "km", "kg");
        assertThat(result).contains("Unsupported conversion");
    }

    @Test
    void shouldHandleCaseInsensitiveUnits() {
        String result = converter.convert(100, "KM", "MILES");
        assertThat(result).contains("62.1371").contains("miles");
    }
}
```

### 8.4 AgentConfigurationTest

Create `src/test/java/com/example/cliai/agent/AgentConfigurationTest.java` (verifies tool-oblivious system prompt and pure embellisher – matches checked-in test):

```java
package com.example.cliai.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConfigurationTest {

    @Test
    void shouldCreateChatClientBean() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);
        ChatClient chatClient = config.chatClient(chatModel, mcpProvider);
        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateDistinctChatClientInstances() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);
        ChatClient client1 = config.chatClient(chatModel, mcpProvider);
        ChatClient client2 = config.chatClient(chatModel, mcpProvider);
        assertThat(client1).isNotSameAs(client2);
    }

    @Test
    void defaultSystemShouldBeToolOblivious() {
        ChatModel chatModel = mock(ChatModel.class);
        org.springframework.ai.chat.prompt.ChatOptions opts = org.springframework.ai.chat.prompt.ChatOptions.builder().build();
        when(chatModel.getDefaultOptions()).thenReturn(opts);
        when(chatModel.getOptions()).thenReturn(opts);
        ChatResponse dummy = new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(dummy);
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider = mock(ObjectProvider.class);
        AgentConfiguration config = new AgentConfiguration();
        ChatClient chatClient = config.chatClient(chatModel, mcpProvider);
        chatClient.prompt().user("Hello").advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, "test-1")).call().content();
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();
        String promptText = prompt.getInstructions().toString() + " " + prompt.getContents();
        assertThat(promptText).contains("You are an interactive CLI assistant");
        assertThat(promptText).contains("Be helpful, concise");
        assertThat(promptText).contains("use an available tool to ask");
        assertThat(promptText).contains("never ask in ordinary assistant text");
        assertThat(promptText).doesNotContain("AskUserQuestionTool");
        assertThat(promptText).doesNotContain("questions array");
        assertThat(promptText).doesNotContain("The tool input must be a JSON object");
        assertThat(promptText).doesNotContain("\"questions\"");
    }

    @Test
    void askUserQuestionToolDescriptionShouldBePreservedByEmbellisher() {
        ToolCallback delegate = ToolCallbacks.from(
            org.springaicommunity.agent.tools.AskUserQuestionTool.builder()
                .questionHandler(questions -> java.util.Map.of())
                .build()
        )[0];
        ToolCallback wrapped = new UserVisibleToolCallback(delegate);
        assertThat(wrapped.getToolDefinition().description()).isEqualTo(delegate.getToolDefinition().description());
        assertThat(wrapped.getToolDefinition().name()).isEqualTo(delegate.getToolDefinition().name());
        assertThat(wrapped.getToolDefinition().inputSchema()).isEqualTo(delegate.getToolDefinition().inputSchema());
        assertThat(wrapped.getToolDefinition().description()).contains("Use this tool when you need to ask the user questions");
        assertThat(wrapped.getToolDefinition().inputSchema()).contains("questions");
    }

    @Test
    void tutorialStep3MustDocumentSeparateQnAImplementation() throws Exception {
        Path tutorial = Path.of("").toAbsolutePath().resolve("TUTORIAL.md");
        if (!Files.exists(tutorial)) tutorial = Path.of("../TUTORIAL.md").toAbsolutePath().normalize();
        if (!Files.exists(tutorial)) tutorial = Path.of("../../TUTORIAL.md").toAbsolutePath().normalize();
        assertThat(Files.exists(tutorial)).as("TUTORIAL.md must exist").isTrue();
        String content = Files.readString(tutorial);
        assertThat(content).contains("Why registration alone is not enough");
        assertThat(content).contains("tool-oblivious");
        assertThat(content).contains("UserVisibleToolCallback");
        assertThat(content).contains("AskUserQuestionTool.builder()");
        assertThat(content).contains("CommandLineQuestionHandler");
        assertThat(content).contains("code.claude.com/docs/en/agent-sdk/user-input#question-format");
        assertThat(content).contains("You are an interactive CLI assistant.");
        assertThat(content).contains("Be helpful, concise");
        assertThat(content).contains("use an available tool to ask");
        assertThat(content).contains("never ask in ordinary assistant text");
    }
}
```

### 8.5 ChatLoopTest

Create `src/test/java/com/example/cliai/cli/ChatLoopTest.java` (covers `/help`/`/tools`/`/clear`, graceful EOF, and streaming – matches checked-in test):

```java
package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatLoopTest {

    @Test
    void shouldExitOnExitCommand() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        try {
            String input = "exit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();
            verify(chatClient, never()).prompt();
        } finally { System.setIn(originalIn); }
    }

    @Test
    void shouldExitOnQuitCommand() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        try {
            String input = "quit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();
            verify(chatClient, never()).prompt();
        } finally { System.setIn(originalIn); }
    }

    @Test
    void shouldExitCleanlyWhenInputEnds() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(new byte[0]));
            chatLoop.run();
            verify(chatClient, never()).prompt();
        } finally { System.setIn(originalIn); }
    }

    @Test
    void shouldHandleSlashCommandsWithoutCallingModel() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("/help\n/tools\n/clear\n/exit\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output));
            chatLoop.run();
            String text = output.toString(StandardCharsets.UTF_8);
            org.assertj.core.api.Assertions.assertThat(text).contains("/help", "CalculatorTool", "Conversation cleared.", "Goodbye!");
            verify(chatClient, never()).prompt();
        } finally { System.setIn(originalIn); System.setOut(originalOut); }
    }

    @Test
    void shouldStreamChatClientResponseOnUserInput() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("AI", " response"));
        ChatLoop chatLoop = new ChatLoop(chatClient);
        InputStream originalIn = System.in;
        try {
            String input = "Hello\nexit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();
            verify(chatClient).prompt();
            verify(requestSpec).user("Hello");
            verify(requestSpec).stream();
            verify(streamSpec).content();
        } finally { System.setIn(originalIn); }
    }
}
```

### 8.5b UserVisibleToolCallbackTest, ChatClientIntegrationTest, ToolCallingEvalTest

Create these additional tests exactly as in the repo (see `src/test/java/com/example/cliai/agent/UserVisibleToolCallbackTest.java`, `ChatClientIntegrationTest.java`, `ToolCallingEvalTest.java`). They verify: trace/normalization (`wrapsSingleAskUserQuestionInQuestionsArray`), live Ollama `lfm2.5` memory (`shouldRememberContextAcrossTurns`), and tool-calling evals (`calculatorPromptMustExecuteCalculatorTool`, `clarificationPromptMustExecuteAskUserQuestionTool`, `sufficientlyAmbiguousPromptShouldTriggerClarificationTool` – run with `mvn test -Devals=true`).

### 8.6 Run Tests

```bash
mvn test
# 42 tests: 39 unit/integration + 3 evals skipped (run with -Devals=true + Ollama lfm2.5 for evals)
# e.g. mvn test -Devals=true -Dtest=ToolCallingEvalTest#clarificationPromptMustExecuteAskUserQuestionTool

mvn test -pl backend
# 10 tests (GraphQL + REST mocked, MCP skipped without -Dmcp.integration)
```

---

## Final Project Structure

```
spring-ai-cli-agent/
├── pom.xml
├── src/main/java/com/example/cliai/
│   ├── Application.java
│   ├── agent/
│   │   ├── AgentConfiguration.java
│   │   ├── UserVisibleToolCallback.java  # pure trace embellishment
│   │   └── tools/
│   │       ├── CalculatorTool.java
│   │       └── UnitConverterTool.java
│   └── cli/
│       └── ChatLoop.java
├── src/main/resources/
│   └── application.properties
└── src/test/java/com/example/cliai/
├── agent/
│   ├── AgentConfiguration.java
│   ├── UserVisibleToolCallback.java  # pure trace embellishment
│   ├── AskUserQuestionNormalizationCallback.java  # QnA-specific flat→questions repair
│   └── tools/
    │       ├── CalculatorToolTest.java
    │       └── UnitConverterToolTest.java
    └── cli/
        ├── ChatLoopTest.java
        ├── ChatClientIntegrationTest.java   # needs Ollama lfm2.5
        └── ToolCallingEvalTest.java         # -Devals=true, needs Ollama lfm2.5
```

---

## Complete pom.xml

> Matches `spring-ai-cli-agent/pom.xml` – use this as final state if you followed Steps 1, 3.1, 8.1 incrementally:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version><relativePath/></parent>
    <groupId>com.example</groupId><artifactId>spring-ai-cli-agent</artifactId><version>0.0.1-SNAPSHOT</version><name>spring-ai-cli-agent</name><description>Spring AI CLI Agent — Learning Project</description>
    <properties><java.version>17</java.version><spring-ai.version>2.0.0</spring-ai.version></properties>
    <dependencyManagement><dependencies><dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-bom</artifactId><version>${spring-ai.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
    <dependencies>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-ollama</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-mcp-client</artifactId></dependency>
        <dependency><groupId>org.springaicommunity</groupId><artifactId>spring-ai-agent-utils</artifactId><version>0.10.0</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
```

## Complete application.properties

```properties
# Local Ollama endpoint and chat model used by Spring AI.
# Default is lfm2.5 (5.2 GB, fastest, tools+thinking). Alternatives under 10 GB with tools:
# qwen3.5:9b (6.6 GB, 256K) and gemma4:e4b (9.6 GB, vision+audio) — see ../../ollama-model-links.md
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=lfm2.5
# MCP client is disabled by default so unit tests run without external dependencies.
spring.ai.mcp.client.enabled=false
spring.ai.mcp.client.sse.connections.polyglot.url=http://localhost:9000
server.port=8081
```

## Complete AgentConfiguration.java

```java
package com.example.cliai.agent;

import com.example.cliai.agent.tools.CalculatorTool;
import com.example.cliai.agent.tools.UnitConverterTool;
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
    ChatClient chatClient(ChatModel chatModel, ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        // QnA implemented separately per https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool
        // and AskUserQuestionTool.md – QnA-specific normalization isolated from generic trace embellishment
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        ToolCallback qnaNormalized = new AskUserQuestionNormalizationCallback(
            ToolCallbacks.from(askUserQuestionTool)[0]);
        ToolCallback[] domainCallbacks = ToolCallbacks.from(new CalculatorTool(), new UnitConverterTool());

        ToolCallback[] visibleTools = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(qnaNormalized),
                java.util.Arrays.stream(domainCallbacks))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
                """)
            .defaultToolCallbacks(visibleTools)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
```

QnA is a separate first-class tool (`AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()` per blog/docs, spec `questions[]:{question,header,options{label,description},multiSelect}`); `AskUserQuestionNormalizationCallback` repairs `lfm2.5` flat `{"question","header","options"}` → `{"questions":[...]}` (QnA-specific), `UserVisibleToolCallback` is pure trace embellishment (no `if-else`).

## Complete UserVisibleToolCallback.java

> Exactly `src/main/java/com/example/cliai/agent/UserVisibleToolCallback.java` – create this file in Step 3.3 to make the tutorial line-by-line complete:

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Adds a concise, user-visible trace around a tool invocation – pure embellishment, no definition mutation. */
final class UserVisibleToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    UserVisibleToolCallback(ToolCallback delegate) { this.delegate = delegate; }
    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
    @Override public String call(String arguments) { return invoke(arguments, () -> delegate.call(arguments)); }
    @Override public String call(String arguments, ToolContext context) { return invoke(arguments, () -> delegate.call(arguments, context)); }
    private String invoke(String arguments, java.util.function.Supplier<String> invocation) {
        System.out.println("\n[Tool] " + getToolDefinition().name());
        System.out.println("[Tool arguments] " + arguments);
        try {
            String result = invocation.get();
            System.out.println("[Tool result] " + result);
            return result;
        } catch (RuntimeException exception) {
            System.out.println("[Tool error] " + exception.getMessage());
            throw exception;
        }
    }
}
```

### Complete AskUserQuestionNormalizationCallback.java

> QnA-specific repair per Claude spec [`questions[]:{question,header,options{label,description},multiSelect}`](https://code.claude.com/docs/en/agent-sdk/user-input#question-format) – applied only to the QnA callback, not via generic `if-else`:

```java
package com.example.cliai.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** QnA-specific payload normalizer – not a generic AoP `if-else`. */
final class AskUserQuestionNormalizationCallback implements ToolCallback {
    private final ToolCallback delegate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    AskUserQuestionNormalizationCallback(ToolCallback delegate) { this.delegate = delegate; }
    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }
    @Override public String call(String arguments) { return delegate.call(normalize(arguments)); }
    @Override public String call(String arguments, ToolContext context) { return delegate.call(normalize(arguments), context); }
    private String normalize(String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments);
            if (root.isObject() && root.has("options") && !root.has("questions")) {
                ObjectNode q = (ObjectNode) root.deepCopy();
                if (!q.has("question")) q.put("question", q.path("header").asText("Please choose an option") + ". Please choose an option.");
                ObjectNode w = objectMapper.createObjectNode(); w.putArray("questions").add(q);
                return objectMapper.writeValueAsString(w);
            }
        } catch (Exception ignored) {}
        return arguments;
    }
}
```

### Complete CalculatorTool.java / UnitConverterTool.java

Exactly as in Steps 4.1/5.1 with `public class` and `context.setVariable("pi", Math.PI)` for `CalculatorTool` – copy verbatim to match repo.

---

## Complete ChatLoop.java

> The checked-in `ChatLoop.java` is the streaming variant (Step 10 is already applied). It uses `UUID` session IDs, `/help`/`/tools`/`/clear` commands, and `stream().content().doOnNext().blockLast()` for token streaming. If you followed Steps 1-6 literally you have the simpler `call().content()` loop – replace it with this complete file to match the repo:

```java
package com.example.cliai.cli;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class ChatLoop implements CommandLineRunner {

    private static final String SESSION_ID_PREFIX = "session-";

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
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                if (!scanner.hasNextLine()) {
                    System.out.println("\nGoodbye!");
                    break;
                }
                String input = scanner.nextLine();
                String command = input.trim().toLowerCase();
                if ("exit".equals(command) || "quit".equals(command) || "/exit".equals(command)) {
                    System.out.println("Goodbye!");
                    break;
                }
                if ("/help".equals(command)) {
                    printHelp();
                    continue;
                }
                if ("/tools".equals(command)) {
                    printTools();
                    continue;
                }
                if ("/clear".equals(command)) {
                    sessionId.set(SESSION_ID_PREFIX + UUID.randomUUID());
                    System.out.println("Conversation cleared.\n");
                    continue;
                }

                try {
                    System.out.print("\nAI: ");
                    chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.get()))
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .blockLast();
                    System.out.println("\n");
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void printHelp() {
        System.out.println("\nCommands:");
        System.out.println("  /help   Show this help");
        System.out.println("  /tools  List available tools");
        System.out.println("  /clear  Start a fresh conversation");
        System.out.println("  /exit   Exit the CLI");
        System.out.println("  exit    Exit the CLI\n");
    }

    private void printTools() {
        System.out.println("\nAvailable tools:");
        System.out.println("  CalculatorTool      Evaluate mathematical expressions");
        System.out.println("  UnitConverterTool   Convert supported units");
        System.out.println("  AskUserQuestionTool Let the agent ask a clarifying question\n");
    }
}
```

---

## Common Issues

| Issue | Fix |
|-------|-----|
| `Connection refused` on Ollama | Run `ollama serve` first |
| Tool calling doesn't work | Use a model with `tools` support — `lfm2.5` (default, 5.2 GB), `qwen3.5:9b` (6.6 GB) or `gemma4:e4b` (9.6 GB). See [`ollama-model-links.md`](ollama-model-links.md) for the full $<10$ GB comparison |
| Model asks clarifying question in plain text, never triggers `AskUserQuestionTool` | QnA not registered as separate first-class tool per blog/docs – ensure `AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()` is registered (tutorial 3.2, production `qnaCallbacks`/`domainCallbacks` in 3.3) and `defaultSystem` contains `use an available tool to ask - never ask in ordinary assistant text` (tool-oblivious nudge) |
| Slow first response | Ollama loads the model into RAM on first call; subsequent calls are fast |
| `OutOfMemoryError` | Use a smaller model: `ollama pull lfm2.5` or `qwen3.5:4b` (3.4 GB) |
| `javax.script` errors | We use SpEL instead — Java 17+ removed Nashorn |

---

## Next Steps

After completing this tutorial:

- **Streaming** — use `.stream()` instead of `.call()` for real-time output
- **RAG** — add document retrieval with vector stores
- **MCP** — connect to external tool servers
- **Multi-model** — route different tasks to different models
- **Subagent orchestration** — delegate tasks to specialized agents

---

## Step 9: Enterprise Backend (Optional)

The `backend/` module provides a Vaadin web UI and REST API for enterprise demos.

### Architecture

```
Backend (port 8080)          CLI Agent (port 8081)
┌─────────────────┐          ┌─────────────────┐
│  Vaadin Web UI  │          │  Terminal REPL   │
│  /chat          │          │                 │
├─────────────────┤          └─────────────────┘
│  REST API       │
│  /edge/*        │          Both use the same
├─────────────────┤          Spring AI ChatClient
│  MCP Client     │          with Ollama (lfm2.5)
│  (polyglot)     │
└─────────────────┘
```

### Run the Backend

```bash
cd backend
mvn spring-boot:run

# Open browser to http://localhost:8080
# You'll see the Vaadin chat interface
```

### REST API (for JavaScript developers)

```bash
# Single-shot inference
curl -X POST http://localhost:8080/edge/infer \
  -H "Content-Type: text/plain" \
  -d "Hello"

# Chat with memory
curl -X POST "http://localhost:8080/edge/chat/USER-123?message=My%20name%20is%20Alice" \
  -H "Content-Type: text/plain"

# Follow up (AI remembers)
curl -X POST "http://localhost:8080/edge/chat/USER-123?message=What%27s%20my%20name%3F" \
  -H "Content-Type: text/plain"
```

### GraphQL (for React/Apollo clients)

The backend also exposes GraphQL at `/graphql`. Use GraphiQL at `/graphiql?path=/graphql` to explore.

```graphql
query {
  infer(prompt: "Hello")
}

query {
  chat(chatId: "USER-123", message: "My name is Alice")
}

mutation {
  toolChat(chatId: "USER-123", message: "Analyze sentiment: I love Java")
}
```

Schema: [`backend/src/main/resources/graphql/schema.graphqls`](backend/src/main/resources/graphql/schema.graphqls)

### Enable MCP (connect to polyglot)

1. Start the polyglot MCP server:
```bash
cd polyglot
mvn clean install
java -jar target/polyglot-runner.jar
```

2. Enable MCP in `backend/src/main/resources/application.properties`:
```properties
spring.ai.mcp.client.enabled=true
```

3. Restart the backend. The AI can now call the sentiment analysis tool.

---

## Step 10: Streaming Responses

By default, ChatClient `.call()` waits for the full response. Streaming shows tokens as they arrive — better UX in both the CLI and Vaadin UI.

### CLI Agent

Replace `.call().content()` with `.stream().content()`:

```java
System.out.print("\nAI: ");
chatClient.prompt()
    .user(input)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-1"))
    .stream()
    .content()
    .doOnNext(System.out::print)
    .blockLast();
System.out.println();
```

Run `mvn spring-boot:run` and watch tokens appear incrementally.

### Vaadin Web UI

In the backend, expose a streaming method on `ChatClients`:

```java
public Flux<String> chatStream(String chatId, String message) {
    return chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
        .stream()
        .content();
}
```

In `ChatView`, run the stream on a background thread and update the UI with `UI.access()` on each chunk:

```java
CompletableFuture.runAsync(() -> {
    StringBuilder response = new StringBuilder();
    chatService.chatStream(conversationId, message)
        .doOnNext(chunk -> {
            response.append(chunk);
            ui.access(() -> aiMessage.setText("AI: " + response));
        })
        .blockLast();
});
```

### Why Streaming Matters

- **Perceived latency** drops — users see output immediately
- **Long responses** feel interactive instead of frozen
- **Same advisors and tools** work with `.stream()` as with `.call()`

---

## References

### Spring AI

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/) — Official reference docs
- [Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html) — Quickstart guide
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) — ChatClient API reference
- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html) — Advisor framework
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tool-calling.html) — Custom tools with `@Tool`

### Ollama

- [Ollama Website](https://ollama.com/) — Download and install
- [Ollama Models](https://ollama.com/library) — Browse available models
- [Ollama GitHub](https://github.com/ollama/ollama) — Source code and issues

### Models

- [lfm2.5](https://ollama.com/library/lfm2.5) — 8B MoE (1B active, 5.2 GB) with tool calling — default
- [qwen3.5:9b](https://ollama.com/library/qwen3.5) — 9B alternative (6.6 GB, 256K, tools+thinking)
- [gemma4:e4b](https://ollama.com/library/gemma4) — 4.5B effective multimodal (9.6 GB, tools+thinking+vision+audio)
- Full comparison of $<10$ GB tools-capable models: [`ollama-model-links.md`](ollama-model-links.md) — single source of truth

### Spring Boot

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/reference/) — Official reference
- [Spring Boot CLI](https://docs.spring.io/spring-boot/reference/packaging/native-image.html) — Native image guide

### Baeldung Articles

- [Spring AI Overview](https://www.baeldung.com/spring-ai) — Baeldung's Spring AI series
- [Introduction to Spring AI](https://www.baeldung.com/spring-ai-introduction) — Getting started with Spring AI

### Vaadin

- [Vaadin Documentation](https://vaadin.com/docs) — Official reference
- [Vaadin Spring Boot](https://vaadin.com/docs/latest/spring/overview) — Spring Boot integration
- [Vaadin AI Integration](https://vaadin.com/docs/latest/building-apps/ai) — Built-in AI components

### Model Context Protocol (MCP)

- [MCP Specification](https://modelcontextprotocol.io/specification/) — Official spec
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) — Spring AI MCP docs

---

## License

MIT License — explore, adapt, and share freely!
