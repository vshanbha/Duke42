# Blueprint: Spring AI — Learning Path & Project

**Goal**: Build a general-purpose AI assistant that teaches Spring AI concepts step by step — now covering the full `docs.spring.io/spring-ai/reference/index.html` surface
**End products**: CLI agent (terminal, streaming + thinking) + Vaadin web UI (browser, streaming) + REST/GraphQL API (for JS) + MCP client (polyglot)
**Stack**: Spring Boot 4 + Spring AI 2.0 + Ollama (local, free, no API keys) + Vaadin 25 + PgVector/Chroma (VectorStore) + Micrometer (Observability)

> **Reference map:** This blueprint tracks `Spring AI Reference` top-level: `Chat Client API`, `Prompts`, `Structured Output`, `Multimodality`, `Models` (Chat/Embedding/Image/Audio/Moderation), `Chat Memory`, `Tool Calling`, `MCP`, `RAG`, `Vector Databases`, `Evaluation`, `Observability`, `Dev-time Services`, `Testing`.

> **✅ IMPLEMENTATION STATUS (2026-08-26):** All 15 steps implemented in `spring-ai-cli-agent` (+ backend RAG parity). Verified deviations from this document as written:
> - **Step 10 retrieval**: Spring AI 2.0.0 ships `QuestionAnswerAdvisor` (`spring-ai-vector-store-advisor`), not a `VectorStoreSimilarityRetriever`; ETL = `TextReader` → `TokenTextSplitter` → `PgVectorStore.add`. RAG is property-gated (`rag.enabled=true`) like MCP.
> - **Step 14**: There is no `ObservationAdvisor` class in Spring AI 2.0.0 – ChatModel observation is native Micrometer; `SimpleLoggerAdvisor` stays for trace. The CLI runs as a **non-web** Spring Shell app (`spring.main.web-application-type=none`), so metrics are exposed over **JMX** (`spring.jmx.enabled=true`, `management.endpoints.jmx.exposure.include=health,info,metrics`), not HTTP `/actuator/metrics`. (The backend still serves HTTP actuator on 8080.)
> - **Step 15 poms**: `spring-boot-starter-parent` does not manage Testcontainers versions – pinned via `testcontainers-ollama.version`; container tests are opt-in (`-Dtc.ollama=true`, `-Dtc.pgvector=true`) to keep default runs dependency-free.
> - **docker-compose.yml** is a superset: backend + polyglot app services plus ollama + pgvector dev-time services.

---

## PREREQUISITES

### What you need installed

| Tool | Version | Purpose |
|---|---|---|
| Java | 17+ | Runtime |
| Maven | 3.6+ | Build |
| Ollama | Latest | Local LLM runtime |
| Docker | Latest | For `pgvector` + `OllamaContainer` testcontainers |

