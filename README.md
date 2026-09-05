# Duke42 🚀

*Duke42 – your Hitchhiker's Guide to the Java AI Galaxy*

Welcome aboard! I'm **Duke**, your trusty Java mascot and pilot, here to navigate you through the far reaches of the **Java + GenAI universe**.

---

## 🌌 Galaxy Overview

Duke42 has two sides: a **learning path** (Spring AI CLI Agent) and an **enterprise demo** (Vaadin Web UI + REST API).

### Primary: Spring AI CLI Agent

| Module | Mission | Key Features |
|--------|---------|--------------|
| **CLI Agent** (`spring-ai-cli-agent/`) | Learning project | CLI REPL with tools, memory, advisors |

**Start here →** See [TUTORIAL.md](TUTORIAL.md) for a step-by-step guide.

### Enterprise Demo

| Module | Mission | Key Features |
|--------|---------|--------------|
| **Backend** (`backend/`) | Enterprise demo | Vaadin Web UI + REST API + GraphQL + MCP client |

### Experimental

| Module | Mission | Status |
|--------|---------|--------|
| **Polyglot** (`polyglot/`) | MCP server | Quarkus + GraalPy sentiment analysis |
| **UI** (`ui/`) | Legacy desktop client | JavaFX — optional, not in default build |

---

## ⚡ Why Duke42?

- **Step-by-step learning**: Complete tutorial from zero to working agent
- **Local & private**: Ollama runs on your machine, no API keys
- **Enterprise patterns**: Spring AI, advisors, tool calling, memory
- **Web UI**: Vaadin chat interface for enterprise demos
- **REST API**: For JavaScript/React developers
- **GraphQL**: Flexible query layer for JS/React clients
- **Tested**: 82 tests (68 CLI + 14 backend)

---

## 🛠️ Getting Started

### Prerequisites

- Java 17+ (for Spring AI)
- Maven 3.6+
- Ollama (for local LLM)

### 📂 Repository Structure

```
Duke42/
├── spring-ai-cli-agent/     # ★ Start here — Learning project
├── backend/                 # Enterprise demo (Vaadin + REST + MCP)
├── polyglot/                # MCP server (experimental)
├── TUTORIAL.md              # ★ Step-by-step tutorial
├── BLUEPRINT-CLI-Agent.md   # Code reference
└── README.md
```

### Build & Run

**1. Clone the repo**

```bash
git clone git@github.com:vshanbha/Duke42.git
cd duke42
```

**2. Run the CLI Agent (recommended)**

```bash
cd spring-ai-cli-agent
mvn spring-boot:run
# Runs as a non-web Spring Shell app (no embedded server)
# Lands directly at the You: prompt (auto-enter chat); 'exit' returns to the
# agent> shell prompt, where chat re-enters and exit quits the app
```

**3. Run the Enterprise Backend**

```bash
cd backend
mvn spring-boot:run
# Runs on port 8080
# Open http://localhost:8080 for Vaadin Web UI
```

**4. Follow the tutorial**

```bash
# See TUTORIAL.md for a complete walkthrough
# Build from scratch in 8 steps with code + explanations
```

**5. Run tests**

```bash
# CLI Agent tests (68 tests; 3 evals need -Devals=true + Ollama,
# 2 Docker-gated: -Dtc.ollama=true / -Dtc.pgvector=true)
cd spring-ai-cli-agent && mvn test

# Backend unit + GraphQL tests only (14 tests)
cd backend && mvn test

# Backend full verification: unit tests, package, then E2E (requires Ollama)
cd backend && mvn clean verify
```

**6. Optional: RAG with pgvector** (BLUEPRINT Step 10)

```bash
docker compose up -d ollama pgvector
cd spring-ai-cli-agent && mvn spring-boot:run \
  -Dspring-boot.run.arguments="--rag.enabled=true --rag.ingest.on-startup=true"
# Then ask about anything in TUTORIAL.md – answers are grounded via QuestionAnswerAdvisor
```

---

## 📚 Learning Path

| Step | Topic | What You Learn |
|------|-------|----------------|
| 1 | Basic ChatClient | Spring AI fundamentals |
| 2 | Chat Memory | Advisors, conversation persistence |
| 3 | AskUserQuestionTool | Tool calling, user interaction |
| 4 | File System Tools | `FileSystemTools`, `GlobTool`, `GrepTool`, sandboxing |
| 5 | Multiple Tools + Chaining | AI tool selection, sequential tool calls |
| 6 | Logging Advisor | Debugging AI calls |
| 7 | Packaging | Executable jar |
| 8 | Unit Testing | JUnit 5, AssertJ, Mockito |
| 9 | Enterprise Backend (Optional) | Vaadin Web UI + REST API + GraphQL |
| 10 | Streaming Responses | Real-time token output in CLI and Vaadin |
| 11 | Structured Output | `BeanOutputConverter` JSON schema |
| 12 | Multimodality | Vision via `Media`, `/image` command |
| 13 | Model Switching | Per-call `ChatOptions`, `/model` + `/temp` commands |
| 14 | RAG + PgVector | ETL pipeline, `QuestionAnswerAdvisor`, docker-compose dev services |
| 15 | Observability | Micrometer, `/actuator/metrics` (`gen_ai.*`) |

**Full tutorial**: [TUTORIAL.md](TUTORIAL.md)

---

## 📚 References

### Spring AI

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tool-calling.html)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)

### Ollama

- [Ollama Website](https://ollama.com/) — Download and install
- [gemma4:e4b-mlx Model](https://ollama.com/library/gemma4) — Default (tools+thinking+vision+audio); CI pins `gemma4:e4b`
- Alternatives $<10$ GB with tools: [qwen3.5:9b](https://ollama.com/library/qwen3.5) (6.6 GB), [lfm2.5 Model](https://ollama.com/library/lfm2.5) (5.2 GB smaller) — see [ollama-model-links.md](ollama-model-links.md) (single source of truth)

### Vaadin

- [Vaadin Documentation](https://vaadin.com/docs) — Official reference
- [Vaadin Spring Boot](https://vaadin.com/docs/latest/spring/overview) — Spring Boot integration

### Experimental Modules

- [Quarkus Langchain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
- [GraalVM Polyglot](https://www.graalvm.org/reference-manual/polyglot/)

---

## 💡 Contributing

Duke42 thrives on curiosity and collaboration. Contributions welcome:
- New AI workflow demos
- Extended Edge / Polyglot capabilities
- UI improvements or visualizations
- Enterprise AI integration patterns

Fork, code, and submit pull requests—we'll navigate the galaxy together!

---

## 🪐 License

MIT License – explore, adapt, and share freely!
