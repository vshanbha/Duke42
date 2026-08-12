# NEXT-TASK.md: Add Vaadin Web UI + Fix Backend Concerns

**Date**: 2026-08-12
**Status**: In Progress

---

## Goal

Add a minimal Vaadin chat UI to the backend module for enterprise client demos.
The backend exposes REST APIs for both Java and JavaScript developers.
The CLI agent stays separate.

## Architecture After Changes

```
┌──────────────────────────────────────────────────┐
│         Backend (Spring Boot, port 8080)          │
│                                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │  Vaadin UI  │  │  REST API   │  │  MCP     │ │
│  │  /chat      │  │  /edge/*    │  │  Client  │ │
│  │  (browser)  │  │  (for JS    │  │  (polyglot│ │
│  │             │  │   devs)     │  │   MCP)   │ │
│  └──────┬──────┘  └──────┬──────┘  └────┬─────┘ │
│         │                │              │        │
│         └────────────────┼──────────────┘        │
│                          │                       │
│                   ┌──────┴──────┐                │
│                   │ Spring AI   │                │
│                   │ ChatClient  │                │
│                   │ + Memory    │                │
│                   │ + Tools     │                │
│                   └──────┬──────┘                │
│                          │                       │
│                   ┌──────┴──────┐                │
│                   │   Ollama    │                │
│                   └─────────────┘                │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│      CLI Agent (Spring Boot, port 8081)           │
│      Terminal REPL, separate module               │
└──────────────────────────────────────────────────┘
```

## Implementation Steps

### Step 1: Verify Vaadin + Spring Boot 4 compatibility
- Test if Vaadin 25 works with Spring Boot 4.0.0
- If not, fall back to HTMX (pure Java, no licensing)

### Step 2: Add Vaadin + MCP client to backend
- Add Vaadin dependency to `backend/pom.xml`
- Add MCP client dependency
- Add Spring AI BOM

### Step 3: Create ChatService + ChatView
- `ChatService.java` — wraps ChatClient for web use
- `ChatView.java` — minimal Vaadin chat UI

### Step 4: Restore /edge/toolChat endpoint
- Add MCP client to EdgeConfiguration
- Add `/edge/toolChat/{chatId}` endpoint

### Step 5: Change CLI agent port
- Set `server.port=8081` in `spring-ai-cli-agent/application.yaml`

### Step 6: Add backend REST tests
- Add tests for `/edge/infer`, `/edge/chat`, `/edge/toolChat`

### Step 7: Update documentation
- Update TUTORIAL.md with Step 9 (Web UI)
- Update README.md
- Update AGENTS.md

## Concerns Addressed

| Concern | Resolution |
|---------|-----------|
| Vaadin + Spring Boot 4 | Verify first, fallback to HTMX |
| Lost `/edge/toolChat` | Restore in backend |
| MCP in wrong place | Move to backend |
| Port conflict | CLI agent → 8081 |
| Backend undertested | Add REST tests |

## Verification

```bash
# Start backend with Vaadin UI
cd backend && mvn spring-boot:run
# Open http://localhost:8080

# Start CLI agent
cd spring-ai-cli-agent && mvn spring-boot:run
# Runs on port 8081

# Test REST API
curl -X POST http://localhost:8080/edge/infer \
  -H "Content-Type: text/plain" \
  -d "Hello"
```
