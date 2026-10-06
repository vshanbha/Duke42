# Lesson: OpenCode V2 plugin migration + shared-server cwd fail-open (session 10)

## Date
2026-10-06

## Context
The factory-hooks plugin (`.opencode/plugin/factory-hooks.ts`) had silently
failed to load since the OpenCode V1→V2 upgrade: server error `err_aca758e7`
and siblings `err_270fd4c5`/`err_96f02f64`. Migrated it to the V2 plugin API,
then a live parity eval exposed a second, deeper bug (fail-open cwd). Evidence
in `~/.local/share/opencode/log/opencode.log` (run 722aa801) and the worklog.

## Non-obvious facts

1. **V2 plugin default export must be an object, not a function.** V1 plugins
   `export default async ({...}) => ({...})`; V2 requires
   `Plugin.define({ id, setup(ctx) })` — otherwise `SchemaError(Expected object
   at ["default"])` (observed 2026-10-06 via opencode.log err_270fd4c5).
   Hooks move to domains: `tool.execute.before` → `ctx.tool.hook("execute.before")`,
   `event` → `ctx.event.subscribe()`, `dispose` → cleanup returned by `setup`.
2. **V2 tool hook events carry `agent` directly** (`ToolHooks["execute.before"]`
   has `agent`, `sessionID`, `input`) — the V1 `chat.message` session→agent map
   workaround is unnecessary (verified from `@opencode/plugin@2.0.23` types,
   `dist/promise/tool.d.ts`).
3. **Do not declare local file plugins in config unless needed.** V2
   auto-discovers `.opencode/plugin/` and `.opencode/plugins/` (migration guide:
   <https://opencode.ai/v2/docs/build/plugins/migrate-v1> — "V2 discovers local
   plugins from both"). A config entry with the V1 `plugin` key and raw relative
   spec (`.opencode/plugin/factory-hooks.ts`) was routed to the npm-package
   installer: `NpmInstallFailedError ... /Users/shanb/.opencode/.../package.json`
   ENOENT (err_aca758e7). Removing the key eliminated the error; verified one
   clean load + `state.status=active` via `opencode api get /api/plugin`
   (`source.path` = the singular-directory file).
4. **Hook scripts must be spawned with an explicit `cwd` — the shared server
   does not run from the project directory.** `scripts/lib/config.sh` resolves
   factory.yaml via `git rev-parse --show-toplevel` **in the script's cwd**;
   the background service's `PWD` was an unrelated project, so
   `test_file_patterns` read as empty and test-edit denial **failed open**
   (observed: live eval wrote the file, debug log showed `allow exit=0`).
   Fix: plugin passes `cwd: directory` to `execFile`; post-fix live eval output
   `Error: DENIED: implementer role cannot edit test files` and created no file.
5. **Live parity eval recipe:** `opencode run --agent implementer -m
   opencode-go/mimo-v2.6-flash --auto "<write a *Test.java file>"` — `--auto`
   approves the `ask` permission so the plugin hook is the deciding gate; the
   implementer's configured default model was geo-blocked (`This model is not
   available in your country`), hence the `-m` override (observed 2026-10-06).
6. **A mock-context probe can pass while the real server fails.** The 16-check
   probe passed pre-fix only because it ran with cwd = project root; the bug
   needed the server's actual cwd to surface. Match cwd/env conditions when
   probing child-process-dependent code (observed 2026-10-06).
