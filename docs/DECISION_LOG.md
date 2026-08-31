# Decision Log

One entry per governance-relevant decision, made before the change it governs.
Referenced by `scripts/hooks/decision-log-gate.sh` for protected-path changes.

## Decision 1 — Adopt softwareaifactory.sh (2026-08-26)

Installed the template at pinned ref v0.1.5 via the documented
`install.sh init --pack java` flow. Rationale: AGENTS.md carried the factory's
rules as prose only; upstream's thesis is that prose without hooks is a promise.
Gates now live in `scripts/hooks/`, roles in `.opencode/agent/` (opencode canon;
`.claude/` and `.codex/` are generated adapters), proofs in
`scripts/selftest/run.sh` (141 checks, run during init: 141 passed / 0 failed).
Model tiers left on `inherit`; reviewer-must-differ-model is enforced socially
until a second provider is wired into `factory.yaml`.

## Decision 2 — Maven override of the java pack check_command; Gradle pack artifacts removed (2026-08-26)

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

## Decision 3 — Factory model tiers via the opencode zen catalog (2026-08-26)

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

## Decision 4 — Local override of spec-writer role: Ginkgo → JUnit 5 + AssertJ (2026-08-26)

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

## Decision 5 — Branch protection intentionally OFF; local direct-push gate is the enforcement (2026-08-26)

Duke42 has a single committer. GitHub-side branch protection on `main` is
therefore deliberately not enabled: the `.githooks/pre-push`
`direct-main-push-block` gate is the enforcement, and the maintainer accepts
that `git push --no-verify` from a configured clone bypasses it locally.

Consequence for agents: do NOT re-suggest enabling branch protection while this
decision stands. If the contributor count grows, revisit.

## Decision 6 — Agents never merge PRs; reviewer-agent pass mandatory; human merges (2026-08-26)

Origin: during factory adoption the agent executed `gh pr merge` for PRs #1–#5
without the human asking per-merge. Green CI is not consent.

Rules, binding on every agent session:

1. An agent NEVER runs `gh pr merge` (or merges by any other means). It creates
   the PR when asked, verifies gates, and reports back — then stops.
2. Every PR gets an adversarial `@reviewer` subagent pass before it is reported
   ready; findings are posted to the PR and addressed or rebutted.
3. Substantial functionality additionally requires explicit human review of the
   diff before merge.
4. Only the human runs the merge command. After merging, the human syncs back:
   `git checkout main && git pull`.

Enforcement status: this is a write-time rule in always-loaded context
(AGENTS.md "PR Workflow"). No local hook can intercept `gh pr merge` — it is a
GitHub API call, invisible to git hooks. Hard-enforcement options if ever
needed: enable branch protection (declined for now, Decision #5), or issue the
agent a GitHub credential scoped without `contents: write` on `main` (fine-grained
PAT: `contents: read`, `pull_requests: write`, `actions: read` — then
`gh pr merge` returns 403). Until one of those lands,
compliance is detectable after the fact via `gh pr view --json mergedBy`.

## Decision 7 — CLI Agent default Ollama model is gemma4:e4b-mlx; CI pins gemma4:e4b (2026-08-29)

`mvn clean install` (and the `ChatClientIntegrationTest` `@SpringBootTest`) ran on the
default profile and requested `gemma4:e4b`, which is not installed on the maintainer's
Mac — only the MLX build `gemma4:e4b-mlx` is pulled locally, so the local build failed
with `model 'gemma4:e4b' not found`.

Decision:

- Checked-in default `spring.ai.ollama.chat.options.model` becomes `gemma4:e4b-mlx`
  (Mac MLX) in `spring-ai-cli-agent/src/main/resources/application.properties`, so a
  plain `mvn clean install` works locally with no extra flags.
- CI (`.github/workflows/ci.yml`, Linux) pins the cross-platform build with
  `-Dspring.ai.ollama.chat.options.model=gemma4:e4b` (it already `ollama pull gemma4:e4b`).
- `application-local.properties` is git-ignored (example only, not committed); it is now
  redundant with the default and left as a personal local file. `AGENTS.md` and the module
  `README.md` updated to reflect the new default.
