# Legacy JavaFX Desktop UI

This module is **not part of the default build**. It predates the Vaadin web UI in `backend/`.

## What It Does

A JavaFX desktop client that calls the backend REST API:
- **Infer** tab — single-shot `/edge/infer`
- **Chat** tab — `/edge/chat/{chatId}` with memory
- **Tool Chat** tab — `/edge/toolChat/{chatId}` with MCP tools

## Prerequisites

- Java 17+
- Backend running on port 8080 (`cd backend && mvn spring-boot:run`)

## Run

```bash
cd ui
mvn javafx:run
```

## Note

For new development, use the Vaadin web UI at http://localhost:8080 instead.
