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

### 1.6 Test Implementation

> Tests start at Step 3, but Step 1 is already covered by `ChatClientIntegrationTest.java:24` `shouldGetResponseFromOllama` (real `gemma4:e4b` via `ChatClient`) and `ChatLoopTest.java:22` `shouldExitOnExitCommand` (mocked `ChatClient`, `never().prompt()`). No specific `-Dtest` needed – general setup:

```bash
mvn test # top-level Duke42/ or spring-ai-cli-agent/
# or mvn test -pl spring-ai-cli-agent -am
```

### 1.7 Further Reading

* Spring AI `ChatClient` – fluent `prompt().user().call().content()` vs `JdbcTemplate` analogy
* Spring Boot auto-configuration – `ChatModel` from `application.properties:4` `spring.ai.ollama.*`
* Ollama `lfm2.5` model card – tool-calling, 125K context

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

### 2.4 Test Implementation

> Added in Step 3.5 (`spring-boot-starter-test`), but verifies Step 2:

* `ChatClientIntegrationTest.java:38` `shouldRememberContextAcrossTurns` – real `gemma4:e4b` with `ChatMemory.CONVERSATION_ID` (`session-` + `UUID`) – asserts second turn contains `TestUser123`
* `AgentConfigurationTest.java:40` `shouldCreateDistinctChatClientInstances` – `MessageWindowChatMemory` bean distinct per `chatClient()` call

```bash
mvn test # top-level Duke42/ or spring-ai-cli-agent/
```

### 2.5 Further Reading

* Spring AI `Advisors` – `MessageChatMemoryAdvisor` as AOP aspect, `ChatMemory` + `CONVERSATION_ID` advisoring
* Spring AI `ChatMemory` – `MessageWindowChatMemory.builder().maxMessages(20)`

---

## Step 3: AskUserQuestionTool

**Concept**: Tools are like stored procedures — plugins the AI can invoke when it needs external data or user input. The AI *decides* when to call them based on the tool's description.

`AskUserQuestionTool` lets the AI ask clarifying questions mid-conversation. For example, if you say "Help me learn Spring AI," the AI might ask "What's your experience level?" before giving advice.

> **Why registration alone is not enough:** Exposing a tool makes it *available*, not *preferred*. LLMs are pre-trained to clarify in plain text. Without an explicit nudge the model will often skip the tool and emit `What's your experience?` as assistant text — so `CommandLineQuestionHandler` never fires. That nudge belongs in the **system prompt generically** (`use an available tool to ask - never ask in ordinary assistant text`) without naming `AskUserQuestionTool`, while the tool's own `@Tool` description + `inputSchema` per [Claude spec](https://code.claude.com/docs/en/agent-sdk/user-input#question-format) and [AskUserQuestionTool.md](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/AskUserQuestionTool.md) already documents `questions[]:{question,header≤12,options[2-4]{label,description},multiSelect}` and `answers:{question:label}`. This keeps concerns separated: system prompt nudges *how to ask*, tool defines *what to ask*, and QnA stays a separate first-class tool per [Spring blog](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) (`AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()`).

### 3.1 Dependencies

Only one new dependency for QnA – MCP stays separate (Step 9):

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.10.0</version>
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

### 3.3 Verify

```bash
mvn spring-boot:run
# Type: Help me learn Spring AI
# The AI should ask about your experience level, interests, etc. via AskUserQuestionTool
# Answer the questions — the AI will tailor its response
# If the model asks in plain text instead, check: defaultSystem contains "use an available tool to ask - never ask in ordinary assistant text" and AskUserQuestionTool is registered via AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build() (3.2) – see Common Issues
```

### 3.4 Test Implementation

> Testing starts here (not Step 8) – every chapter adds its own tests. Step 3 introduces the test harness.

