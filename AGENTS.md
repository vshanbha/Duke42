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

From top level (`Duke42/`):

```bash
mvn test # all modules: spring-ai-cli-agent 64 + backend 14
mvn test -pl spring-ai-cli-agent -am # only CLI agent
mvn test -pl backend -am # only backend
```

From module:

```bash
# CLI Agent: 64 tests (59 run + 3 evals skipped without -Devals=true + Ollama,
# plus 2 Docker-gated opt-ins: -Dtc.ollama=true, -Dtc.pgvector=true)
cd spring-ai-cli-agent && mvn test
# opt-in evals with Ollama gemma4:e4b (or gemma4:e4b-mlx via -Dspring.ai.ollama.chat.options.model)
cd spring-ai-cli-agent && mvn test -Devals=true

# Backend: unit + GraphQL tests only (14 tests; no Ollama required)
cd backend && mvn test
# Backend full verification: unit tests, package, then E2E via failsafe (4 e2e; requires Ollama)
cd backend && mvn clean verify
# MCP integration (requires polyglot on port 9000) – no -Dtest needed
cd backend && mvn test -Dmcp.integration=true # or mvn test -Dmcp.integration=true -pl backend from top level
```

## Module Structure

| Module | Purpose | Port | Tests |
|--------|---------|------|-------|
| `spring-ai-cli-agent/` | CLI Agent learning project | 8081 | 64 (3 evals + 2 Docker-gated opt-in) |
| `backend/` | Enterprise demo (Vaadin + REST + GraphQL + MCP) | 8080 | 14 (10 unit + 4 e2e via failsafe) |
| `polyglot/` | GraalVM Python integration (MCP server) | 9000 | 3 (integration) |
| `ui/` | Legacy JavaFX desktop client (optional, not in parent build) | — | — |

## Critical Quirks

### MCP Integration

- Backend can use Polyglot module as MCP server on port 9000
- **MCP config is disabled by default** in `backend/src/main/resources/application.properties`
- To enable MCP: set `spring.ai.mcp.client.enabled=true` in application.properties
- **Unit tests run without MCP by default** - ensures tests work without external dependencies
- For integration tests with MCP: start polyglot module first, then run with `-Dmcp.integration=true`
- CI installs Ollama and pulls `lfm2.5` for e2e tests

### GraalVM Polyglot Issues

- GraalPy has known issues with NumPy/TextBlob during build
- Currently experimental - may fail on some platforms
- Polyglot module needs to be built separately before backend MCP tests

### Test Dependencies

- Backend unit tests use `@SpringBootTest` with Mockito
- Backend e2e tests start jar as real process, test with Java HTTP client
- Each test generates unique `chatId` (UUID) for isolation
- Tests expect Ollama running locally with `gemma4:e4b` (Linux/CI, `ollama pull gemma4:e4b`) or `gemma4:e4b-mlx` on Mac (`application-local.properties` or `-Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx`)
- Spring AI CLI Agent tests use JUnit 5 + AssertJ + Mockito (no Ollama required); evals need Ollama (`-Devals=true`)

## Environment Requirements

- Java 17+ (for Spring Boot 4)
- Maven 3.6+
- GraalVM 21+ (for polyglot module only)
- Ollama CLI with model: `gemma4:e4b` (or `gemma4:e4b-mlx` on Mac, `lfm2.5` still works but less reliable for AskUserQuestionTool)

## Common Pitfalls

1. **Forgetting Ollama**: Backend tests will fail if Ollama isn't running
2. **MCP config disabled**: Integration tests with MCP tools won't work unless config is enabled
3. **Polyglot build order**: Build polyglot module before running backend with MCP
4. **Port conflicts**: CLI agent runs on 8081, backend on 8080
5. **Module isolation**: Each module has its own `pom.xml` - run commands from module directory
6. **Vaadin frontend**: Requires Node.js for dev mode; use `vaadin.productionMode=true` for tests
7. **Vaadin fat jar needs production mode**: `vaadin-dev` is `<optional>` and excluded from the packaged jar. Running the jar with `vaadin.productionMode=false` (the yaml default, meant for `mvn spring-boot:run`) fails with `'vaadin-dev-server' not found`. Always start the jar with `--vaadin.productionMode=true` — E2EIT does this for you
8. **E2E tests need the packaged jar**: they run via maven-failsafe-plugin in the `verify` phase (`mvn clean verify`), never during `mvn test` — surefire would fail with "Jar not found" since packaging hasn't happened yet

## Code Conventions

- Java 17+ features (records, sealed classes, pattern matching)
- Spring Boot dependency injection (CDI)
- JUnit 5 + AssertJ for unit tests
- Java HTTP client for e2e tests (no browser needed)
- Maven Surefire for test execution
- Vaadin for web UI (backend module only)

## Agent Rules

### After Major Feature Changes

After completing any significant code change (new feature, refactor, bug fix):

1. **Add tests**: Unit tests for each new class/method. E2E tests for new endpoints or workflows.
2. **Update documentation**: README.md, TUTORIAL.md, BLUEPRINT-CLI-Agent.md, AGENTS.md — all must reflect current state.
3. **Run all tests**: `mvn test` in each module. All must pass before commit.
4. **Commit with conventional commit message**: `feat:`, `fix:`, `test:`, `docs:`, etc.

### Worklog

After each session, add an entry to `worklog.md` documenting what was done.
Use the `/worklog` command to generate the entry.

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
