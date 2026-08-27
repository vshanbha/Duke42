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

> These roles are now enforced, not aspirational: the softwareaifactory.sh gates
> (below) block test edits by implementer/refactorer sessions via
> `scripts/hooks/test-edit-denial.sh`. In ordinary (non-role) sessions the agent
> writes tests and implementation together per "After Major Feature Changes".

## Software Factory (softwareaifactory.sh — installed 2026-08-26)

Enforcement hooks from [softwareaifactory.sh](https://softwareaifactory.sh)
(template `anoop2811/software-factory-template` @ v0.1.5) are installed and
proven. Full rulebook: [`docs/FACTORY_RULES.md`](docs/FACTORY_RULES.md);
decisions: [`docs/DECISION_LOG.md`](docs/DECISION_LOG.md).

- **Gates** (`scripts/hooks/*.sh`): commit-message lint (no unverified "verified" claims),
  test-edit denial, decision-log gate on protected paths (`scripts/hooks`, `.github/workflows`),
  direct-push block, drift checks. Configured in flat [`factory.yaml`](factory.yaml).
- **Roles**: `.opencode/agent/{spec-writer,implementer,refactorer,reviewer,wiki-maintainer}.md`
  — opencode is canon; `.claude/` + `.codex/` adapters are generated (`make sync-harnesses`).
- **Proofs**: every gate ships a break→FAIL→revert→PASS fixture — `make selftest`
  (141 checks). CI runs it in the `factory-gates` job.
- **Check command** (Maven override, Decision #2):
  `mvn -q test '-Dtest=!ChatClientIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false && ./scripts/hooks/junit5-only-check.sh`
- **Push gate**: `git config core.hooksPath .githooks` (one-time per clone).

### PR Workflow (how every change lands)

> **AGENTS NEVER MERGE PRs. Full stop.**
> Create → verify gates → report → stop. Every PR needs @reviewer;
> substantial functionality additionally needs human diff review.
> Origin: see Decision #6 (PRs #1–#5 were auto-merged without per-merge consent).

Direct pushes to `main` are DENIED by `.githooks/pre-push` (`direct-main-push-block`).
There is NO bypass env var; `--no-verify` defeats local gates but is a governance
violation — don't. Branch protection is intentionally OFF (Decision #5: solo
maintainer); the local gate is the enforcement.

1. `git checkout -b <type>/<short-desc>` — never stage work on `main`
2. Commit message must pass `commit-message-lint.sh`: conventional type,
   ≤6 body bullets of ≤25 words, any "verified"/"fixed"/"works" claim cites the
   exact command AND its output (or write "written but NOT verified")
3. Commit touches `factory.yaml`, `opencode.json`, `scripts/hooks`, or
   `.github/workflows`? It must reference a `docs/DECISION_LOG.md` number
4. Push branch → `gh pr create`
5. Both CI jobs must pass before the PR may be called ready:
   - `Factory gate proofs` (~30s break/fix selftest)
   - `test` (~8min full suite)
6. Run the adversarial subagent on the diff — `@reviewer` (muse-spark tier,
   edit-denied) per `workflows/review-diamond.md`; post findings to the PR,
   address or rebut each one. Mandatory for every PR; minor docs-only PRs
   may be batched but the reviewer call is always made.
7. **Report back and stop.** State what was done, PR URL, gate status,
   reviewer findings. The human decides: review the diff (always for
   substantial functionality) and run `gh pr merge N --merge --delete-branch`
   themselves, then sync back: `git checkout main && git pull`.
8. **Addressing review findings on an unmerged PR:** amend the existing
   branch and force-push — do NOT create a new PR for fixes to an open PR.
   Commit the fixes, force-push the same branch, re-run CI, and update the
   PR description with what was addressed.

### Enforcement

No local hook can intercept `gh pr merge` — it is a GitHub API call invisible
to git hooks. This is a write-time rule in always-loaded context (AGENTS.md).
Hard-enforcement options if ever needed: branch protection (Decision #5: declined
while solo) or scoped GitHub credential without `contents: write` on `main`
(fine-grained PAT: `contents: read`, `pull_requests: write`, `actions: read` —
then `gh pr merge` returns 403). Audit: `gh pr view --json mergedBy` shows who
merged (currently always `vshanbha`; no agent identity exists to distinguish).
See Decision #6 for full discussion.

Quick reference — current state is always checkable via `gh pr list --state open`,
`./factory doctor` (gate health), `./factory report` (what gates caught).
- **Doctor / report**: `./factory doctor`, `./factory report`.
- **Model tiers** (Decision #3): frontier = `opencode-go/muse-spark-1.2-contributor`
  (spec-writer, reviewer); default & economy = `opencode-go/ox-alpha-free`
  (implementer, refactorer, wiki-maintainer, harness default).
  **To swap models at runtime**: edit the three `opencode_*_model` keys in
  [`factory.yaml`](factory.yaml) → run `make sync-harnesses` → land via PR.
- Java pack is upstream-"experimental": Spotless/Error Prone/SpotBugs/PIT are NOT
  wired into these Maven poms yet (Decision #2).

### Gate-Based Verification

- Every check must be seen to fail before passing: `break → FAIL → revert → PASS`
- Computational controls beat inferential ones (shell hooks > prose rules)
- Tests use JUnit 5 + AssertJ + Testcontainers (Java pack)