### Ollama setup

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull gemma4:e4b        # default CLI, 9.6 GB, tools+thinking+vision+audio — or gemma4:e4b-mlx on Mac
ollama pull minicpm-v4.6      # small vision 1.6 GB, tools+vision, already downloaded — for edge vision
ollama pull lfm2.5            # alternative 5.2 GB, fastest — still in ollama-model-links.md
ollama run gemma4:e4b "Say hello"
```

### Why Ollama?

- **Free**: No API keys, no billing
- **Private**: Everything runs on your machine
- **Fast**: No network latency for local models
- **Tool calling + thinking**: `gemma4:e4b` (vision+audio) and `qwen3.5:9b` support `tools`+`thinking` — full comparison in [`ollama-model-links.md`](ollama-model-links.md)

---

## PROJECT STRUCTURE (expanded)

```
Duke42/
├── spring-ai-cli-agent/           # Learning project (CLI)
│   ├── pom.xml
│   ├── src/main/java/com/example/cliai/
│   │   ├── Application.java
│   │   ├── agent/
│   │   │   ├── AgentConfiguration.java      # ChatClient + memory + tools + MCP
│   │   │   ├── UserVisibleToolCallback.java # pure trace embellishment
│   │   │   └── tools/
│   │   │       ├── CalculatorTool.java
│   │   │       └── UnitConverterTool.java   # later: StructuredOutputConverter
│   │   ├── cli/
│   │   │   ├── ChatLoop.java                # streaming + thinking + SlashCommandHandler
│   │   │   ├── SlashCommand.java            # command pattern interface
│   │   │   └── SlashCommandHandler.java     # registry /help,/tools,/clear,/think,/exit
│   │   └── rag/
│   │       ├── RagConfiguration.java        # VectorStore + EmbeddingModel + ETL
│   │       └── IngestionService.java
│   └── src/test/java/             # 64 tests (59 run + 3 evals skipped + 2 Docker-gated opt-ins)
│
├── backend/                       # Enterprise demo (Vaadin + REST + GraphQL + MCP)
│   ├── pom.xml
│   ├── src/main/java/com/example/edge/
│   │   ├── Application.java
│   │   ├── EdgeConfiguration.java
│   │   ├── EdgeController.java
│   │   ├── ChatService.java       # chat + chatStream + ragChat
│   │   └── ui/
│   │       ├── ChatView.java      # Vaadin streaming + thinking indicator
│   │       └── ChatService.java
│   └── src/main/resources/
│       └── application.properties
│
├── polyglot/                      # MCP server (Quarkus, GraalPy)
│   └── src/main/java/com/example/
│       └── SentimentScoringResource.java
│
├── docker-compose.yml             # pgvector + ollama for dev-time services
│
└── BLUEPRINT-CLI-Agent.md         # This file
```

### Architecture (expanded)

```
┌──────────────────────────────────────────────────┐
│         Backend (port 8080) + CLI (non-web, Spring Shell)          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Vaadin   │  │ REST/    │  │ MCP      │       │
│  │  /chat   │  │ GraphQL  │  │ Client   │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       └─────────────┼─────────────┘             │
│                ┌────┴─────┐                     │
│                │ ChatClient│ ← Prompts (System/User, PromptTemplate, ChatOptions)
│                │ + Advisors│ ← Memory + SimpleLogger + ToolCallingAdvisor
│                │ + Tools   │ ← AskUserQuestion + Calculator + MCP tools
│                │ + RAG     │ ← ETL → VectorStore (PgVector) + EmbeddingModel
│                └────┬─────┘                     │
│                     │                           │
│              ┌──────┴──────┐  ┌──────────┐     │
│              │  Ollama     │  │Observability│  │
│              │ gemma4:think│  │ Micrometer │   │
│              └─────────────┘  └──────────┘     │
└──────────────────────────────────────────────────┘
```

---

## LEARNING PATH (15 STEPS) — each step: Concept → What you'll build → Key files → Verify

### STEP 1: Basic ChatClient (Chat Client API)

**Concept**: `ChatClient` is `JdbcTemplate` for LLMs — fluent `prompt().user().call().content()` vs `ChatModel.call(Prompt)`. `ChatClient` adds advisors, tools, memory.

**What you'll build**: CLI loop `You: → AI:` via `ChatClient` (no memory/tools).

**Key files**: `pom.xml` (`spring-ai-starter-model-ollama` via `spring-ai-bom:2.0.0`), `application.properties` (`spring.ai.ollama.base-url`, `spring.ai.ollama.chat.model=gemma4:e4b`), `Application.java`, `AgentConfiguration.java` (`ChatClient.builder(chatModel).build()`), `ChatLoop.java` (simple `Scanner` loop, later: `SlashCommandHandler`).

**Verify**: `mvn spring-boot:run` → `What is 2+2?`

### STEP 2: Prompts (System/User/Assistant, PromptTemplate, ChatOptions)

**Concept**: `Prompts` = `SystemMessage` (who AI is) + `UserMessage` (task) + `AssistantMessage` (history). `PromptTemplate` + `ChatOptions` (`temperature`, `topP`, `model`) override per-request.

**What you'll build**: Templated `You are a {role}...` system prompt + per-call `ChatOptions` (`temperature 0.7` vs `0.2`).

**Key files**: `AgentConfiguration.java` (`defaultSystem` with `PromptTemplate` placeholder), `ChatLoop.java` (`chatClient.prompt().system("You are a {role}").user("...").options(OllamaChatOptions.builder().temperature(0.7).build())`).

**Verify**: Change `role=travel assistant` vs `math tutor` and see style shift; `temperature 0.9` vs `0.2` diversity.

### STEP 3: Chat Memory (Chat Memory)

**Concept**: Advisors as AOP — `MessageChatMemoryAdvisor` injects history via `ChatMemory` (`MessageWindowChatMemory`) + `CONVERSATION_ID`.

**What you'll build**: `MessageWindowChatMemory(20)` + `sessionId = "session-"+UUID`.

**Verify**: `My name is Alice` → `What's my name?` → `Alice`.