**Concept:** Use `spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito). Unit tests mock `ChatModel` (no Ollama); integration/evals use real `lfm2.5` via `ChatClient`.

**Dependencies:** Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Code – `AgentConfigurationTest` + `UserVisibleToolCallbackTest` + `CommandLineQuestionHandlerTest` (unit, no Ollama):**

Create `src/test/java/com/example/cliai/agent/AgentConfigurationTest.java`, `UserVisibleToolCallbackTest.java`, `CommandLineQuestionHandlerTest.java` exactly as in the repo (see `## Complete` sections or copy from `src/test/java`). Key checks:

```java
// AgentConfigurationTest – tool-oblivious system prompt per 3.2, pure embellisher per 3.3
void defaultSystemShouldBeToolOblivious() { /* asserts prompt contains "use an available tool to ask" and not "AskUserQuestionTool" */ }
void askUserQuestionToolDescriptionShouldBePreservedByEmbellisher() { /* wrapped description == delegate description, contains "Use this tool when you need to ask" */ }
void tutorialStep3MustDocumentSeparateQnAImplementation() { /* TUTORIAL.md contains tool-oblivious + AskUserQuestionTool.builder() + CommandLineQuestionHandler + Claude spec link */ }

// UserVisibleToolCallbackTest – pure trace, pass-through
void passesArgumentsThroughUnchangedForAskUserQuestionTool() { /* {"questions":[...]} unchanged */ }
void preservesToolDefinition() { /* name/description/inputSchema equal */ }

// CommandLineQuestionHandlerTest – confirms handler implements full spec (via javap -c handle): header+question, options 2-4, multiSelect, free-text
void shouldHandleSingleSelectViaNumber() { /* Input "2" → "Day.js", prints "Library: Which...", "(Enter a number...)" */ }
void shouldHandleMultiSelectViaCommaSeparatedNumbers() { /* Input "1,3" → "Auth, Cache", prints "(Enter numbers separated by commas...)" */ }
void shouldHandleFreeTextWhenNotANumber() { /* Input "my custom answer" → map entry */ }
void shouldBeWiredInAgentConfiguration() { /* CommandLineQuestionHandler implements QuestionHandler */ }
```

**Code – `ToolCallingEvalTest` (opt-in, needs Ollama `gemma4:e4b` – confirms `CommandLineQuestionHandler` was actually *used* end-to-end):**

Create `src/test/java/com/example/cliai/cli/ToolCallingEvalTest.java` (see repo). Mocks `System.in` with `1\n` via `ByteArrayInputStream` and asserts embellished trace via `UserVisibleToolCallback`:

```bash
mvn test -Devals=true # top-level Duke42/ – runs ToolCallingEvalTest 3 (clarification + ambiguous) via gemma4:e4b-mlx
# No -Dtest needed – general setup covers all (ToolCallingEvalTest skipped without -Devals)
```

```bash
mvn test # top-level
# Step 3 alone: 12 tests (AgentConfigurationTest 5 + UserVisibleToolCallbackTest 3 + CommandLineQuestionHandlerTest 4) – ToolCallingEvalTest 3 skipped without -Devals
```

### 3.5 Further Reading

* `AskUserQuestionTool.md` – `Question Format`/`Answer Format`/`Error Handling` and `CommandLineQuestionHandler` vs `Web/GUI Implementation` (`CompletableFuture` + `/api/answers`)
* Spring Blog `AskUserQuestionTool – Agents That Clarify Before Acting` – QnA as first-class `AskUserQuestionTool.builder().questionHandler(...)` + `defaultTools(askTool)`
* Claude spec `code.claude.com/docs/en/agent-sdk/user-input#question-format` – `questions[]:{question,header≤12,options[2-4]{label,description},multiSelect}` and `canUseTool` `if (toolName=="AskUserQuestion")`
* Spring AI `Tool Calling` – `@Tool`/`@ToolParam`, `ToolCallbacks.from`, `ToolDefinition.inputSchema`

---

## Step 4: Custom Tool (Calculator)

