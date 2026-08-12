# Duke42 🚀

*Duke42 – your Hitchhiker's Guide to the Java AI Galaxy*

Welcome aboard! I'm **Duke**, your trusty Java mascot and pilot, here to navigate you through the far reaches of the **Java + GenAI universe**.

---

## 🌌 Galaxy Overview

Duke42 has two sides: a **learning path** (Spring AI CLI Agent) and **experimental modules** (Quarkus, GraalVM, MCP).

### Primary: Spring AI CLI Agent

| Module | Mission | Key Features |
|--------|---------|--------------|
| **Spring AI** | Enterprise AI integration | CLI Agent with tools, memory, advisors, Ollama |

**Start here →** See [TUTORIAL.md](TUTORIAL.md) for a step-by-step guide.

### Experimental: Legacy Modules

These modules explore different parts of the Java GenAI ecosystem. They work but are not the primary focus:

| Module | Mission | Status |
|--------|---------|--------|
| **Edge** (`backend/`) | Local LLM inference | Quarkus + LangChain4j |
| **Polyglot** (`polyglot/`) | Java ↔ Python pipelines | GraalVM polyglot (experimental) |
| **UI** (`ui/`) | JavaFX frontend | Interactive Duke guide |

---

## ⚡ Why Duke42?

- **Step-by-step learning**: Complete tutorial from zero to working agent
- **Local & private**: Ollama runs on your machine, no API keys
- **Enterprise patterns**: Spring AI, advisors, tool calling, memory
- **Tested**: 31 tests (29 unit + 2 integration)
- **Modular**: Explore Spring AI or experimental modules independently

---

## 🛠️ Getting Started

### Prerequisites

- Java 17+ (for Spring AI CLI Agent)
- Maven 3.6+
- Ollama (for local LLM)

### 📂 Repository Structure

```
Duke42/
├── spring-ai-cli-agent/     # ★ Start here — Spring AI CLI Agent
├── backend/                 # Quarkus backend (experimental)
├── polyglot/                # GraalVM polyglot (experimental)
├── ui/                      # JavaFX frontend (experimental)
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

**2. Run the Spring AI CLI Agent (recommended)**

```bash
cd spring-ai-cli-agent
mvn spring-boot:run
```

**3. Follow the tutorial**

```bash
# See TUTORIAL.md for a complete walkthrough
# Build from scratch in 8 steps with code + explanations
```

**4. Run tests**

```bash
cd spring-ai-cli-agent
mvn test
# 31 tests: CalculatorTool, UnitConverterTool, AgentConfiguration, ChatLoop, Integration
```

### Experimental Modules

These require additional setup (Quarkus, GraalVM):

```bash
# Polyglot (GraalVM Python)
cd polyglot && mvn clean install && java -jar target/polyglot-runner.jar

# Backend (Quarkus + LangChain4j)
cd backend && mvn quarkus:dev

# UI (JavaFX)
cd ui && mvn javafx:run
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

**Full tutorial**: [TUTORIAL.md](TUTORIAL.md)

---

## 📚 References

### Spring AI

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tool-calling.html)

### Ollama

- [Ollama Website](https://ollama.com/) — Download and install
- [lfm2.5 Model](https://ollama.com/library/lfm2.5) — Primary model used in tutorial

### Experimental Modules

- [Quarkus Langchain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
- [Langchain4j](https://docs.langchain4j.dev/)
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
