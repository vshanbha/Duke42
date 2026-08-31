# Duke42 Tutorial Roadmap: Enterprise AI Integration with Spring AI

**Goal**: Build a complete AI agent in Java using Spring AI, ready for enterprise deployment  
**Duration**: 7-10 days (1-2 hours per workshop module)  
**End product**: A downloadable executable jar — run it, chat with an AI in your terminal  
**Stack**: Spring Boot 4 + Spring AI 2.0 + Ollama (local, free, no API keys)

---

## Day 1-2: Foundation (Baeldung Article Focus)

### Workshop 1: Basic ChatClient (2 hours)
**Concept**: ChatClient is to LLMs what JdbcTemplate is to databases — a fluent API that sends prompts and gets responses.

**Learning Outcomes**:
- Set up Spring Boot 4 with Spring AI 2.0
- Configure Ollama as local LLM provider
- Create interactive CLI loop with ChatClient
- Understand ChatModel auto-configuration

**Key Files**:
- `pom.xml` with Spring AI BOM
- `application.properties` with Ollama config
- `Application.java` entry point
- `AgentConfiguration.java` with ChatClient bean
- `ChatLoop.java` interactive REPL

**Verification**:
```bash
cd spring-ai-cli-agent
mvn spring-boot:run
# Type: What is 2+2?
# Should get response from LLM
```

### Workshop 2: Chat Memory (2 hours)
**Concept**: Advisors are like AOP aspects — they wrap AI calls with cross-cutting behavior. `MessageChatMemoryAdvisor` adds conversation history.

**Learning Outcomes**:
- Implement conversation memory with Advisors
- Use MessageWindowChatMemory for session persistence
- Track conversation IDs for multi-session support
- Understand advisor composition

**Key Changes**:
- Add ChatMemory bean to AgentConfiguration
- Configure MessageChatMemoryAdvisor
- Update ChatLoop with conversation ID

**Verification**:
```bash
mvn spring-boot:run
# Type: My name is Alice
# Type: What's my name?
# AI should remember "Alice"
```

---

## Day 3-4: Tool Integration (Baeldung Article Focus)

### Workshop 3: AskUserQuestionTool (2 hours)
**Concept**: Tools are like stored procedures — plugins the AI can invoke when it needs external data or user input. The AI *decides* when to call them.

**Learning Outcomes**:
- Integrate AskUserQuestionTool for interactive Q&A
- Use CommandLineQuestionHandler for CLI interaction
- Understand tool calling flow
- Implement dynamic user input collection

**Key Changes**:
- Add spring-ai-agent-utils dependency
- Configure AskUserQuestionTool in AgentConfiguration
- Test interactive questioning flow

**Verification**:
```bash
mvn spring-boot:run
# Type: Help me learn Spring AI
# AI should ask about experience level, interests, etc.
# Answer questions — AI will tailor response
```

### Workshop 4: File System Tools (2 hours)
**Concept**: Use pre-built tools from `spring-ai-agent-utils` for file system operations. The AI decides when to call based on the tool's description.

**Learning Outcomes**:
- Understand FileSystemTools (read/write/edit with sandboxing)
- Understand GlobTool (find files by pattern)
- Understand GrepTool (search file contents by regex)
- Register tools in ChatClient

**Key Files**:
- `FileSystemTools.java`, `GlobTool.java`, `GrepTool.java` from spring-ai-agent-utils
- Updated AgentConfiguration with tool registration

**Verification**:
```bash
mvn spring-boot:run
# Type: Find all Java files in this project
# AI should call GlobTool
# Type: Search for @Tool annotations in the codebase
# AI should call GrepTool
```

---

## Day 5-6: Advanced Tooling

### Workshop 5: Multiple Tools + Tool Calling Flow (2 hours)
**Concept**: When multiple tools are registered, the AI picks the right one based on the user's request. It can even call multiple tools in sequence.

**Learning Outcomes**:
- Use FileSystemTools for read/write/edit operations
- Observe AI choosing between tools
- Test sequential tool calling
- Understand tool selection logic

**Key Files**:
- FileSystemTools, GlobTool, GrepTool from spring-ai-agent-utils
- Updated AgentConfiguration with multiple tools

**Verification**:
```bash
mvn spring-boot:run
# Type: Read the pom.xml file
# AI calls FileSystemTools.read()
# Type: Find all test files
# AI calls GlobTool
# Type: Search for "ChatClient" in all Java files and show the results
# AI calls GrepTool
```

### Workshop 6: Advisors — Logging (2 hours)
**Concept**: Advisors wrap around every AI call. A logging advisor shows you what's being sent to the model and what comes back — invaluable for debugging.

**Learning Outcomes**:
- Implement SimpleLoggerAdvisor for debugging
- Understand advisor composition order
- Log prompts and responses
- Debug AI interactions

**Key Changes**:
- Add SimpleLoggerAdvisor to advisor chain
- Test logging output
- Understand advisor precedence

**Verification**:
```bash
mvn spring-boot:run
# Type anything
# See full prompt and response logged to console
```