**Concept**: You can build your own tools by implementing a method annotated with `@Tool`. The `description` tells the AI *when* to use it. The `@ToolParam` annotations describe each parameter.

### 4.1 Dependencies

No new dependencies – `SpEL` (`spring-expression`, already via `spring-boot-starter`) is used instead of removed `Nashorn`. `Tools` are discovered via `ToolCallbacks.from(new CalculatorTool())`.

### 4.2 Code Implementation

#### 4.2.1 Create CalculatorTool

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

#### 4.2.2 Register the Tool

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

### 4.4 Test Implementation

* `src/test/java/com/example/cliai/agent/tools/CalculatorToolTest.java:12` – 12 tests (`shouldAddTwoNumbers`, `shouldHandleParentheses`, `shouldThrowOnInvalidExpression` etc., including `pi` via `context.setVariable("pi", Math.PI)`). Historically `Step 8.2`, now introduced here:

```bash
mvn test # top-level Duke42/ or spring-ai-cli-agent/
# 12 tests, SpEL `2 + 3` → `5.0`, `(2+3)*4` → `20.0`, `1/0` → `IllegalArgumentException`
```

### 4.5 Further Reading

* Spring AI `Tool Calling` – `@Tool(description="...")` as selection hint, `@ToolParam` per-param docs
* Spring `SpEL` – `SpelExpressionParser`, `StandardEvaluationContext` vs removed `Nashorn`

---

## Step 5: Multiple Tools

**Concept**: When multiple tools are registered, the AI picks the right one based on the user's request. It can even call multiple tools in sequence for complex questions.

### 5.1 Dependencies

No new dependencies – second tool discovered via same `ToolCallbacks.from(new CalculatorTool(), new UnitConverterTool())` and `UserVisibleToolCallback` trace.

### 5.2 Code Implementation

#### 5.2.1 Create UnitConverterTool

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

#### 5.2.2 Register Both Tools

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

### 5.4 Test Implementation

* `src/test/java/com/example/cliai/agent/tools/UnitConverterToolTest.java:12` – 12 tests (`shouldConvertKmToMiles` `100→62.1371`, `shouldConvertCelsiusToFahrenheit`, `shouldHandleCaseInsensitiveUnits` etc.):

```bash
mvn test # top-level
# 12 tests, `convert(100,"km","miles")` → `62.1371 miles`, unsupported → `Unsupported conversion`
```

### 5.5 Further Reading

* Spring AI `Tool Calling` – multiple `ToolCallbacks`, AI tool selection via `description`
* `ToolCallingEvalTest.java:32` `calculatorPromptMustExecuteCalculatorTool` – `trace.contains("[Tool] calculate","[Tool result] 128.0")` with real `gemma4:e4b-mlx`

---

## Step 6: Logging Advisor

**Concept**: Advisors wrap around every AI call. A logging advisor shows you what's being sent to the model and what comes back — invaluable for debugging.

### 6.1 Dependencies

No new dependencies – `SimpleLoggerAdvisor` is built into `spring-ai-client-chat:2.0.0` (already via `spring-ai-starter-model-ollama`).

### 6.2 Code Implementation

#### 6.2.1 Update AgentConfiguration

```java
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

// ...

.defaultAdvisors(
    new SimpleLoggerAdvisor(),
    MessageChatMemoryAdvisor.builder(chatMemory).build()
)
```

### 6.3 Verify

```bash
mvn spring-boot:run
# Type anything
# You'll see the full prompt and response logged to console via SimpleLoggerAdvisor + UserVisibleToolCallback [Tool] trace
```

### 6.4 Test Implementation

No new unit test – verified visually via logs and existing `ChatClientIntegrationTest` (`shouldGetResponseFromOllama` now logs via `SimpleLoggerAdvisor`) and `UserVisibleToolCallbackTest` trace. For MCP-logs see Step 10.

### 6.5 Further Reading

