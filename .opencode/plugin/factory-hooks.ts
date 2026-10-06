import { Plugin } from "@opencode/plugin"
import { execFile, execFileSync } from "node:child_process"
import { join } from "node:path"
import { existsSync, unlinkSync, writeFileSync } from "node:fs"

// Duke42 software-factory hooks — OpenCode V2 plugin API.
// Migrated from the V1 function entrypoint (export default async ({...}) => ({...})),
// which V2 rejects with "Plugin must export a default definition with an id and
// an effect or setup function". V2 registers hooks on their owning domain inside
// setup(); the executing agent is carried on the tool hook event, so the V1
// chat.message session→agent map is no longer needed.
export default Plugin.define({
  id: "factory-hooks",
  async setup(ctx) {
    const directory = ctx.location.directory

    // Loop-close check baseline: HEAD at plugin load, compared at unload time
    // by scripts/hooks/loop-close-check.sh.
    const flagPath = join(directory, "memory", ".pending-lesson-reminder")
    let startHead = ""
    try {
      startHead = execFileSync("git", ["rev-parse", "HEAD"], {
        cwd: directory,
        encoding: "utf-8",
        timeout: 3000,
      }).trim()
    } catch {
      // Not a git repo or git unavailable — loop-close check will skip silently.
    }

    // Test-edit denial: call the shared shell script.
    // per ADR-0004 Decision 2: no inline enforcement logic in the plugin.
    // The script (scripts/hooks/test-edit-denial.sh) is the single source of the rule.
    // V2 hook callback: one mutable event; throwing denies the tool call.
    await ctx.tool.hook("execute.before", async (event) => {
      const toolName = event.tool
      if (toolName === "edit" || toolName === "write") {
        const input = (event.input ?? {}) as { filePath?: string; path?: string }
        const filePath = input.filePath ?? input.path ?? ""

        // Role comes straight from the hook event (V1 derived it from the
        // most recent chat.message for the session).
        const role = agentNameToRole(String(event.agent ?? ""))

        // Call the shared script via execFile (non-promisified to avoid stdin deadlock).
        // The script reads JSON from stdin, so we must write to stdin and close it.
        // The promisified execFile does not support the `input` option (that's execFileSync only).
        const scriptPath = join(directory, "scripts", "hooks", "test-edit-denial.sh")
        const payload = JSON.stringify({
          tool_name: toolName,
          tool_input: { file_path: filePath },
        })

        const exitCode = await new Promise<number>((resolve) => {
          const child = execFile(scriptPath, [], {
            // cwd MUST be the project directory: the script resolves factory.yaml
            // via `git rev-parse --show-toplevel` in ITS cwd, and the shared
            // background server runs from an unrelated directory — without this,
            // test_file_patterns reads as empty and the gate fails open.
            cwd: directory,
            env: {
              ...process.env,
              FACTORY_AGENT_ROLE: role,
            },
            timeout: 5000,
          }, (err) => {
            // err is non-null if the child exited with non-zero code or timed out.
            // Exit code 2 = intentional denial (not an error in our protocol).
            if (err && "code" in err && typeof err.code === "number") {
              resolve(err.code)
            } else if (err) {
              // Script missing, timeout, or other error — fail open (see tradeoff in commit message).
              resolve(0)
            } else {
              resolve(0)
            }
          })

          // Write the JSON payload to the child's stdin and close it.
          // This unblocks the script's `INPUT=$(cat)` line.
          child.stdin?.end(payload)
        })

        if (exitCode === 2) {
          throw new Error("DENIED: implementer role cannot edit test files (*_test.go). Generator/evaluator separation.")
        }
      }
    })

    // PostToolUse: run gofmt on Go files after edit.
    // Uses execFile (not exec) to prevent command injection.
    await ctx.tool.hook("execute.after", async (event) => {
      const toolName = event.tool
      if (toolName === "edit" || toolName === "write") {
        const input = (event.input ?? {}) as { filePath?: string; path?: string }
        const filePath = input.filePath ?? input.path ?? ""
        if (/\.go$/.test(filePath)) {
          try {
            const { promisify } = await import("node:util")
            const execFileAsync = promisify(execFile)
            // cwd: filePath may be relative to the project directory.
            await execFileAsync("gofmt", ["-w", filePath], { cwd: directory })
          } catch {
            // Best-effort; non-blocking
          }
        }
      }
    })

    // Second-brain loop-close nudge: on session.idle, write a flag file
    // that reminds the agent to reflect on whether the previous turn
    // produced a lesson worth writing to memory/lessons/.
    // Per AGENTS.md "Second-brain loop-close" rule + Karpathy pattern.
    // V1 returned an `event` hook; V2 subscribes to the server event stream.
    // The stream is server-wide, so skip idle events from other locations —
    // this flag file belongs to this plugin's directory only.
    const controller = new AbortController()
    void (async () => {
      for await (const evt of ctx.event.subscribe({ signal: controller.signal })) {
        if (evt.type !== "session.idle") continue
        if (evt.location && evt.location.directory !== directory) continue
        try {
          writeFileSync(flagPath,
            `sessionID: ${evt.data.sessionID}\n` +
            `turn ended: ${new Date().toISOString()}\n\n` +
            `If this turn revealed a non-obvious fact (gotcha, version mismatch,\n` +
            `API shape, bug fix that cost time), write memory/lessons/NNN-*.md\n` +
            `with provenance. Then delete this file.\n` +
            `See AGENTS.md "Second-brain loop-close" rule.\n`
          )
        } catch {
          // Best-effort; non-blocking
        }
      }
    })().catch(() => {
      // Stream aborted during plugin unload — expected.
    })

    // Best-effort loop-close check at plugin unload (V1 `dispose` hook).
    // Calls the shared script which checks: files changed since startHead
    // but no new lesson files -> writes memory/PENDING-LESSONS.md.
    // Uses execFileSync (synchronous) so it completes before the process exits.
    return async () => {
      controller.abort()
      if (!startHead) return
      try {
        // Delete the per-turn flag file — plugin is unloading, the nudge is stale.
        if (existsSync(flagPath)) {
          unlinkSync(flagPath)
        }
        const scriptPath = join(directory, "scripts", "hooks", "loop-close-check.sh")
        execFileSync(scriptPath, [], {
          cwd: directory,
          env: {
            ...process.env,
            FACTORY_SESSION_START_HEAD: startHead,
          },
          timeout: 5000,
          stdio: "pipe",
        })
      } catch {
        // Best-effort; non-blocking. The script exits 1 when it writes
        // a reminder (changes exist, no lessons) — that's expected, not an error.
      }
    }
  },
})

// agentNameToRole maps an opencode agent name to a FACTORY_AGENT_ROLE value.
// The mapping is: implementer → "implementer" (denied test edits); all others → "" (allowed).
// This is the only place role derivation happens — the shell script does the enforcement.
function agentNameToRole(agentName: string): string {
  if (agentName === "implementer") {
    return "implementer"
  }
  return ""
}