### STEP 4: AskUserQuestionTool (Tool Calling – QnA)

**Concept**: Tools as stored procedures — AI decides via `@Tool(description)`. `AskUserQuestionTool` per `spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool` + `AskUserQuestionTool.md` + Claude spec `questions[]:{question,header≤12,options[2-4]{label,description},multiSelect}`.

**What you'll build**: `AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()` as separate first-class tool + tool-oblivious but directive `defaultSystem` (`use an available tool to ask - never ask...`).

**Verify**: `Help me learn Spring AI` → tool asks `Header: question` `1. label - desc`.

### STEP 5: Custom Tools (Tool Calling – @Tool)

**Concept**: `@Tool(description="...")` + `@ToolParam` → `ToolCallbacks.from(new CalculatorTool())`. `description` is selection hint.

**What you'll build**: `CalculatorTool` (`SpEL` `StandardEvaluationContext` `pi`).

**Verify**: `(15 * 7) + 23` → `128.0` via `[Tool] calculate`.

### STEP 6: Multiple Tools (Tool Calling – selection)

**Concept**: AI picks via `description` among `AskUserQuestion`, `Calculator`, `UnitConverter`; can chain.

**What you'll build**: `UnitConverterTool` (`km/miles` etc.) + `ToolCallbacks.from(...).map(UserVisibleToolCallback::new)` pure trace (no `if-else`).

**Verify**: `Convert 100 km to miles` → `UnitConverter`, `If I drive 100 km at 60 mph...` → both.

### STEP 7: Structured Output (Structured Output)

**Concept**: `Structured Output` — `BeanOutputConverter<T>` + JSON Schema (`@JsonProperty(required=true)`) vs free text. Ollama `format="json"` vs `outputSchema`.

**What you'll build**: `UnitConversion` record → `BeanOutputConverter<UnitConversion>.getJsonSchema()` → `OllamaChatOptions.builder().outputSchema(converter.getJsonSchema())` for `convert`.

**Verify**: `Convert 100 km` returns `{"value":62.14,"unit":"miles"}` validated.

### STEP 8: Multimodality (Multimodality, Models – Vision/Audio)

**Concept**: `Multimodality` — `UserMessage(Media(MimeType, Resource))` for vision+audio. Default `gemma4:e4b` (9.6 GB) does vision+audio, but for edge use `minicpm-v4.6:latest` (1.6 GB, already downloaded, `ollama list`).

**What you'll build**: `ChatLoop` `/image /tmp/pic.jpg What do you see?` → `new UserMessage("...", new Media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(path)))` via `minicpm-v4.6` or `gemma4:e4b`.

**Verify**: `ollama pull minicpm-v4.6` (1.6 GB) → image of bananas → `basket with bananas...` (small vision, `tools+vision`); `gemma4:e4b` also works but larger.

### STEP 9: Models – Chat/Embedding/Image/Audio/Moderation