---

## Day 7-8: Enterprise Patterns

### Workshop 7: Packaging as Executable Jar (2 hours)
**Concept**: Spring Boot's Maven plugin packages your app as a self-contained executable jar. No servlet container needed — it runs as a CLI application.

**Learning Outcomes**:
- Configure spring-boot-maven-plugin
- Build executable jar
- Run standalone application
- Optional: GraalVM native image

**Key Changes**:
- Add build plugin to pom.xml
- Build and test executable jar
- Optional: Native image compilation

**Verification**:
```bash
# Build executable jar
mvn clean package -DskipTests

# Run it
java -jar target/spring-ai-cli-agent-0.0.1-SNAPSHOT.jar

# Test all features
```

### Workshop 8: Unit Testing (2 hours)
**Concept**: Write unit tests for your tools and components using JUnit 5, AssertJ, and Mockito. No external dependencies required.

**Learning Outcomes**:
- Write unit tests for `@Tool` methods
- Test component wiring with mocked dependencies
- Use AssertJ for fluent assertions
- Verify CLI loop behavior with Mockito

**Key Files**:
- `CalculatorToolTest.java` — 12 tests for math expressions
- `UnitConverterToolTest.java` — 12 tests for all conversion paths
- `AgentConfigurationTest.java` — 2 tests for bean wiring
- `ChatLoopTest.java` — 3 tests for REPL logic

**Verification**:
```bash
mvn test
# 29 tests should pass
```

### Workshop 9: Enterprise Integration Patterns (2 hours)
**Concept**: Connect your AI agent to enterprise systems using Spring's integration patterns.

**Learning Outcomes**:
- Connect to external APIs via tools
- Implement authentication patterns
- Add error handling and retry logic
- Understand production readiness

**Key Changes**:
- Add HTTP client tools
- Implement retry patterns
- Add proper error handling

**Verification**:
```bash
mvn spring-boot:run
# Test external API integration
# Verify error handling
# Check logging output
```

---

## Day 9-10: Production Readiness

### Workshop 10: Monitoring and Observability (2 hours)
**Concept**: Add monitoring, metrics, and tracing to your AI agent for production deployment.

**Learning Outcomes**:
- Add Spring Boot Actuator
- Implement custom metrics
- Add distributed tracing
- Monitor AI interactions

**Key Changes**:
- Add actuator dependencies
- Configure health checks
- Add custom metrics

**Verification**:
```bash
mvn spring-boot:run
# Check /actuator/health
# Verify metrics endpoint
# Test tracing
```

### Workshop 11: Final Project Review (2 hours)
**Concept**: Review all concepts, optimize performance, and prepare for deployment.

**Learning Outcomes**:
- Review all Spring AI concepts
- Optimize tool calling performance
- Prepare for production deployment
- Document architecture decisions

**Key Activities**:
- Code review and optimization
- Performance testing
- Documentation updates
- Final verification

---

## Baeldung Article Structure

The 7-10 day tutorial maps to sections in the Baeldung article:

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

## Software Factory Integration

Adopted patterns from softwareaifactory.sh:

### Verification Contract
- **WROTE**: "I wrote code intended to do X." No evidence
- **RAN**: "I executed the check for X in this session; here is the output."
- **OBSERVED**: "I saw X happen at the level of the system the claim is about."

### Commit Message Conventions
- Conventional commits: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`
- Evidence-based claims with command output
- No trailing period in subject

### Role-Based Development
- **Spec-writer**: Writes acceptance tests only
- **Implementer**: Makes failing tests pass; cannot edit test files
- **Reviewer**: Adversarial, different model; output is findings

### Gate-Based Verification
- Every check must be seen to fail before passing
- Computational controls beat inferential ones
- Tests use JUnit 5 + AssertJ + Testcontainers

---

## Environment Requirements

- Java 17+ (Spring AI 2.0 requirement)
- Maven 3.6+
- Ollama CLI with model `lfm2.5` (default, 5.2 GB) — alternatives `<10$ GB` with tools: `qwen3.5:9b` (6.6 GB), `gemma4:e4b` (9.6 GB). Full comparison in [`ollama-model-links.md`](ollama-model-links.md) (single source of truth)
- GraalVM 21+ (optional, for native image)

## Common Pitfalls

1. **Ollama not running**: `Connection refused` on localhost:11434
2. **Wrong model**: Tool calling requires a model with `tools` support — `lfm2.5` (default), `qwen3.5:9b` or `gemma4:e4b` (see `ollama-model-links.md`)
3. **Slow first response**: Ollama loads model into RAM on first call
4. **OutOfMemoryError**: Use smaller model like `lfm2.5`
5. **Build fails**: Skip native image for now, use `java -jar`

---

## Next Steps

After completing the tutorial:
- **Subagent orchestration** — delegate tasks to specialized agents
- **RAG** — add document retrieval with vector stores
- **MCP** — connect to external tool servers
- **Streaming** — use `.stream()` instead of `.call()` for real-time output
- **Multi-model** — route different tasks to different models