# Duke42: Enterprise AI Integration with Spring AI

**Working home**: WeShall.build  
**Positioning**: Practical AI integration for enterprise Java systems  
**Focus**: Architecture, enterprise software, regulated industries, Java ecosystems, production readiness

---

## What We Built

### Repository Transformation
- **Removed**: AnomalyDetector module (Python anomaly detection - didn't work properly)
- **Kept**: Polyglot module (GraalVM Python integration - experimental but valuable)
- **Added**: Spring AI CLI Agent module (new direction for enterprise AI integration)
- **Updated**: README, AGENTS.md, root pom.xml for new structure

### New Module: Spring AI CLI Agent
A complete learning project that teaches Spring AI concepts step by step:

**Technology Stack**:
- Spring Boot 4.0.0
- Spring AI 2.0.0
- Ollama (local LLM runtime)
- Java 17+

**Key Features**:
1. ChatClient with Ollama integration
2. Chat memory with Advisors
3. AskUserQuestionTool for interactive Q&A
4. File system tools (FileSystemTools, GlobTool, GrepTool)
5. Multiple tool calling flow
6. SimpleLoggerAdvisor for debugging
7. Executable jar packaging
8. Unit tests (49 tests: FileSystemTools, GlobTool, GrepTool, AgentConfiguration, ChatLoop)

---

## Software Factory Integration

Adopted patterns from softwareaifactory.sh for enterprise-grade development:

### Verification Contract
- **WROTE**: "I wrote code intended to do X." No evidence
- **RAN**: "I executed the check for X in this session; here is the output."
- **OBSERVED**: "I saw X happen at the level of the system the claim is about."

### Role-Based Development
- **Spec-writer**: Writes acceptance tests only, never implementation
- **Implementer**: Makes failing tests pass; cannot edit test files
- **Reviewer**: Adversarial, different model than implementer; output is findings for human

### Gate-Based Verification
- Every check must be seen to fail before passing: `break → FAIL → revert → PASS`
- Computational controls beat inferential ones (shell hooks > prose rules)
- Tests use JUnit 5 + AssertJ + Testcontainers (Java pack)

### Commit Message Conventions
- Conventional commits: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`
- Evidence-based claims with command output
- No trailing period in subject
- Body max 6 bullets of max 25 words each

---

## Tutorial Roadmap (7-10 Days)

### Day 1-2: Foundation
- Workshop 1: Basic ChatClient (2 hours)
- Workshop 2: Chat Memory (2 hours)

### Day 3-4: Tool Integration
- Workshop 3: AskUserQuestionTool (2 hours)
- Workshop 4: Custom Tool (2 hours)

### Day 5-6: Advanced Tooling
- Workshop 5: Multiple Tools + Tool Calling Flow (2 hours)
- Workshop 6: Advisors — Logging (2 hours)

### Day 7-8: Enterprise Patterns
- Workshop 7: Packaging as Executable Jar (2 hours)
- Workshop 8: Unit Testing (2 hours)
- Workshop 9: Enterprise Integration Patterns (2 hours)

### Day 9-10: Production Readiness
- Workshop 10: Monitoring and Observability (2 hours)
- Workshop 11: Final Project Review (2 hours)

---

## Baeldung Article Structure

The tutorial maps to sections in the Baeldung article:

1. **Introduction** - Enterprise AI integration with Spring AI
2. **Project Setup** - Spring Boot 4 + Spring AI 2.0 + Ollama
3. **Building the Agent** - ChatClient, memory, advisors
4. **Tool Integration** - AskUserQuestionTool, custom tools
5. **Advanced Tooling** - Multiple tools, tool calling flow
6. **Advisors** - Logging, composition, debugging
7. **Packaging** - Executable jar, optional native image
8. **Testing** - Unit tests with JUnit 5, AssertJ, Mockito
9. **Enterprise Patterns** - API integration, error handling
10. **Production Readiness** - Monitoring, metrics, deployment
11. **Conclusion** - Next steps and resources

---

## Quick Start

### Prerequisites
- Java 17+ (Spring AI 2.0 requirement)
- Maven 3.6+
- Ollama CLI with models: `lfm2.5`, `qwen2.5`, `llama3.1`, `mistral`

### Run the Agent
```bash
# Clone the repository
git clone git@github.com:vshanbha/Duke42.git
cd duke42

# Build all modules
mvn clean install

# Run the Spring AI CLI Agent
cd spring-ai-cli-agent
mvn spring-boot:run

# Test it
# Type: What is 2+2?
# Type: My name is Alice
# Type: What's my name?
# Type: Convert 100 km to miles
```

### Build Executable Jar
```bash
cd spring-ai-cli-agent
mvn clean package -DskipTests
java -jar target/spring-ai-cli-agent-0.0.1-SNAPSHOT.jar
```

### Run Tests
```bash
cd spring-ai-cli-agent
mvn test
# 49 tests covering FileSystemTools, GlobTool, GrepTool, AgentConfiguration, ChatLoop
```

---

## Environment Requirements

- Java 21+ (LTS recommended)
- Maven 4+
- GraalVM 21+ (for native image and polyglot)
- Ollama CLI with models: `llama3.2:1b`, `smollm2`, `qwen3`
- Python environment (optional, for polyglot demos)

---

## Key Files

- `README.md` - Repository overview and quick start
- `AGENTS.md` - Agent instructions and conventions
- `TUTORIAL.md` - Step-by-step tutorial (start here)
- `TUTORIAL-ROADMAP.md` - Workshop plan (11 workshops)
- `BLUEPRINT-CLI-Agent.md` - Code reference for Spring AI CLI Agent
- `ollama-model-links.md` - Ollama model comparison and selection guide
- `spring-ai-cli-agent/` - Complete Spring AI CLI Agent project

---

## Next Steps

After completing the tutorial:
- **Subagent orchestration** — delegate tasks to specialized agents
- **RAG** — add document retrieval with vector stores
- **MCP** — connect to external tool servers
- **Streaming** — use `.stream()` instead of `.call()` for real-time output
- **Multi-model** — route different tasks to different models

---

## Contributing

Duke42 thrives on curiosity and collaboration. Contributions welcome:
- New AI workflow demos
- Extended Edge / Polyglot capabilities
- UI improvements or visualizations
- Enterprise AI integration patterns

Fork, code, and submit pull requests—we'll navigate the galaxy together!

---

## License

MIT License – explore, adapt, and share freely!