**Concept**: `Models` — `ChatModel` (`OllamaChatModel`), `EmbeddingModel` (`OllamaEmbeddingModel`), plus `Image/Audio/Moderation` (OpenAI). `ChatModelsComparison`.

**What you'll build**: Switch `OllamaModel` (`gemma4:e4b` vs `lfm2.5`) via `ChatOptions`, `Moderation` filter before `ChatClient`.

**Verify**: `gemma4:e4b` vision works, `lfm2.5` fastest 1B active.

### STEP 10: RAG + Vector Databases (RAG, ETL, VectorStore)

**Concept**: `RAG` = `ETL` (`TextReader` → `TokenTextSplitter` → `EmbeddingModel` → `VectorStore`) + `Retrieval` (`VectorStoreSimilarityRetriever` as `Advisor`). `Vector Databases` — `PgVector`, `Chroma`, `Milvus` etc.

**What you'll build**: `docker-compose.yml` `pgvector:16`, `RagConfiguration.java` (`PgVectorStore`, `OllamaEmbeddingModel`), `IngestionService` (`TextReader` docs → `VectorStore.add`), `ChatClient` `defaultAdvisors(RagAdvisor)`.

**Verify**: Ingest `TUTORIAL.md` → `What does AskUserQuestionTool do?` → answer cites `questions[]` from `TUTORIAL.md`.

### STEP 11: MCP (Model Context Protocol)

**Concept**: `MCP` — client discovers remote tools via `SyncMcpToolCallbackProvider` (SSE `polyglot:9000`).

**What you'll build**: `spring-ai-starter-mcp-client` (no `<version>` via `spring-ai-bom`), `AgentConfiguration` `mcpProvider.ifAvailable(b -> b.defaultTools(provider))`, `application.properties` `spring.ai.mcp.client.enabled=false` / `sse.connections.polyglot.url`.

**Verify**: `polyglot` `java -jar polyglot-runner.jar` + `spring.ai.mcp.client.enabled=true` → `sentiment` tool via `ToolCallingEvalTest`.

### STEP 12: Streaming (ChatClient streaming)

**Concept**: `ChatClient` `.stream().content()` vs `.call().content()`; Vaadin `Flux<String>` + `UI.access()`.

**What you'll build**: `ChatLoop` `stream().chatResponse().doOnNext(cr -> {thinking = metadata.get("thinking")})` + thinking indicator.

**Verify**: `mvn spring-boot:run` tokens stream, `Vaadin` `ChatView` background thread.

### STEP 13: Thinking Mode (Models – Reasoning)

**Concept**: Ollama `Thinking Mode` (`OllamaChatOptions.enableThinking()`, `spring.ai.ollama.chat.think=medium`, metadata `thinking`/`reasoningContent` per `ollama-chat.html#_thinking_mode_reasoning` and `#_reasoning_content_via_openai_compatibility`).

**What you'll build**: `application.properties` `spring.ai.ollama.chat.think=medium`, `ChatLoop` `/think` + `Thinking...` indicator + `[Thinking] <content>` from `ChatResponse.getResult().getMetadata().get("thinking")`.

**Verify**: `Explain quantum entanglement` → `[Thinking] ...` then `AI: ...` with `gemma4:e4b-mlx`.

### STEP 14: Observability (Observability)

**Concept**: `Observability` — `Micrometer` `ChatModelObservationConvention` vs `SimpleLoggerAdvisor`. `Micrometer Tracing` + `Metrics`.

**What you'll build**: Replace `SimpleLoggerAdvisor` with `ObservationAdvisor` + `Micrometer` `MeterRegistry`, view `actuator/metrics`.

**Verify**: `curl localhost:8080/actuator/metrics` shows `gen_ai.client.token.usage`.

### STEP 15: Testing & Evaluation (Testing, Testcontainers, Evaluation)

**Concept**: `Testing` — `spring-ai-spring-boot-testcontainers` + `OllamaContainer` `@ServiceConnection` per `testcontainers.html`; `Evaluation` — `LLM-as-a-Judge` (`Testcontainers` `ollama` + `EvaluationRequest`).

