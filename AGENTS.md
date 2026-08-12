# AGENTS.md – Duke42

## Quick Reference

### Build & Run Commands

```bash
# Build all modules (from root)
mvn clean install

# Run specific modules
cd spring-ai-cli-agent && mvn spring-boot:run  # CLI Agent (port 8081)
cd backend && mvn spring-boot:run               # Backend with Vaadin UI (port 8080)
cd polyglot && java -jar target/polyglot-runner.jar  # Polyglot MCP server (port 9000)
```

### Test Commands

```bash
# CLI Agent tests (31 tests)
cd spring-ai-cli-agent && mvn test

# Backend tests (1 test)
cd backend && mvn test

# Run all tests in a module
mvn test
```

## Module Structure

| Module | Purpose | Port | Key Notes |
|--------|---------|------|-----------|
| `spring-ai-cli-agent/` | CLI Agent learning project | 8081 | Terminal REPL, 31 tests |
| `backend/` | Enterprise demo (Vaadin + REST + MCP) | 8080 | Web UI + REST API |
| `polyglot/` | GraalVM Python integration (MCP server) | 9000 | Experimental |

## Critical Quirks

### MCP Integration

- Backend can use Polyglot module as MCP server on port 9000
- **MCP config is disabled by default** in `backend/src/main/resources/application.yaml`
- To enable MCP: set `spring.ai.mcp.client.enabled=true` in application.yaml
- **Unit tests run without MCP by default** - ensures tests work without external dependencies
- For integration tests with MCP: start polyglot module first (`java -jar target/polyglot-runner.jar`)

### GraalVM Polyglot Issues

- GraalPy has known issues with NumPy/TextBlob during build
- Currently experimental - may fail on some platforms
- Polyglot module needs to be built separately before backend MCP tests

### Test Dependencies

- Backend tests use `@SpringBootTest` with Mockito
- Each test generates unique `chatId` (UUID) for isolation
- Tests expect Ollama running locally with `lfm2.5` model
- Spring AI CLI Agent tests use JUnit 5 + AssertJ + Mockito (no Ollama required)

## Environment Requirements

- Java 17+ (for Spring Boot 4)
- Maven 3.6+
- GraalVM 21+ (for polyglot module only)
- Ollama CLI with model: `lfm2.5`

## Common Pitfalls

1. **Forgetting Ollama**: Backend tests will fail if Ollama isn't running
2. **MCP config disabled**: Integration tests with MCP tools won't work unless config is enabled
3. **Polyglot build order**: Build polyglot module before running backend with MCP
4. **Port conflicts**: CLI agent runs on 8081, backend on 8080
5. **Module isolation**: Each module has its own `pom.xml` - run commands from module directory

## Code Conventions

- Java 17+ features (records, sealed classes, pattern matching)
- Spring Boot dependency injection (CDI)
- JUnit 5 + AssertJ for testing
- Maven Surefire for test execution
- Vaadin for web UI (backend module only)

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