* Spring AI `Advisors` – `SimpleLoggerAdvisor` as `ChatClient` advisor, `MessageChatMemoryAdvisor` composition
* Spring AI `Observability` – `ChatModelObservationConvention` vs `SimpleLoggerAdvisor` for production

---

## Step 7: Packaging as Executable Jar

**Concept**: Spring Boot's Maven plugin packages your app as a self-contained executable jar. No servlet container needed — it runs as a CLI application.

### 7.1 Dependencies

No new dependencies – `spring-boot-maven-plugin:4.1.0` already in `pom.xml:59` `build.plugins`.

### 7.2 Code Implementation

#### 7.2.1 Build

```bash
mvn clean package -DskipTests
```

#### 7.2.2 Run

```bash
java -jar target/spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
```

The jar includes all dependencies and the Spring Boot loader. Anyone with Java 17+ can run it.

### 7.3 Test Implementation

No new test – `mvn package` already runs `mvn test` (skipped here via `-DskipTests` for speed); full suite verified in Step 8.6 (`46 tests` with `gemma4:e4b-mlx`).

### 7.4 Further Reading

* Spring Boot `Maven Plugin` – `spring-boot:run` vs `java -jar`, `repackage` goal
* GraalVM `native:compile` – `native-maven-plugin` for `spring-ai-cli-agent` (experimental, `polyglot` needs GraalPy)

---

## Step 8: Full Test Suite (Tests Were Introduced Per Chapter)

**Concept**: Testing was introduced at `Step 3.4` (`spring-boot-starter-test`) and each chapter added its own tests (`CalculatorToolTest` in `4.4`, `UnitConverterToolTest` in `5.4`, `AgentConfigurationTest`/`UserVisibleToolCallbackTest`/`CommandLineQuestionHandlerTest` in `3.4`). This step aggregates them – no new concept, just verification.

### 8.1 Dependencies

Already added in `Step 3.4`: `spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito). No new dependencies here.

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

### 8.5b Additional Tests (see repo)

Create `UserVisibleToolCallbackTest.java` (pure trace `passesArgumentsThroughUnchanged`), `ChatClientIntegrationTest.java` (real `gemma4:e4b-mlx` memory `shouldRememberContextAcrossTurns`), `ToolCallingEvalTest.java` (opt-in `gemma4:e4b-mlx` `calculatorPromptMustExecuteCalculatorTool` / `clarificationPromptMustExecuteAskUserQuestionTool` – asserts `[Tool] AskUserQuestionTool` trace + `CommandLineQuestionHandler` `System.in` mock), and `CommandLineQuestionHandlerTest.java` (4 tests for handler spec as in `3.4`).

### 8.6 Run Tests

From top level (`Duke42/`):

```bash
mvn test # 46 tests: 43 unit/integration + 3 evals skipped (add -Devals=true + Ollama gemma4:e4b-mlx for evals)
mvn test -Devals=true # same as above but runs ToolCallingEvalTest 3 with gemma4:e4b-mlx
mvn test -pl backend # 10 tests
```

No `-Dtest=...` needed – general setup covers all.

### 8.7 Further Reading

* JUnit 5 + AssertJ + Mockito – `mock(ChatModel.class)`, `ArgumentCaptor<Prompt>`, `verify(chatModel).call`
* `ChatClientIntegrationTest` vs `ToolCallingEvalTest` – mocked vs real `gemma4:e4b-mlx` with `ChatMemory.CONVERSATION_ID`
* `spring-boot-starter-test` – `test` scope, surefire `JUnitPlatformProvider`

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
│       ├── ChatLoop.java
│       ├── SlashCommand.java              # command pattern interface for /help, /tools, etc.
│       └── SlashCommandHandler.java       # registry for all slash commands
├── src/main/resources/
│   └── application.properties
└── src/test/java/com/example/cliai/
    ├── agent/
    │   ├── AgentConfigurationTest.java
    │   ├── UserVisibleToolCallbackTest.java
    │   ├── CommandLineQuestionHandlerTest.java  # verifies handler spec: header≤12, options 2-4, multiSelect, free-text
    │   └── tools/
    │       ├── CalculatorToolTest.java
    │       └── UnitConverterToolTest.java
    └── cli/
        ├── ChatLoopTest.java
        ├── ChatClientIntegrationTest.java   # needs Ollama gemma4:e4b-mlx (or lfm2.5)
        └── ToolCallingEvalTest.java         # -Devals=true, confirms handler was *used* via mocked System.in + trace
```