**What you'll build**: `pom.xml` `spring-ai-spring-boot-testcontainers:2.0.0` + `testcontainers:ollama` (test scope, already added), `ChatClientIntegrationTest` with `OllamaContainer` `@ServiceConnection`, `ToolCallingEvalTest` (`-Devals=true`) as `LLM-as-a-Judge` (trace contains `[Tool] AskUserQuestionTool`).

**Verify**: `mvn test` (mocked, no Ollama) → 64 tests; `mvn test -Devals=true` (real `gemma4:e4b-mlx`) → 3 evals PASS; `mvn test -Dmcp.integration=true` (polyglot `9000`).

---

## FINAL PROJECT STATE

After all 15 steps, the project has:

| Feature | Concept Taught | Docs Reference |
|---|---|---|
| ChatClient + Ollama | Chat Client API, ChatModel auto-configuration | `chatclient.html` |
| Prompts | System/User/Assistant, PromptTemplate, ChatOptions | `prompt.html` |
| Chat memory | Advisors, ChatMemory | `chat-memory.html` |
| AskUserQuestionTool | Tool Calling (QnA), user interaction | `spring.io/blog/2026/01/16/...` + `AskUserQuestionTool.md` |
| Calculator/UnitConverter | Custom Tools, Tool selection | `tools.html` |
| Structured Output | BeanOutputConverter, JSON Schema | `structured-output.html` |
| Multimodality | Media, vision+audio | `multimodality.html` |
| Models | Chat/Embedding/Image/Audio/Moderation | `chat/ollama-chat.html`, `comparison.html` |
| RAG + PgVector | ETL, VectorStore, EmbeddingModel | `retrieval-augmented-generation.html`, `vectordbs/pgvector.html` |
| MCP | SyncMcpToolCallbackProvider | `mcp/mcp-overview.html` |
| Streaming | Flux<ChatResponse> | `chatclient.html#streaming` |
| Thinking | OllamaChatOptions.enableThinking(), reasoningContent | `ollama-chat.html#_thinking_mode_reasoning` |
| Observability | Micrometer | `observability/index.html` |
| Dev-time Services | Docker Compose, Testcontainers | `docker-compose.html`, `testcontainers.html` |
| Packaging + Tests | Executable jar, 64 tests + evals | `spring-boot:run`, `testcontainers.html`, `testing/evaluation` |

