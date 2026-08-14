# NEXT-TASK.md: Add Vaadin Web UI + Fix Backend Concerns

**Date**: 2026-08-12  
**Completed**: 2026-08-14

---

## Status: Complete

All goals from this task were implemented across sessions 1–2 and follow-up work on 2026-08-14:

- Vaadin 25 web UI on backend (port 8080)
- REST API `/edge/infer`, `/edge/chat`, `/edge/toolChat`
- GraphQL layer with shared `ChatClients` service
- MCP client wiring (disabled by default, config in `application.yaml`)
- CLI agent on port 8081
- Backend tests (unit, GraphQL, e2e)
- Docker Compose, CI/CD, Swagger/OpenAPI
- Vaadin memory fix (uses `ChatClients`), async streaming UI
- Documentation synced (README, TUTORIAL, backend README)

See [worklog.md](worklog.md) for session details.