---

## Complete pom.xml

> Matches `spring-ai-cli-agent/pom.xml` – use this as final state if you followed Steps 1, 3.1, 8.1 incrementally:

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
        <version>4.1.0</version>
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
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springaicommunity</groupId>
            <artifactId>spring-ai-agent-utils</artifactId>
            <version>0.10.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
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

## Complete application.properties

> Checked-in default is `gemma4:e4b` (Linux/CI-friendly, `ollama pull gemma4:e4b`). For local Mac MLX use `application-local.properties` (see below) or `-Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx`.

```properties
# Local Ollama endpoint and chat model used by Spring AI.
# Checked-in default is gemma4:e4b (Linux/CI-friendly) – see ../../ollama-model-links.md
# For local Mac (MLX) use gemma4:e4b-mlx via application-local.properties (not committed) or -Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=gemma4:e4b
# Thinking: enable model thinking for gemma4/lfm2.5 (tools+thinking). Values: true/false/low/medium/high
# See https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning and #_reasoning_content_via_openai_compatibility
spring.ai.ollama.chat.think=medium
# MCP client is disabled by default so unit tests run without external dependencies.
spring.ai.mcp.client.enabled=false
spring.ai.mcp.client.sse.connections.polyglot.url=http://localhost:9000
server.port=8081
```

Create `src/main/resources/application-local.properties` for local Mac (not committed, `**/application-local.properties` in `.gitignore`):

```properties
# Local override for Mac (MLX) – run with --spring.profiles.active=local
spring.ai.ollama.chat.options.model=gemma4:e4b-mlx
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
        // and AskUserQuestionTool.md – tool implementation passed as .questionHandler(new CommandLineQuestionHandler()) to builder
        // Spec: https://code.claude.com/docs/en/agent-sdk/user-input#question-format (questions[]:{question,header,options{label,description},multiSelect})
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(new CommandLineQuestionHandler())
            .build();

        // Visibility embellishment for all tools (including QnA) – pure trace, no definition mutation or spec decoration
        ToolCallback[] allWithTrace = java.util.Arrays.stream(
                ToolCallbacks.from(askUserQuestionTool, new CalculatorTool(), new UnitConverterTool()))
            .map(UserVisibleToolCallback::new)
            .toArray(ToolCallback[]::new);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                Be helpful, concise. If you need information, a preference, confirmation, or disambiguation from the user, use an available tool to ask - never ask in ordinary assistant text. After receiving the tool result, continue with the response.
                """)
            .defaultToolCallbacks(allWithTrace)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );

        mcpProvider.ifAvailable(provider -> builder.defaultTools(provider));

        return builder.build();
    }
}
```