### `pom.xml` (complete, CLI)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
        https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version><relativePath/></parent>
    <groupId>com.example</groupId><artifactId>spring-ai-cli-agent</artifactId><version>0.0.1-SNAPSHOT</version>
    <properties><java.version>17</java.version><spring-ai.version>2.0.0</spring-ai.version></properties>
    <dependencyManagement><dependencies><dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-bom</artifactId><version>${spring-ai.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
    <dependencies>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-ollama</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-mcp-client</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-pgvector-store</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-vector-store-advisor</artifactId></dependency>
        <dependency><groupId>org.springaicommunity</groupId><artifactId>spring-ai-agent-utils</artifactId><version>0.10.0</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-spring-boot-testcontainers</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>ollama</artifactId><version>${testcontainers-ollama.version}</version><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><version>${testcontainers-ollama.version}</version><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><version>${testcontainers-ollama.version}</version><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
```

### `application.properties` (complete, CLI)

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma4:e4b
spring.ai.ollama.chat.think=medium
spring.ai.mcp.client.enabled=false
spring.ai.mcp.client.sse.connections.polyglot.url=http://localhost:9000
spring.jmx.enabled=true
management.endpoints.jmx.exposure.include=health,info,metrics
rag.enabled=false
rag.datasource.url=jdbc:postgresql://localhost:5432/vector
rag.vectorstore.dimensions=768
```

### `docker-compose.yml` (dev-time services)

The repo compose file additionally carries the backend + polyglot app services; the dev-time services per this blueprint:

```yaml
services:
  ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    volumes: ["ollama-models:/root/.ollama"]
  pgvector:
    image: pgvector/pgvector:pg16
    ports: ["5432:5432"]
    environment: [POSTGRES_DB=vector, POSTGRES_USER=postgres, POSTGRES_PASSWORD=postgres]
    volumes: ["pgvector-data:/var/lib/postgresql/data"]
volumes:
  ollama-models:
  pgvector-data:
```

---

## HOW TO USE THIS BLUEPRINT

### For learning (step by step)

1. Create project from scratch, one step at a time
2. After each step, `mvn spring-boot:run` and verify
3. Read Spring AI docs for the concept you just added
4. Experiment — change system prompt, add parameters, break things

### For the Baeldung article

The 15 steps map to sections:
- Steps 1-3 → "Project Setup" + "Building the Agent" (`ChatClient`, `Prompts`, `Memory`)
- Steps 4-6 → "Custom Tools" (`AskUserQuestion`, `Calculator`, `UnitConverter`)
- Steps 7-10 → "Advanced" (`Structured Output`, `Multimodality`, `Models`, `RAG`)
- Steps 11-15 → "Production" (`MCP`, `Streaming`, `Thinking`, `Observability`, `Testing`)

### For a downloadable tool

After Step 15, you have a self-contained jar + `docker-compose.yml` for `pgvector`:
```bash
java -jar spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
docker compose up pgvector ollama
```

---

## COMMON ISSUES

| Issue | Fix |
|---|---|
| `Connection refused` on Ollama | `ollama serve` or `docker compose up ollama` or Testcontainers `OllamaContainer` |
| Tool calling doesn't work | Use `tools` model — `gemma4:e4b` (9.6 GB), `qwen3.5:9b` (6.6 GB), `lfm2.5` (5.2 GB) — see `ollama-model-links.md` |
| Model asks in plain text | QnA not separate / nudge missing – `AskUserQuestionTool.builder().questionHandler(new CommandLineQuestionHandler()).build()` + `defaultSystem` `use an available tool to ask - never ask...` |
| Slow first response | Ollama loads model into RAM first call; next calls fast |
| `OutOfMemoryError` | Use smaller model: `ollama pull gemma4:e4b` vs `lfm2.5-thinking` 731MB |
| `pgvector` not found | `docker compose up pgvector` or `spring-boot-docker-compose` |
| Build fails on native image | Skip native, use `java -jar` |

---

## NEXT STEPS (post-tutorial)

After completing this project, consider:
- **Subagent orchestration** — `spring-ai-agent-utils` `TaskTool` (hierarchical agents)
- **A2A Integration** — `Agent2Agent` protocol
- **RAG** already done — add `Evaluation` (LLM-as-a-Judge) for RAG quality
- **Streaming** already done — add `Thinking` levels `low/medium/high` for `gpt-oss`
- **Multi-model** — route tasks to different models via `ChatOptions`

---

## BACKEND MODULE (Enterprise Demo)

See `TUTORIAL.md` Step 10/14 for Vaadin + REST/GraphQL + MCP details.

## References (Spring AI Reference index)

- `Chat Client API` – `chatclient.html`
- `Prompts` – `prompt.html`
- `Structured Output` – `structured-output.html`
- `Multimodality` – `multimodality.html`
- `Models` – `chat/ollama-chat.html#_thinking_mode_reasoning`, `chat/ollama-chat.html#_reasoning_content_via_openai_compatibility`, `comparison.html`
- `Chat Memory` – `chat-memory.html`
- `Tool Calling` – `tools.html`
- `MCP` – `mcp/mcp-overview.html`
- `RAG` – `retrieval-augmented-generation.html` + `etl-pipeline.html`
- `Vector Databases` – `vectordbs/pgvector.html`
- `Evaluation` – `testing.html`
- `Observability` – `observability/index.html`
- `Dev-time Services` – `docker-compose.html`
- `Testing` – `testcontainers.html` + `testing.html`
