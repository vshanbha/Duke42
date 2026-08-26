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

## #3 — Factory model tiers via the opencode zen catalog (2026-08-26)

Reviewer/spec-writer must run on a different model than the implementer
(AGENTS.md Role-Based Development; reviewer.md's own header). With
`model_provider: inherit` every tier stayed blank, so all roles would have run
this repo's default model — violating that rule.

Decision: pin both zen-catalog models, one per tier:

- `opencode_frontier_model: opencode-go/muse-spark-1.2-contributor`
  → spec-writer, reviewer (frontier tier)
- `opencode_default_model: opencode-go/ox-alpha-free`
  → implementer, harness default; also `opencode_economy_model`
  → refactorer, wiki-maintainer (collapses to default under standard profile)

Both IDs verified present in https://opencode.ai/zen/go/v1/models on
2026-08-26. Choice is deliberately cheap to change at runtime:

1. edit the three `opencode_*_model` keys in `factory.yaml`
2. `make sync-harnesses`
3. land via PR (generated adapters are drift-checked)

No other file needs touching. `claude_*`/`codex_*` tiers stay blank on purpose:
those harnesses are unused here and blank means "keep their own defaults".

## #4 — Local override of spec-writer role: Ginkgo → JUnit 5 + AssertJ (2026-08-26)

The template's `spec-writer.md` is Go-specific ("Writes Ginkgo acceptance
specs", "You write ONLY test files (*_test.go)"). Duke42 is Java: the blessed
stack per the java pack itself is JUnit 5 + AssertJ + Testcontainers, and our
test-file convention is `(Test|Tests|IT|ITCase)\.java$` under `src/test/java`.

Decision: locally override the wording in `.opencode/agent/spec-writer.md`
(description + body), regenerate adapters with `make sync-harnesses`.

Consequence: `.opencode/agent/spec-writer.md` now differs from upstream v0.1.5.
`./factory upgrade` may report or overwrite it — if so, re-apply this Decision's
wording rather than silently reverting to Ginkgo. Filed as an upstream note via
the java-pack discussion (#66) since other Maven/Java adopters hit the same leak.

## #5 — Branch protection intentionally OFF; local direct-push gate is the enforcement (2026-08-26)

Duke42 has a single committer. GitHub-side branch protection on `main` is
therefore deliberately not enabled: the `.githooks/pre-push`
`direct-main-push-block` gate is the enforcement, and the maintainer accepts
that `git push --no-verify` from a configured clone bypasses it locally.

Consequence for agents: do NOT re-suggest enabling branch protection while this
decision stands. If the contributor count grows, revisit.
