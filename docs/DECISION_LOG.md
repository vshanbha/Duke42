# Decision Log

One entry per governance-relevant decision, made before the change it governs.
Referenced by `scripts/hooks/decision-log-gate.sh` for protected-path changes.

## #1 — Adopt softwareaifactory.sh (2026-08-26)

Installed the template at pinned ref v0.1.5 via the documented
`install.sh init --pack java` flow. Rationale: AGENTS.md carried the factory's
rules as prose only; upstream's thesis is that prose without hooks is a promise.
Gates now live in `scripts/hooks/`, roles in `.opencode/agent/` (opencode canon;
`.claude/` and `.codex/` are generated adapters), proofs in
`scripts/selftest/run.sh` (141 checks, run during init: 141 passed / 0 failed).
Model tiers left on `inherit`; reviewer-must-differ-model is enforced socially
until a second provider is wired into `factory.yaml`.

## #2 — Maven override of the java pack check_command; Gradle pack artifacts removed (2026-08-26)

The java pack assumes Gradle (`./gradlew spotlessCheck test`,
`quality.gradle`, `.github/workflows/java-pack.yml`). Duke42 is a multi-module
Maven build with no wrapper. Decisions:

- `factory.yaml check_command` overridden to:
  `mvn -q test '-Dtest=!ChatClientIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false && ./scripts/hooks/junit5-only-check.sh`
  — deterministic offline gate. The excluded test needs a locally pulled LLM
  model; evals and Testcontainers tests gate themselves separately.
- `quality.gradle` deleted (no Gradle build to apply it from).
- `java-pack.yml` workflow deleted until Spotless/Error Prone/SpotBugs/PIT are
  wired as Maven plugins in the poms — shipping a red-on-arrival CI workflow is
  worse than deferring it. Revisit when pom quality plugins land.