QnA is a separate first-class tool (`AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()` per blog/docs, spec `questions[]:{question,header,options{label,description},multiSelect}`); `UserVisibleToolCallback` is pure trace embellishment for all tools (including QnA) – no `if-else`, visible as `[Tool]`/`[Tool arguments]`/`[Tool result]` in manual `mvn spring-boot:run`.

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
        SlashCommandHandler slashHandler = new SlashCommandHandler();
        SlashCommand.Context slashContext = new SlashCommand.Context(sessionId);
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

                try {
                    System.out.print("\nThinking... ");
                    System.out.flush();
                    java.util.concurrent.atomic.AtomicBoolean firstContent = new java.util.concurrent.atomic.AtomicBoolean(true);
                    java.util.concurrent.atomic.AtomicBoolean thinkingPrinted = new java.util.concurrent.atomic.AtomicBoolean(false);
                    chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.get()))
                        .stream()
                        .chatResponse()
                        .doOnNext(cr -> {
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
                } catch (Exception e) {
                    System.out.println("\n[Error] " + e.getMessage() + "\n");
                }
            }
        }
    }
}
```

> **Slash commands via command pattern:** All `/help`, `/tools`, `/clear`, `/think`, `/exit` (and aliases `exit`/`quit`) are handled in `SlashCommand.java` (`interface SlashCommand {name(), description(), supports(), execute()}`) and `SlashCommandHandler.java` (registry `List<SlashCommand>` + `handle()`). `ChatLoop` only delegates to `slashHandler.handle(input, slashContext)` – no `if-else` chain in `ChatLoop`. See `src/main/java/com/example/cliai/cli/SlashCommand*.java`.

> **Thinking indicator:** `ChatLoop` shows `Thinking...` while waiting for first `chatResponse()` chunk and, if `spring.ai.ollama.chat.think=medium` (or `true`/`low`/`high`), prints `[Thinking] <content>` from `ChatResponse.getResult().getMetadata().get("thinking")` / `get("reasoningContent")` per [Ollama docs](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning) and `#_reasoning_content_via_openai_compatibility`. Enable via `application.properties` and `/think` shows help.

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

---

## Step 10: MCP Client (Model Context Protocol) – Connect to Polyglot

**Concept:** MCP is an open standard (like LSP for tools) – a client (our `spring-ai-cli-agent`/`backend`) discovers tools from an MCP server (`polyglot` on `9000`) via SSE. Unlike `AskUserQuestionTool` (agent-local QnA), MCP tools are remote, discovered at runtime, and require a `SyncMcpToolCallbackProvider`.

**Dependencies:** Add to `spring-ai-cli-agent/pom.xml` (already in `Complete pom.xml` – deferred from Step 3 per separate-section rule):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```
No `<version>` – managed by `spring-ai-bom:2.0.0`. `AskUserQuestionTool` (`agent-utils:0.10.0`) needs `<version>` because `org.springaicommunity` is not BOM-managed.

**Code Implementation – Wiring in `AgentConfiguration.java`:**

The final `AgentConfiguration.java` (see `## Complete AgentConfiguration.java`) already does:

```java
AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
    .questionHandler(new CommandLineQuestionHandler())
    .build();
ToolCallback[] allWithTrace = ToolCallbacks.from(askUserQuestionTool, new CalculatorTool(), new UnitConverterTool())
    .map(UserVisibleToolCallback::new).toArray(ToolCallback[]::new);
ChatClient.Builder builder = ChatClient.builder(chatModel)
    .defaultSystem("You are an interactive CLI assistant...")
    .defaultToolCallbacks(allWithTrace)
    .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build());
mcpProvider.ifAvailable(provider -> builder.defaultTools(provider)); // MCP tools added if server on 9000
return builder.build();
```
`application.properties` (both `spring-ai-cli-agent` and `backend`):

```properties
spring.ai.mcp.client.enabled=false # true to use polyglot
spring.ai.mcp.client.sse.connections.polyglot.url=http://localhost:9000
```

**Test Implementation:**

`backend/src/test/java/com/example/edge/McpIntegrationTest.java` – opt-in, needs `polyglot` running:

```bash
cd polyglot && mvn clean install && java -jar target/polyglot-runner.jar # 9000
cd backend && mvn test -Dmcp.integration=true
# or from top level: mvn test -Dmcp.integration=true -pl backend
```

Unit tests mock `ObjectProvider<SyncMcpToolCallbackProvider>` (`AgentConfigurationTest.java:31`) so `mvn test` passes without MCP.

**Verify (manual):**

