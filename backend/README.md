# Duke42 Backend

Spring Boot enterprise demo: Vaadin web UI, REST API, GraphQL, and optional MCP client.

## Features

| Layer | Endpoint | Description |
|-------|----------|-------------|
| Vaadin UI | http://localhost:8080 | Browser chat with streaming responses |
| REST | `/edge/infer`, `/edge/chat/{id}`, `/edge/toolChat/{id}` | For JavaScript/React clients |
| GraphQL | `/graphql` | Queries and mutations mirroring REST |
| Swagger | `/swagger-ui.html` | OpenAPI documentation |

## Quick Start

```bash
# Prerequisites: Java 17+, Maven, Ollama with gemma4:e4b (or gemma4:e4b-mlx on Mac)
ollama pull gemma4:e4b # Mac MLX: gemma4:e4b-mlx, or -Dspring.ai.ollama.chat.options.model=gemma4:e4b-mlx

cd backend
mvn spring-boot:run
# Open http://localhost:8080
```

## REST API Examples

```bash
# Single-shot inference
curl -X POST http://localhost:8080/edge/infer \
  -H "Content-Type: text/plain" \
  -d "Hello"

# Chat with memory
curl -X POST "http://localhost:8080/edge/chat/user-1?message=My%20name%20is%20Alice" \
  -H "Content-Type: text/plain"

# Chat with MCP tools (requires polyglot server + MCP enabled)
curl -X POST "http://localhost:8080/edge/toolChat/user-1?message=Analyze%20sentiment%3A%20I%20love%20Java" \
  -H "Content-Type: text/plain"
```

## GraphQL

Open `/graphiql?path=/graphql` in the browser, or query programmatically:

```graphql
query {
  chat(chatId: "user-1", message: "Hello")
}
```

## Enable MCP (Polyglot Sentiment Server)

1. Start polyglot: `cd polyglot && mvn package -DskipTests && java -jar target/polyglot-runner.jar`
2. Set `spring.ai.mcp.client.enabled=true` in `src/main/resources/application.properties`
3. Restart backend

## Tests

From top level (`Duke42/`) or `backend/`:

```bash
# Everything: unit + GraphQL tests, package, then E2E via failsafe
# (E2E requires Ollama running; runs after packaging against the real jar)
mvn clean verify # or mvn verify -pl backend -am from top level

# Unit + GraphQL tests only (no Ollama required)
mvn test # top-level or mvn test -pl backend -am

# MCP integration (requires polyglot on port 9000) – no -Dtest needed, general setup
mvn test -Dmcp.integration=true # or mvn test -pl backend -Dmcp.integration=true
```

## Docker

```bash
# From repo root
docker compose up --build
```

Ollama must be reachable from the container (default: `host.docker.internal:11434`).
