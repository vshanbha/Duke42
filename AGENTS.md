# AGENTS.md – Duke42

## Quick Reference

### Build & Run Commands

```bash
# Build all modules (from root)
mvn clean install

# Run specific modules
cd backend && mvn quarkus:dev      # Backend (Quarkus dev mode)
cd polyglot && java -jar target/polyglot-runner.jar  # Polyglot runner
cd ui && mvn javafx:run            # JavaFX UI
cd spring-ai-cli-agent && mvn spring-boot:run  # Spring AI CLI Agent

# Native image (backend or polyglot)
cd backend && mvn package -Pnative && ./target/duke42-runner
```

### Test Commands

```bash
# Run all tests in a module
mvn test

# Run integration tests (requires Quarkus test profile)
mvn verify -Dnative  # Native integration tests
```

## Module Structure

| Module | Purpose | Key Notes |
|--------|---------|-----------|
| `backend/` | Quarkus REST API with LLM integration | Requires Ollama running locally (port 11434) |
| `polyglot/` | GraalVM Python integration (MCP server) | Runs on port 9000, experimental |
| `ui/` | JavaFX frontend | Uses AtlantaFX theme |
| `spring-ai-cli-agent/` | Spring AI CLI Agent learning project | New direction for WeShall.build |

## Critical Quirks

### MCP Integration

- Backend can use Polyglot module as MCP server on port 9000
- **MCP config is commented out by default** in `backend/src/main/resources/application.properties`
- To enable MCP: uncomment lines 28-29 in application.properties
- **Unit tests run without MCP by default** - ensures tests work without external dependencies
- For integration tests with MCP: start polyglot module first (`java -jar target/polyglot-runner.jar`)

### GraalVM Polyglot Issues

- GraalPy has known issues with NumPy/TextBlob during build
- Currently experimental - may fail on some platforms
- Polyglot module needs to be built separately before backend MCP tests

### Test Dependencies

- Backend tests use `@QuarkusTest` with REST-assured
- Each test generates unique `chatId` (UUID) for isolation
- Tests expect Ollama running locally with `llama3.2:1b` model
- Spring AI CLI Agent tests use JUnit 5 + AssertJ + Mockito (no Ollama required)

## Environment Requirements

- Java 21+ (LTS recommended)
- Maven 4+
- GraalVM 21+ (for native image and polyglot)
- Ollama CLI with models: `llama3.2:1b`, `smollm2`, `qwen3`
- Python environment (optional, for polyglot demos)

## Common Pitfalls

1. **Forgetting Ollama**: Backend tests will fail if Ollama isn't running
2. **MCP config disabled**: Integration tests with MCP tools won't work unless config is enabled
3. **Polyglot build order**: Build polyglot module before running backend with MCP
4. **Native image profile**: Use `-Pnative` flag, not just `mvn package`
5. **Module isolation**: Each module has its own `pom.xml` - run commands from module directory

## Code Conventions

- Java 21+ features (records, sealed classes, pattern matching)
- Quarkus dependency injection (CDI)
- REST-assured for API testing
- Maven Surefire/Failsafe for test execution
- `--add-opens java.base/java.lang=ALL-UNNAMED` required for test JVM args

## Software Factory Patterns (Adopted from softwareaifactory.sh)

### Verification Contract

- **WROTE**: "I wrote code intended to do X." No evidence. Cannot claim "verified"
- **RAN**: "I executed the check for X in this session; here is the output." Can claim "verified"
- **OBSERVED**: "I saw X happen at the level of the system the claim is about." Can claim "verified"

### Commit Message Conventions

- Use conventional commits: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `build`, `perf`
- No trailing period in subject
- Body max 6 bullets of max 25 words each
- Every "verified"/"fixed"/"works" claim must cite the exact command and paste its output

### Role-Based Development

- **Spec-writer**: Writes acceptance tests only, never implementation
- **Implementer**: Makes failing tests pass; cannot edit test files
- **Reviewer**: Adversarial, different model than implementer; output is findings for human

### Gate-Based Verification

- Every check must be seen to fail before passing: `break → FAIL → revert → PASS`
- Computational controls beat inferential ones (shell hooks > prose rules)
- Tests use JUnit 5 + AssertJ + Testcontainers (Java pack)