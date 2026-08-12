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
| **Backend** (`backend/`) | Enterprise demo | Vaadin Web UI + REST API + MCP client |

### Experimental

| Module | Mission | Status |
|--------|---------|--------|
| **Polyglot** (`polyglot/`) | MCP server | Quarkus + GraalPy sentiment analysis |

---

## ⚡ Why Duke42?

- **Step-by-step learning**: Complete tutorial from zero to working agent
- **Local & private**: Ollama runs on your machine, no API keys
- **Enterprise patterns**: Spring AI, advisors, tool calling, memory
- **Web UI**: Vaadin chat interface for enterprise demos
- **REST API**: For JavaScript/React developers
- **Tested**: 32 tests (31 unit + 1 integration)

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
# Runs on port 8081
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
# CLI Agent tests (31 tests)
cd spring-ai-cli-agent && mvn test

# Backend tests (1 test)
cd backend && mvn test
```

---

## 📚 Learning Path

| Step | Topic | What You Learn |
|------|-------|----------------|
| 1 | Basic ChatClient | Spring AI fundamentals |
| 2 | Chat Memory | Advisors, conversation persistence |
| 3 | AskUserQuestionTool | Tool calling, user interaction |
| 4 | Custom Tool (Calculator) | `@Tool`, SpEL expressions |
| 5 | Multiple Tools (Unit Converter) | AI tool selection |
| 6 | Logging Advisor | Debugging AI calls |
| 7 | Packaging | Executable jar |
| 8 | Unit Testing | JUnit 5, AssertJ, Mockito |
| 9 | Enterprise Backend (Optional) | Vaadin Web UI + REST API |

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
- [lfm2.5 Model](https://ollama.com/library/lfm2.5) — Primary model used in tutorial

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
