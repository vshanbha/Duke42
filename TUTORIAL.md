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

> **Why registration alone is not enough:** Exposing a tool makes it *available*, not *preferred*. LLMs are pre-trained to clarify in plain text. Without an explicit tool-usage policy in the system prompt the model will often skip the tool and emit `What's your experience?` as assistant text — so `CommandLineQuestionHandler` never fires. The `defaultSystem` policy below forces every user-directed question through `AskUserQuestionTool` in a structured, handler-driven way. The JSON schema in the same prompt tells the model the exact shape (`{"questions":[{question,header,options:[{label,description}],multiSelect}]}`) that `CommandLineQuestionHandler` expects.

### 3.1 Add Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.10.0</version>
</dependency>
```

### 3.2 Update AgentConfiguration

Add a `defaultSystem` prompt that *requires* clarification via the tool, then register the tool:

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
                You must use AskUserQuestionTool for every question directed at the user.
                Never ask the user a question in ordinary assistant text. If you need
                information, a preference, confirmation, or disambiguation, stop and call
                AskUserQuestionTool first. After receiving the tool result, continue with
                the response.
                The tool input must be a JSON object with a questions array. Each question
                must contain question, header, options, and multiSelect. The questions field
                must always be an array, never a string. Each option must contain label and
                description. Example:
                {"questions":[{"question":"Which option do you prefer?","header":"Preference","options":[{"label":"Option A","description":"First choice"},{"label":"Option B","description":"Second choice"}],"multiSelect":false}]}
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

> **Note:** `defaultSystem` must come before `defaultTools`/`defaultAdvisors` in the builder chain for copy-paste parity with the final project. The real production code wraps the tool via `ToolCallbacks.from(...).map(UserVisibleToolCallback::new)` and uses `defaultToolCallbacks` for a visible trace and to normalize flat `{"options":…}` payloads into `{"questions":[...]}` — see `UserVisibleToolCallback.java`. The tutorial keeps `defaultTools` for simplicity.

### 3.3 Verify

```bash
mvn spring-boot:run
# Type: Help me learn Spring AI
# The AI should ask about your experience level, interests, etc. via AskUserQuestionTool
# Answer the questions — the AI will tailor its response
# If the model asks in plain text instead, check that defaultSystem is present (see Common Issues)
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

class UnitConverterTool {

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

Create `src/test/java/com/example/cliai/agent/AgentConfigurationTest.java`:

```java
package com.example.cliai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentConfigurationTest {

    @Test
    void shouldCreateChatClientBean() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);

        ChatClient chatClient = config.chatClient(chatModel);

        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateDistinctChatClientInstances() {
        AgentConfiguration config = new AgentConfiguration();
        ChatModel chatModel = mock(ChatModel.class);

        ChatClient client1 = config.chatClient(chatModel);
        ChatClient client2 = config.chatClient(chatModel);

        assertThat(client1).isNotSameAs(client2);
    }
}
```

### 8.5 ChatLoopTest

Create `src/test/java/com/example/cliai/cli/ChatLoopTest.java`:

```java
package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.io.ByteArrayInputStream;
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
            System.setIn(new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();
            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldExitOnQuitCommand() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("quit\n".getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();
            verify(chatClient, never()).prompt();
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldCallChatClientOnUserInput() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("AI response");

        ChatLoop chatLoop = new ChatLoop(chatClient);

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Hello\nexit\n".getBytes(StandardCharsets.UTF_8)));
            chatLoop.run();

            verify(chatClient).prompt();
            verify(requestSpec).user("Hello");
            verify(requestSpec).call();
            verify(responseSpec).content();
        } finally {
            System.setIn(originalIn);
        }
    }
}
```

### 8.6 Run Tests

```bash
mvn test
# 29 tests should pass
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
│   │   └── tools/
│   │       ├── CalculatorTool.java
│   │       └── UnitConverterTool.java
│   └── cli/
│       └── ChatLoop.java
├── src/main/resources/
│   └── application.properties
└── src/test/java/com/example/cliai/
    ├── agent/
    │   ├── AgentConfigurationTest.java
    │   └── tools/
    │       ├── CalculatorToolTest.java
    │       └── UnitConverterToolTest.java
    └── cli/
        └── ChatLoopTest.java
```

---

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
    ChatClient chatClient(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an interactive CLI assistant.
                You must use AskUserQuestionTool for every question directed at the user.
                Never ask the user a question in ordinary assistant text. If you need
                information, a preference, confirmation, or disambiguation, stop and call
                AskUserQuestionTool first. After receiving the tool result, continue with
                the response.
                The tool input must be a JSON object with a questions array. Each question
                must contain question, header, options, and multiSelect. The questions field
                must always be an array, never a string. Each option must contain label and
                description. Example:
                {"questions":[{"question":"Which option do you prefer?","header":"Preference","options":[{"label":"Option A","description":"First choice"},{"label":"Option B","description":"Second choice"}],"multiSelect":false}]}
                """)
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

---

## Complete ChatLoop.java

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

## Common Issues

| Issue | Fix |
|-------|-----|
| `Connection refused` on Ollama | Run `ollama serve` first |
| Tool calling doesn't work | Use a model with `tools` support — `lfm2.5` (default, 5.2 GB), `qwen3.5:9b` (6.6 GB) or `gemma4:e4b` (9.6 GB). See [`ollama-model-links.md`](ollama-model-links.md) for the full $<10$ GB comparison |
| Model asks clarifying question in plain text, never triggers `AskUserQuestionTool` | Tool registered but no system prompt policy — add `.defaultSystem("You must use AskUserQuestionTool for every question… Never ask…")` before `.defaultTools` (see Step 3) |
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