1. Start polyglot:
```bash
cd polyglot
mvn clean install
java -jar target/polyglot-runner.jar
```
2. Enable MCP in `src/main/resources/application.properties`:
```properties
spring.ai.mcp.client.enabled=true
```
3. Restart `spring-ai-cli-agent` or `backend` – logs show `McpClient` connecting, `ToolCallingEvalTest` can now call `sentiment` tool.

**Further Reading:**

* MCP Spec `modelcontextprotocol.io/specification/2025-03-26/client/elicitation` (elicitation vs `AskUserQuestionTool` local QnA)
* Spring AI MCP `docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html` + `mcp-annotations-client` (`@McpElicitation`)
* `AskUserQuestionTool.md` `Important: AskUserQuestionTool can be used only with main Agents not sub-agents`

---

## Step 11: Streaming Responses

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

### Thinking Mode (Reasoning) – gemma4/lfm2.5 via `spring.ai.ollama.chat.think`

**Concept:** Thinking-capable models (`gemma4:e4b-mlx`, `qwen3:*-thinking`, `deepseek-r1` per [Ollama docs](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_thinking_mode_reasoning)) can emit `thinking` before the final answer. Native Ollama uses `thinking` metadata (`OllamaChatModel:THINKING_METADATA_KEY="thinking"`), OpenAI-compatibility uses `reasoningContent` (`#_reasoning_content_via_openai_compatibility`).

**Dependencies:** No new dependency – `spring-ai-starter-model-ollama:2.0.0` already includes `OllamaChatOptions` `ThinkOption` (`ThinkBoolean`/`ThinkLevel`) and `OllamaChatProperties` `think` (`TRUE/FALSE/LOW/MEDIUM/HIGH`).

**Code – `application.properties`:**

```properties
spring.ai.ollama.chat.think=medium  # or true/false/low/medium/high – see OllamaChatProperties:think / OllamaChatOptions.enableThinking()
# alternative: spring.ai.ollama.chat.options.think=medium
```

**Code – `ChatLoop.java` indicator + reasoning via `chatResponse()` (`ChatResponse.getResult().getMetadata().get("thinking")` or `"reasoningContent"`):**

```java
System.out.print("\nThinking... "); // indicator while waiting for first token
AtomicBoolean firstContent = new AtomicBoolean(true), thinkingPrinted = new AtomicBoolean(false);
chatClient.prompt().user(input).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.get()))
    .stream().chatResponse().doOnNext(cr -> {
        String thinking = (String) cr.getResult().getMetadata().get("thinking");
        if (thinking == null) thinking = (String) cr.getResult().getMetadata().get("reasoningContent");
        if (thinking != null && !thinking.isBlank()) {
            if (thinkingPrinted.compareAndSet(false, true)) System.out.print("\r");
            System.out.println("[Thinking] " + thinking);
        }
        String content = cr.getResult().getOutput().getText();
        if (content != null && !content.isBlank()) {
            if (firstContent.getAndSet(false) && !thinkingPrinted.get()) System.out.print("\r");
            System.out.print(content); // streaming content
        }
    }).blockLast();
System.out.println();
```

`/think` in CLI prints help and property docs.

**Test Implementation:** `CommandLineQuestionHandlerTest` still `4` unit tests; `ToolCallingEvalTest` with `gemma4:e4b-mlx` `think=medium` still asserts `[Tool] AskUserQuestionTool` trace – thinking does not break tool calling. Manual: `mvn spring-boot:run` → `You: Explain quantum entanglement` → `[Thinking] ...` then `AI: ...`.

**Further Reading:**

* Ollama `Thinking Mode` – `enableThinking()/disableThinking()/thinkLow()/thinkMedium()/thinkHigh()` and `response.getResult().getMetadata().get("thinking")` (`stream().chatResponse()` variant)
* Ollama `Reasoning Content via OpenAI Compatibility` – `reasoningContent` via `OpenAiChatModel` with `baseUrl=http://localhost:11434/v1` for same Ollama models

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
